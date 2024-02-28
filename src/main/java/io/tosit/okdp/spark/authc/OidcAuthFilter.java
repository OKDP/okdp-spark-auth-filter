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

package io.tosit.okdp.spark.authc;

import static io.tosit.okdp.spark.authc.utils.HttpAuthenticationUtils.domain;
import static io.tosit.okdp.spark.authc.utils.JsonUtils.loadJsonFromUrl;
import static io.tosit.okdp.spark.authc.utils.PreconditionsUtils.checkNotNull;
import static io.tosit.okdp.spark.authc.utils.TokenUtils.userInfo;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import io.tosit.okdp.spark.authc.config.Constants;
import io.tosit.okdp.spark.authc.config.HttpSecurityConfig;
import io.tosit.okdp.spark.authc.config.OidcConfig;
import io.tosit.okdp.spark.authc.model.AccessToken;
import io.tosit.okdp.spark.authc.model.PersistedToken;
import io.tosit.okdp.spark.authc.model.UserInfo;
import io.tosit.okdp.spark.authc.model.WellKnownConfiguration;
import io.tosit.okdp.spark.authc.provider.AuthProvider;
import io.tosit.okdp.spark.authc.provider.store.CookieTokenStore;
import io.tosit.okdp.spark.authc.utils.HttpAuthenticationUtils;
import io.tosit.okdp.spark.authc.utils.exception.Try;
import io.tosit.okdp.spark.authz.OidcGroupMappingServiceProvider;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OidcAuthFilter implements Filter, Constants {
  private AuthProvider authProvider;

  @Override
  public void init(FilterConfig filterConfig) {

    String issuerUri =
        checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_ISSUER_URI))
                .orElse(System.getenv("AUTH_ISSUER_URI")),
            AUTH_ISSUER_URI);
    String clientId =
        checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_ID))
                .orElse(System.getenv("AUTH_CLIENT_ID")),
            AUTH_CLIENT_ID);
    String clientSecret =
        checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_SECRET))
                .orElse(System.getenv("AUTH_CLIENT_SECRET")),
            AUTH_CLIENT_SECRET);
    String redirectUri =
        checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_REDIRECT_URI))
                .orElse(System.getenv("AUTH_REDIRECT_URI")),
            AUTH_REDIRECT_URI);
    String scope =
        checkNotNull(
            ofNullable(filterConfig.getInitParameter(AUTH_SCOPE))
                .orElse(System.getenv("AUTH_SCOPE")),
            AUTH_SCOPE);
    String encryptionKey =
        checkNotNull(
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
                loadJsonFromUrl(
                    format("%s%s", issuerUri, AUTH_ISSUER_WELL_KNOWN_CONFIGURATION),
                    WellKnownConfiguration.class))
            .build();

    log.info(
        "OIDC Server well known configuration: \nAuthorization Endpoint: {}, \nToken Endpoint: {}, \nUser Info Endpoint: {}, \nSupported Scopes: {}",
        oidcConfig.wellKnownConfiguration().authorizationEndpoint(),
        oidcConfig.wellKnownConfiguration().tokenEndpoint(),
        oidcConfig.wellKnownConfiguration().userInfoEndpoint(),
        oidcConfig.wellKnownConfiguration().scopesSupported());

    log.info(
        "Initializing OIDC Auth Provider (access token cookie based storage/cookie name: {}, max-age (minutes): {}) ...",
        AUTH_COOKE_NAME,
        cookieMaxAgeMinutes);
    authProvider =
        HttpSecurityConfig.create(oidcConfig)
            .authorizeRequests(".*/.*\\.css", ".*/.*\\.js", ".*/.*\\.png")
            .tokenStore(
                CookieTokenStore.of(
                    AUTH_COOKE_NAME,
                    domain(oidcConfig.redirectUri()),
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
                    e -> ((HttpServletResponse) servletResponse).setStatus(e.getHttpStatusCode()));
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
              .onException(
                  e -> ((HttpServletResponse) servletResponse).setStatus(e.getHttpStatusCode()));
      UserInfo userInfo = userInfo(accessToken.accessToken());
      log.info(
          "Successfully authenticated user: {} (roles: {}, groups: {})",
          userInfo.email(),
          userInfo.roles(),
          userInfo.groups());
      Cookie cookie = authProvider.httpSecurityConfig().tokenStore().save(accessToken);
      ((HttpServletResponse) servletResponse).addCookie(cookie);
      // Add the user and groups in the user/group mappings authorization cache
      OidcGroupMappingServiceProvider.addUserAndGroups(
          userInfo.email(), userInfo.getGroupsAndRoles());
      // Redirect the user from the browser (client) side into spark/history UI home page (prevent
      // browser back button to back with an invalid 'code')
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
