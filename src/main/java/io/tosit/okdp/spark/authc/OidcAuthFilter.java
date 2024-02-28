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

import io.tosit.okdp.spark.authc.config.Constants;
import io.tosit.okdp.spark.authc.config.HttpSecurityConfig;
import io.tosit.okdp.spark.authc.config.OidcConfig;
import io.tosit.okdp.spark.authc.exception.FilterInitializationException;
import io.tosit.okdp.spark.authc.model.AccessToken;
import io.tosit.okdp.spark.authc.model.AccessTokenPayload;
import io.tosit.okdp.spark.authc.model.PersistedToken;
import io.tosit.okdp.spark.authc.model.WellKnownConfiguration;
import io.tosit.okdp.spark.authc.provider.AuthProvider;
import io.tosit.okdp.spark.authc.provider.store.CookieTokenStore;
import io.tosit.okdp.spark.authc.utils.HttpAuthenticationUtils;
import io.tosit.okdp.spark.authc.utils.exception.Try;
import io.tosit.okdp.spark.authz.OidcGroupMappingServiceProvider;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static io.tosit.okdp.spark.authc.utils.JsonUtils.loadJsonFromUrl;
import static io.tosit.okdp.spark.authc.utils.PreconditionsUtils.checkNotNull;
import static io.tosit.okdp.spark.authc.utils.TokenUtils.payload;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Slf4j
public class OidcAuthFilter implements Filter, Constants {
    private AuthProvider authProvider;

    @Override
    public void init(FilterConfig filterConfig) {

        String issuerUri = checkNotNull(ofNullable(filterConfig.getInitParameter(AUTH_ISSUER_URI))
                .orElse(System.getenv("AUTH_ISSUER_URI")), AUTH_ISSUER_URI);
        String clientId = checkNotNull(ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_ID))
                .orElse(System.getenv("AUTH_CLIENT_ID")), AUTH_CLIENT_ID);
        String clientSecret = checkNotNull(ofNullable(filterConfig.getInitParameter(AUTH_CLIENT_SECRET))
                .orElse(System.getenv("AUTH_CLIENT_SECRET")), AUTH_CLIENT_SECRET);
        String redirectUri = checkNotNull(ofNullable(filterConfig.getInitParameter(AUTH_REDIRECT_URI))
                .orElse(System.getenv("AUTH_REDIRECT_URI")), AUTH_REDIRECT_URI);
        String scope = checkNotNull(ofNullable(filterConfig.getInitParameter(AUTH_SCOPE))
                .orElse(System.getenv("AUTH_SCOPE")), AUTH_SCOPE);
        String encryptionKey = checkNotNull(ofNullable(filterConfig.getInitParameter(AUTH_COOKIE_ENCRYPTION_KEY))
                .orElse(System.getenv("AUTH_COOKIE_ENCRYPTION_KEY")), AUTH_COOKIE_ENCRYPTION_KEY);
        int cookieMaxAgeSeconds = Integer.parseInt(ofNullable(filterConfig.getInitParameter(AUTH_COOKE_MAX_AGE_SECONDS))
                .orElse(ofNullable(System.getenv("AUTH_COOKE_MAX_AGE_SECONDS"))
                        .orElse(String.valueOf(AUTH_COOKE_DEFAULT_MAX_AGE_SECONDS))));

        log.info("Initializing OIDC Auth filter ({}: <{}>,  {}: <{}>) ...", AUTH_ISSUER_URI, issuerUri, AUTH_CLIENT_ID, clientId);

        OidcConfig oidcConfig = OidcConfig
                .builder()
                .issuerUri(issuerUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .responseType("code")
                .scope(scope)
                .wellKnownConfiguration(loadJsonFromUrl(format("%s%s", issuerUri, AUTH_ISSUER_WELL_KNOWN_CONFIGURATION), WellKnownConfiguration.class))
                .build();

        log.info("OIDC Server well known configuration: \nAuthorization Endpoint: {}, \nToken Endpoint: {}, \nUser Info Endpoint: {}, \nSupported Scopes: {}",
                oidcConfig.wellKnownConfiguration().authorizationEndpoint(),
                oidcConfig.wellKnownConfiguration().tokenEndpoint(),
                oidcConfig.wellKnownConfiguration().userInfoEndpoint(),
                oidcConfig.wellKnownConfiguration().scopesSupported());

        try {
            log.info("Initializing OIDC Auth Provider (access token cookie based storage/cookie name: {}, max-age: {}) ...", AUTH_COOKE_NAME, cookieMaxAgeSeconds);
            authProvider = HttpSecurityConfig.create(oidcConfig)
                    .authorizeRequests(".*/.*\\.css", ".*/.*\\.js", ".*/.*\\.png")
                    .tokenStore(CookieTokenStore.of(AUTH_COOKE_NAME,
                            new URL(oidcConfig.redirectUri()).getHost(),
                            encryptionKey, cookieMaxAgeSeconds))
                    .configure();
        } catch (MalformedURLException e) {
            throw new FilterInitializationException(e.getMessage(), e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // Skip authentication for static content (.js, .css, .png, etc)
        if (authProvider.isAuthorized(servletRequest)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Extract the access token from the http cookie if present
        Optional<String> maybeCookie = HttpAuthenticationUtils.getCookieValue(AUTH_COOKE_NAME, servletRequest);
        if (maybeCookie.isPresent()) {
            PersistedToken persistedToken = authProvider.httpSecurityConfig()
                    .tokenStore()
                    .readToken(maybeCookie.get());

            if (persistedToken.isExpired()) {
                log.info("The user {} token was expired, renewing ... ", persistedToken.accessTokenPayload().email());
                AccessToken accessToken = Try.of(() -> authProvider.refreshToken(persistedToken.refreshToken()))
                        .onException(e -> ((HttpServletResponse) servletResponse).setStatus(e.getHttpStatusCode()));
                Cookie cookie = authProvider.httpSecurityConfig().tokenStore().save(accessToken);
                ((HttpServletResponse) servletResponse).addCookie(cookie);
            }
            // Add the user and groups in the authorization cache
            OidcGroupMappingServiceProvider.addUserAndGroups(persistedToken.accessTokenPayload().email(),
                    persistedToken.accessTokenPayload().getAllGroups());
            filterChain.doFilter(new PrincipalHttpServletRequestWrapper((HttpServletRequest) servletRequest,
                    persistedToken.accessTokenPayload().email()), servletResponse);
            return;

        }

        if (servletRequest.getParameter("code") == null) {
            // When the users access spark UI/History for the time, redirect them to the oidc server to authenticate
            authProvider.redirectUserToAuthorizationEndpoint(servletResponse);
        } else {
            // The user was authenticated with the oidc server and got the code
            // From the code, get an access token from the oidc server
            AccessToken accessToken = Try.of(() -> authProvider.requestAccessToken(servletRequest.getParameter("code")))
                    .onException(e -> ((HttpServletResponse) servletResponse).setStatus(e.getHttpStatusCode()));

            AccessTokenPayload payload = payload(accessToken.accessToken());
            log.info("Successfully authenticated user: {} (roles: {}, groups: {})", payload.email(), payload.roles(), payload.groups());
            Cookie cookie = authProvider.httpSecurityConfig().tokenStore().save(accessToken);
            ((HttpServletResponse) servletResponse).addCookie(cookie);
            // Add the user and groups in the authorization cache
            OidcGroupMappingServiceProvider.addUserAndGroups(payload.email(), payload.getAllGroups());
            // Redirect the user from the browser (client) side (the authz 'code' becomes invalid)
            servletResponse.getWriter().print("<script type=\"text/javascript\">window.location.href = '/home'</script>");
        }
    }

    @Override
    public void destroy() {
        log.info("OIDC Auth filter destroyed");
    }

}
