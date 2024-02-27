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
package io.tosit.okdp.spark.authc.provider;

import com.google.common.annotations.VisibleForTesting;
import io.tosit.okdp.spark.authc.config.Constants;
import io.tosit.okdp.spark.authc.config.HttpSecurityConfig;
import io.tosit.okdp.spark.authc.exception.AuthenticationException;
import io.tosit.okdp.spark.authc.model.AccessToken;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static io.tosit.okdp.spark.authc.utils.JsonUtils.loadJsonFromString;
import static io.tosit.okdp.spark.authc.utils.PreconditionsUtils.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.hc.core5.util.Timeout.ofSeconds;

@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Accessors(fluent = true)
public class OidcAuthProvider implements Constants, AuthProvider {

    @NonNull
    private HttpSecurityConfig httpSecurityConfig;

    /**
     * {@inheritDoc}
     */
    @Override
    public void redirectUserToAuthorizationEndpoint(ServletResponse servletResponse) throws AuthenticationException {
        String authzUrl = String.format("%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s",
                httpSecurityConfig.oidcConfig().wellKnownConfiguration().authorizationEndpoint(),
                httpSecurityConfig.oidcConfig().clientId(),
                httpSecurityConfig.oidcConfig().redirectUri(),
                httpSecurityConfig.oidcConfig().responseType(),
                httpSecurityConfig.oidcConfig().scope());
        try {
            ((HttpServletResponse) servletResponse).sendRedirect(authzUrl);
        } catch (IOException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken requestAccessToken(String code) throws AuthenticationException {
        checkNotNull(code, "code");

        Request request = Request.post(httpSecurityConfig.oidcConfig().wellKnownConfiguration().tokenEndpoint())
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .bodyForm(Form.form()
                        .add("client_id", httpSecurityConfig.oidcConfig().clientId())
                        .add("client_secret", httpSecurityConfig.oidcConfig().clientSecret())
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", httpSecurityConfig.oidcConfig().redirectUri())
                        .build())
                .responseTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS))
                .connectTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS));

        return loadJsonFromString(doExecute(request), AccessToken.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken refreshToken(String refreshToken) throws AuthenticationException {
        checkNotNull(refreshToken, "refresh_token");
        Request request = Request.post(httpSecurityConfig.oidcConfig().wellKnownConfiguration().tokenEndpoint())
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .bodyForm(Form.form()
                        .add("client_id", httpSecurityConfig.oidcConfig().clientId())
                        .add("client_secret", httpSecurityConfig.oidcConfig().clientSecret())
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .build())
                .responseTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS))
                .connectTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS));

        return loadJsonFromString(doExecute(request), AccessToken.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(ServletRequest servletRequest) {
        return httpSecurityConfig
                .patterns()
                .stream()
                .anyMatch(p -> p.matcher(((HttpServletRequest) servletRequest).getRequestURI()).matches());
    }

    @VisibleForTesting
    public String doExecute(Request request) throws AuthenticationException {
        try {
            return request.execute().handleResponse(response -> {
                        final int status = response.getCode();
                        final Optional<HttpEntity> maybeEntity = ofNullable(response.getEntity());
                        String content;
                        try (HttpEntity entity = maybeEntity.orElseThrow(() -> new ClientProtocolException(format("%s %s - The response does not contain content", status, response.getReasonPhrase())))) {
                            content = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                        }
                        if (status != HttpStatus.SC_OK) {
                            throw new AuthenticationException(status, format("%s %s - Unable to retrieve an access token (%s)", status, response.getReasonPhrase(), content));
                        }
                        return content;
                    }
            );
        } catch (IOException e) {
            throw new AuthenticationException(e.getMessage(), e);
        }
    }
}
