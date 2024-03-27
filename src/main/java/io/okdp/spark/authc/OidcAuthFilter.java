/*
 *    Copyright 2024 tosit.io
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.okdp.spark.authc;

import static io.okdp.spark.authc.utils.HttpAuthenticationUtils.domain;
import static io.okdp.spark.authc.utils.HttpAuthenticationUtils.sendError;
import static io.okdp.spark.authc.utils.PreconditionsUtils.assertCookieSecure;
import static io.okdp.spark.authc.utils.PreconditionsUtils.assertSupportePKCE;
import static io.okdp.spark.authc.utils.PreconditionsUtils.assertSupportedScopes;
import static io.okdp.spark.authc.utils.PreconditionsUtils.checkAuthLogin;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import io.okdp.spark.authc.config.Constants;
import io.okdp.spark.authc.config.HttpSecurityConfig;
import io.okdp.spark.authc.config.OidcConfig;
import io.okdp.spark.authc.exception.AuthenticationException;
import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.PersistedToken;
import io.okdp.spark.authc.model.WellKnownConfiguration;
import io.okdp.spark.authc.provider.AuthProvider;
import io.okdp.spark.authc.provider.IdentityProviderFactory;
import io.okdp.spark.authc.provider.impl.store.CookieSessionStore;
import io.okdp.spark.authc.utils.HttpAuthenticationUtils;
import io.okdp.spark.authc.utils.JsonUtils;
import io.okdp.spark.authc.utils.PreconditionsUtils;
import io.okdp.spark.authc.utils.TokenUtils;
import io.okdp.spark.authc.utils.exception.Try;
import io.okdp.spark.authz.OidcGroupMappingServiceProvider;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;

@Slf4j
public class OidcAuthFilter implements Filter, Constants {

  private AuthProvider authProvider;

  @Override
  public void init(FilterConfig filterConfig) {
    String issuerUri =
        PreconditionsUtils.checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_ISSUER_URI))
                .orElse(System.getenv("AUTH_ISSUER_URI")),
            AUTH_ISSUER_URI);
    String clientId =
        PreconditionsUtils.checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_ID))
                .orElse(System.getenv("AUTH_CLIENT_ID")),
            AUTH_CLIENT_ID);
    String clientSecret =
        ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_SECRET))
            .orElse(System.getenv("AUTH_CLIENT_SECRET"));
    String redirectUri =
        PreconditionsUtils.checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_REDIRECT_URI))
                .orElse(System.getenv("AUTH_REDIRECT_URI")),
            AUTH_REDIRECT_URI);
    Boolean isCookieSecure =
        Boolean.valueOf(
            ofNullable(filterConfig.getInitParameter(AUTH_COOKE_IS_SECURE))
                .orElse(
                    ofNullable(System.getenv("AUTH_COOKE_IS_SECURE"))
                        .orElse(AUTH_COOKE_DEFAULT_IS_SECURE)));
    String scope =
        PreconditionsUtils.checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_SCOPE))
                .orElse(System.getenv("AUTH_SCOPE")),
            AUTH_SCOPE);
    String encryptionKey =
        PreconditionsUtils.checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_COOKIE_ENCRYPTION_KEY))
                .orElse(System.getenv("AUTH_COOKIE_ENCRYPTION_KEY")),
            AUTH_COOKIE_ENCRYPTION_KEY);
    int cookieMaxAgeMinutes =
        Integer.parseInt(
            ofNullable(filterConfig.getInitParameter(AUTH_COOKE_MAX_AGE_MINUTES))
                .orElse(
                    ofNullable(System.getenv("AUTH_COOKE_MAX_AGE_SECONDS"))
                        .orElse(String.valueOf(AUTH_COOKE_DEFAULT_MAX_AGE_MINUTES))));
    String usePKCE =
        ofNullable(filterConfig.getInitParameter(AUTH_USE_PKCE))
            .orElse(ofNullable(System.getenv("AUTH_USE_PKCE")).orElse("auto"));
    String idProvider =
        ofNullable(filterConfig.getInitParameter(AUTH_USER_ID))
            .orElse(ofNullable(System.getenv("AUTH_USER_ID")).orElse("Email"));

    log.info(
        "Initializing OIDC Auth filter ({}: <{}>,  {}: <{}>) ...",
        AUTH_ISSUER_URI,
        issuerUri,
        AUTH_CLIENT_ID,
        clientId);

    ofNullable(clientSecret)
        .ifPresentOrElse(
            secret ->
                log.info(
                    "Client Secret provided - Running with Confidential Client with PKCE support set to '{}'",
                    usePKCE),
            () ->
                log.info(
                    "Client Secret not provided - Running with Public Client with PKCE support set to '{}'",
                    usePKCE));

    OidcConfig oidcConfig =
        OidcConfig.builder()
            .issuerUri(issuerUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(redirectUri)
            .responseType("code")
            .scope(scope)
            .usePKCE(usePKCE)
            .identityProvider(IdentityProviderFactory.from(TokenUtils.capitalize(idProvider)))
            .wellKnownConfiguration(
                JsonUtils.loadJsonFromUrl(
                    format("%s%s", issuerUri, AUTH_ISSUER_WELL_KNOWN_CONFIGURATION),
                    WellKnownConfiguration.class))
            .build();

    log.info(
        "Your OIDC provider well known configuration: \n"
            + "Authorization Endpoint: {}, \n"
            + "Token Endpoint: {}, \n"
            + "User Info Endpoint: {}, \n"
            + "Supported Scopes: {}, \n"
            + "PKCE Supported Code Challenge Methods: {}, \n",
        oidcConfig.wellKnownConfiguration().authorizationEndpoint(),
        oidcConfig.wellKnownConfiguration().tokenEndpoint(),
        oidcConfig.wellKnownConfiguration().userInfoEndpoint(),
        oidcConfig.wellKnownConfiguration().scopesSupported(),
        oidcConfig.wellKnownConfiguration().supportedPKCECodeChallengeMethods());

    assertSupportedScopes(
        oidcConfig.wellKnownConfiguration().scopesSupported(),
        scope,
        format("%s|env: %s", AUTH_SCOPE, "AUTH_SCOPE"));
    assertCookieSecure(
        oidcConfig.redirectUri(),
        isCookieSecure,
        format("%s|env: %s", AUTH_COOKE_IS_SECURE, "AUTH_COOKE_IS_SECURE"));
    assertSupportePKCE(
        oidcConfig.wellKnownConfiguration().supportedPKCECodeChallengeMethods(),
        usePKCE,
        clientSecret,
        format("%s|env: %s", AUTH_CLIENT_SECRET, "AUTH_COOKE_IS_SECURE"));

    log.info(
        "Initializing OIDC Auth Provider (Cookie based storage for High Available session persistence/cookie name: {},"
            + " max-age (minutes): {}) ...",
        AUTH_COOKE_NAME,
        cookieMaxAgeMinutes);
    authProvider =
        HttpSecurityConfig.create(oidcConfig)
            .authorizeRequests(".*/.*\\.css", ".*/.*\\.js", ".*/.*\\.png")
            .sessionStore(
                CookieSessionStore.of(
                    AUTH_COOKE_NAME,
                    domain(oidcConfig.redirectUri()),
                    isCookieSecure,
                    encryptionKey,
                    cookieMaxAgeMinutes * 60))
            .configure();
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    // Skip authentication for static content (.js, .css, .png, etc)
    if (authProvider.isAuthorized(servletRequest)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    // Extract the access token from the http auth cookie if present
    Optional<String> maybeAuthCookie =
        HttpAuthenticationUtils.getCookieValue(AUTH_COOKE_NAME, servletRequest);
    if (maybeAuthCookie.isPresent()) {
      PersistedToken persistedToken =
          authProvider.httpSecurityConfig().sessionStore().readToken(maybeAuthCookie.get());

      if (persistedToken.isExpired()) {
        log.info("The user {} token was expired, renewing ... ", persistedToken.userInfo().email());
        // Handle scenarios where offline_access is disabled, oidc provider logout or expired the
        // session/tokens, user logged in with another identifier, etc.
        // Note that, even the oidc provider had expired the token, it remains valid in the cookie
        // until it expire there.
        // So, in case we cannot renew the token, we set the cookie value as empty and let the
        // current request passes
        // The subsequent requests will require a user authentication from the oidc provider
        AccessToken accessToken =
            Try.of(() -> authProvider.refreshToken(persistedToken.refreshToken()))
                .onException(
                    e ->
                        log.info(
                            "Unable to renew access token from refresh token, removing cookie and retrying ....., cause: {}",
                            e.getMessage()));
        PersistedToken pToken = authProvider.httpSecurityConfig().toPersistedToken(accessToken);
        Cookie cookie = authProvider.httpSecurityConfig().sessionStore().save(pToken);
        ((HttpServletResponse) servletResponse).addCookie(cookie);
      }
      // Add the user and groups in the user/group mappings authorization cache
      OidcGroupMappingServiceProvider.addUserAndGroups(
          persistedToken.id(), persistedToken.userInfo().getGroupsAndRoles());
      filterChain.doFilter(
          new PrincipalHttpServletRequestWrapper(
              (HttpServletRequest) servletRequest, persistedToken.id()),
          servletResponse);
      return;
    }

    // Get the oidc authorization code if the user is authenticated
    Optional<String> maybeAuthzCode = ofNullable(servletRequest.getParameter("code"));
    if (maybeAuthzCode.isEmpty()) {
      // The previous redirect was maybe failed (prevent infinite loop)
      Try.of(() -> checkAuthLogin(servletRequest))
          .onException(e -> sendError(servletResponse, e.getHttpStatusCode(), e.getMessage()));
      // Redirect the user to the oidc provider to authenticate at the first time access to spark
      // UI/History
      authProvider.redirectUserToAuthorizationEndpoint(servletResponse);
    } else {
      // The user is authenticated and redirected by the oidc provider into the application with a
      // 'code' query parameter (?code=...)
      // Exchange the obtained 'code' with an access token by issuing a request against the oidc
      // provider
      AccessToken accessToken =
          Try.of(() -> authProvider.requestAccessToken(servletRequest, servletResponse))
              .onException(e -> sendError(servletResponse, e.getHttpStatusCode(), e.getMessage()));
      PersistedToken persistedToken =
          authProvider.httpSecurityConfig().toPersistedToken(accessToken);
      // UserInfo userInfo = authProvider.httpSecurityConfig().userInfo(accessToken.accessToken());
      log.info(
          "Successfully authenticated user ({}): email {} sub {} (roles: {}, groups: {})",
          persistedToken.userInfo().name(),
          persistedToken.userInfo().email(),
          persistedToken.userInfo().sub(),
          persistedToken.userInfo().roles(),
          persistedToken.userInfo().groups());

      Try.of(
              () ->
                  ofNullable(persistedToken.id())
                      .orElseThrow(
                          () ->
                              new AuthenticationException(
                                  HttpStatus.SC_UNAUTHORIZED,
                                  "Your oidc provider returned an empty user id and may have expired your oidc session! "
                                      + "Please try to delete your oidc provider cookie from the browser and try again!")))
          .onException(e -> sendError(servletResponse, e.getHttpStatusCode(), e.getMessage()));

      Cookie cookie = authProvider.httpSecurityConfig().sessionStore().save(persistedToken);
      ((HttpServletResponse) servletResponse).addCookie(cookie);
      // Add the user and groups in the user/group mappings authorization cache
      OidcGroupMappingServiceProvider.addUserAndGroups(
          persistedToken.id(), persistedToken.userInfo().getGroupsAndRoles());
      // Redirect the user from the browser (client) side into spark/history UI home page (i.e.
      // remove the authz 'code' from the browser)
      servletResponse
          .getWriter()
          .print("<script type=\"text/javascript\">window.location.href = '/home'</script>");
    }
  }

  @Override
  public void destroy() {
    log.info("OIDC Auth filter destroyed");
  }
}
