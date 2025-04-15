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
import static io.okdp.spark.authc.utils.PreconditionsUtils.checkState;
import static java.lang.String.format;
import static org.apache.hc.core5.util.Timeout.ofSeconds;

import io.okdp.spark.authc.config.Constants;
import io.okdp.spark.authc.config.HttpSecurityConfig;
import io.okdp.spark.authc.exception.AuthenticationException;
import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.AuthState;
import io.okdp.spark.authc.provider.AuthProvider;
import io.okdp.spark.authc.utils.HttpAuthenticationUtils;
import io.okdp.spark.authc.utils.JsonUtils;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;

/**
 * The Confidential/Public Client Oauth2/oidc Authorization Code grant provider with PKCE support
 * implementation
 *
 * @see AuthProvider
 */
@Getter
@Accessors(fluent = true)
@Slf4j
public class PKCEAuthorizationCodeAuthProvider extends AbstractAuthorizationCodeAuthProvider {

  @NonNull private final HttpSecurityConfig httpSecurityConfig;

  @Builder
  public PKCEAuthorizationCodeAuthProvider(@NonNull HttpSecurityConfig httpSecurityConfig) {
    super(httpSecurityConfig);
    this.httpSecurityConfig = httpSecurityConfig;
    log.info("Running with PKCE Authorization Provider");
  }

  @Override
  public void redirectUserToAuthorizationEndpoint(ServletResponse servletResponse)
      throws AuthenticationException {
    AuthState authState = AuthState.randomState();
    String authzUrl =
        format(
            "%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
            httpSecurityConfig.oidcConfig().wellKnownConfiguration().authorizationEndpoint(),
            httpSecurityConfig.oidcConfig().clientId(),
            httpSecurityConfig.oidcConfig().redirectUri(),
            httpSecurityConfig.oidcConfig().responseType(),
            httpSecurityConfig.oidcConfig().scope(),
            authState.state(),
            authState.codeChallenge());

    try {
      Cookie cookie = httpSecurityConfig.sessionStore().save(authState);
      ((HttpServletResponse) servletResponse).addCookie(cookie);
      servletResponse.setContentType("text/html;charset=UTF-8");
      servletResponse
          .getWriter()
          .print(
              format(
                  "<script type=\"text/javascript\">window.location.href = '%s'</script>",
                  authzUrl));
    } catch (IOException e) {
      throw new AuthenticationException(e.getMessage(), e);
    }
  }

  @Override
  public AccessToken requestAccessToken(
      ServletRequest servletRequest, ServletResponse servletResponse)
      throws AuthenticationException {
    String code = checkNotNull(servletRequest.getParameter("code"), "code");
    String state = checkNotNull(servletRequest.getParameter("state"), "state");

    // Extract the auth state from the http auth state cookie if present
    Optional<String> maybeAuthStateCookie =
        HttpAuthenticationUtils.getCookieValue(Constants.AUTH_STATE_COOKE_NAME, servletRequest);

    String authStateAsJson =
        maybeAuthStateCookie.orElseThrow(
            () ->
                new AuthenticationException(
                    401, format("The cookie '%s' is not present", AUTH_STATE_COOKE_NAME)));

    AuthState authState = httpSecurityConfig.sessionStore().readPKCEState(authStateAsJson);

    checkState(
        authState.state(),
        state,
        format(
            "Invalid state, the state does not match with the oidc provider state, expected: <%s>, provided: <%s>. Please retry!",
            authState.state(), state));

    String codeVerifier = checkNotNull(authState.codeVerifier(), "code_verifier");

    Form form =
        Form.form()
            .add("client_id", httpSecurityConfig.oidcConfig().clientId())
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("code_verifier", codeVerifier)
            .add("redirect_uri", httpSecurityConfig.oidcConfig().redirectUri());

    Form newform =
        Optional.ofNullable(httpSecurityConfig.oidcConfig().clientSecret())
            .map(clientSecret -> form.add("client_secret", clientSecret))
            .orElse(form);

    Request request =
        Request.post(httpSecurityConfig.oidcConfig().wellKnownConfiguration().tokenEndpoint())
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .bodyForm(newform.build())
            .responseTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS))
            .connectTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS));

    // Remove the auth state cookie
    Cookie cookie = httpSecurityConfig.sessionStore().save((AuthState) null);
    ((HttpServletResponse) servletResponse).addCookie(cookie);

    return JsonUtils.loadJsonFromString(doExecute(request), AccessToken.class);
  }

  @Override
  public AccessToken refreshToken(String refreshToken) throws AuthenticationException {
    checkNotNull(refreshToken, "refresh_token");
    Form form =
        Form.form()
            .add("client_id", httpSecurityConfig.oidcConfig().clientId())
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken);

    Form newform =
        Optional.ofNullable(httpSecurityConfig.oidcConfig().clientSecret())
            .map(clientSecret -> form.add("client_secret", clientSecret))
            .orElse(form);

    Request request =
        Request.post(httpSecurityConfig.oidcConfig().wellKnownConfiguration().tokenEndpoint())
            .addHeader("cache-control", "no-cache")
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .bodyForm(newform.build())
            .responseTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS))
            .connectTimeout(ofSeconds(OIDC_REQUEST_TIMEOUT_SECONDS));

    return JsonUtils.loadJsonFromString(doExecute(request), AccessToken.class);
  }
}
