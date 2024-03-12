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
import static io.okdp.spark.authc.utils.PreconditionsUtils.assertSupportedScopes;
import static io.okdp.spark.authc.utils.PreconditionsUtils.checkAuthLogin;
import static io.okdp.spark.authc.utils.TokenUtils.userInfo;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import io.okdp.spark.authc.config.Constants;
import io.okdp.spark.authc.config.HttpSecurityConfig;
import io.okdp.spark.authc.config.OidcConfig;
import io.okdp.spark.authc.exception.AuthenticationException;
import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.PersistedToken;
import io.okdp.spark.authc.model.UserInfo;
import io.okdp.spark.authc.model.WellKnownConfiguration;
import io.okdp.spark.authc.provider.AuthProvider;
import io.okdp.spark.authc.provider.store.CookieTokenStore;
import io.okdp.spark.authc.utils.HttpAuthenticationUtils;
import io.okdp.spark.authc.utils.JsonUtils;
import io.okdp.spark.authc.utils.PreconditionsUtils;
import io.okdp.spark.authc.utils.exception.Try;
import io.okdp.spark.authz.OidcGroupMappingServiceProvider;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.*;
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
        PreconditionsUtils.checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_SECRET))
                .orElse(System.getenv("AUTH_CLIENT_SECRET")),
            AUTH_CLIENT_SECRET);
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

    log.info(
        "Initializing OIDC Auth filter ({}: <{}>,  {}: <{}>) ...",
        AUTH_ISSUER_URI,
        issuerUri,
        AUTH_CLIENT_ID,
        clientId);

    OidcConfig oidcConfig =
        OidcConfig.builder()
            .issuerUri(issuerUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(redirectUri)
            .responseType("code")
            .scope(scope)
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
            + "Supported Scopes: {}",
        oidcConfig.wellKnownConfiguration().authorizationEndpoint(),
        oidcConfig.wellKnownConfiguration().tokenEndpoint(),
        oidcConfig.wellKnownConfiguration().userInfoEndpoint(),
        oidcConfig.wellKnownConfiguration().scopesSupported());

    assertSupportedScopes(
        oidcConfig.wellKnownConfiguration().scopesSupported(),
        scope,
        format("%s|env: %s", AUTH_SCOPE, "AUTH_SCOPE"));
    assertCookieSecure(
        oidcConfig.redirectUri(),
        isCookieSecure,
        format("%s|env: %s", AUTH_COOKE_IS_SECURE, "AUTH_COOKE_IS_SECURE"));

    log.info(
        "Initializing OIDC Auth Provider (access token cookie based storage/cookie name: {},"
            + " max-age (minutes): {}) ...",
        AUTH_COOKE_NAME,
        cookieMaxAgeMinutes);
    authProvider =
        HttpSecurityConfig.create(oidcConfig)
            .authorizeRequests(".*/.*\\.css", ".*/.*\\.js", ".*/.*\\.png")
            .tokenStore(
                CookieTokenStore.of(
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
          authProvider.httpSecurityConfig().tokenStore().readToken(maybeAuthCookie.get());

      if (persistedToken.isExpired()) {
        log.info("The user {} token was expired, renewing ... ", persistedToken.userInfo().email());
        AccessToken accessToken =
            Try.of(() -> authProvider.refreshToken(persistedToken.refreshToken()))
                .onException(
                    e -> sendError(servletResponse, e.getHttpStatusCode(), e.getMessage()));
        Cookie cookie = authProvider.httpSecurityConfig().tokenStore().save(accessToken);
        ((HttpServletResponse) servletResponse).addCookie(cookie);
      }
      // Add the user and groups in the user/group mappings authorization cache
      OidcGroupMappingServiceProvider.addUserAndGroups(
          persistedToken.userInfo().email(), persistedToken.userInfo().getGroupsAndRoles());
      filterChain.doFilter(
          new PrincipalHttpServletRequestWrapper(
              (HttpServletRequest) servletRequest, persistedToken.userInfo().email()),
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
          Try.of(() -> authProvider.requestAccessToken(maybeAuthzCode.get()))
              .onException(e -> sendError(servletResponse, e.getHttpStatusCode(), e.getMessage()));
      UserInfo userInfo = userInfo(accessToken.accessToken());
      log.info(
          "Successfully authenticated user ({}): {} (roles: {}, groups: {})",
          userInfo.name(),
          userInfo.email(),
          userInfo.roles(),
          userInfo.groups());

      Try.of(
              () ->
                  ofNullable(userInfo.email())
                      .orElseThrow(
                          () ->
                              new AuthenticationException(
                                  HttpStatus.SC_UNAUTHORIZED,
                                  "The oidc provider returned an empty user email, you may try to delete your oidc provider cookie from the browser")))
          .onException(e -> sendError(servletResponse, e.getHttpStatusCode(), e.getMessage()));

      Cookie cookie = authProvider.httpSecurityConfig().tokenStore().save(accessToken);
      ((HttpServletResponse) servletResponse).addCookie(cookie);
      // Add the user and groups in the user/group mappings authorization cache
      OidcGroupMappingServiceProvider.addUserAndGroups(
          userInfo.email(), userInfo.getGroupsAndRoles());
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
