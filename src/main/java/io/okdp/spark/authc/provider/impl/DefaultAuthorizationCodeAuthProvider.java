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

package io.okdp.spark.authc.provider.impl;

import static io.okdp.spark.authc.utils.PreconditionsUtils.checkNotNull;
import static java.lang.String.format;
import static org.apache.hc.core5.util.Timeout.ofSeconds;

import io.okdp.spark.authc.config.HttpSecurityConfig;
import io.okdp.spark.authc.exception.AuthenticationException;
import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.provider.AuthProvider;
import io.okdp.spark.authc.utils.JsonUtils;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;

/**
 * The Confidential Client Oauth2/oidc Authorization Code grant provider implementation
 *
 * @see AuthProvider
 */
@Getter
@Accessors(fluent = true)
@Slf4j
public class DefaultAuthorizationCodeAuthProvider extends AbstractAuthorizationCodeAuthProvider {

  @NonNull private final HttpSecurityConfig httpSecurityConfig;

  @Builder
  public DefaultAuthorizationCodeAuthProvider(@NonNull HttpSecurityConfig httpSecurityConfig) {
    super(httpSecurityConfig);
    this.httpSecurityConfig = httpSecurityConfig;
    log.info("Running with Default Authorization Provider (Non PKCE)");
  }

  @Override
  public void redirectUserToAuthorizationEndpoint(ServletResponse servletResponse)
      throws AuthenticationException {
    String authzUrl =
        format(
            "%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s",
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

  @Override
  public AccessToken requestAccessToken(
      ServletRequest servletRequest, ServletResponse servletResponse)
      throws AuthenticationException {
    String code = checkNotNull(servletRequest.getParameter("code"), "code");

    Request request =
        Request.post(httpSecurityConfig.oidcConfig().wellKnownConfiguration().tokenEndpoint())
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .bodyForm(
                Form.form()
                    .add("client_id", httpSecurityConfig.oidcConfig().clientId())
                    .add("client_secret", httpSecurityConfig.oidcConfig().clientSecret())
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", httpSecurityConfig.oidcConfig().redirectUri())
                    .build())
            .responseTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS))
            .connectTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS));

    return JsonUtils.loadJsonFromString(doExecute(request), AccessToken.class);
  }

  @Override
  public AccessToken refreshToken(String refreshToken) throws AuthenticationException {
    checkNotNull(refreshToken, "refresh_token");
    Request request =
        Request.post(httpSecurityConfig.oidcConfig().wellKnownConfiguration().tokenEndpoint())
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .bodyForm(
                Form.form()
                    .add("client_id", httpSecurityConfig.oidcConfig().clientId())
                    .add("client_secret", httpSecurityConfig.oidcConfig().clientSecret())
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build())
            .responseTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS))
            .connectTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS));

    return JsonUtils.loadJsonFromString(doExecute(request), AccessToken.class);
  }
}
