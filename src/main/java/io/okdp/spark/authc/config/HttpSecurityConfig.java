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

package io.okdp.spark.authc.config;

import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.Date.from;

import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.PersistedToken;
import io.okdp.spark.authc.provider.AuthProvider;
import io.okdp.spark.authc.provider.SessionStore;
import io.okdp.spark.authc.provider.impl.DefaultAuthorizationCodeAuthProvider;
import io.okdp.spark.authc.provider.impl.PKCEAuthorizationCodeAuthProvider;
import io.okdp.spark.authc.utils.TokenUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor(staticName = "create")
@Getter
@Accessors(fluent = true)
@Slf4j
public class HttpSecurityConfig {

  private final List<Pattern> patterns = new ArrayList<>();
  @NonNull private OidcConfig oidcConfig;
  private SessionStore sessionStore;

  /**
   * Skip authentication for the requests with the provided URL patterns
   *
   * @param patterns the URL patterns to skip authentication for
   */
  public HttpSecurityConfig authorizeRequests(String... patterns) {
    this.patterns.addAll(stream(patterns).map(Pattern::compile).collect(Collectors.toList()));
    return this;
  }

  /**
   * The token store {@link SessionStore} implementation managing the access token persistence
   *
   * @see SessionStore
   */
  public HttpSecurityConfig sessionStore(SessionStore sessionStore) {
    this.sessionStore = sessionStore;
    return this;
  }

  public PersistedToken toPersistedToken(AccessToken token) {
    return PersistedToken.builder()
        .userInfo(TokenUtils.userInfo(token.accessToken()))
        .refreshToken(token.refreshToken())
        .expiresIn(token.expiresIn())
        .expiresAt(from(now().plusSeconds(token.expiresIn())))
        .identityProvider(oidcConfig().identityProvider())
        .build();
  }

  /** Configure the auth provider */
  public AuthProvider configure() {
    switch (oidcConfig.usePKCE().toLowerCase()) {
      case "true":
        return new PKCEAuthorizationCodeAuthProvider(this);
      case "auto":
        List<String> supportedMethods =
            oidcConfig.wellKnownConfiguration().supportedPKCECodeChallengeMethods();
        return !supportedMethods.isEmpty()
            ? new PKCEAuthorizationCodeAuthProvider(this)
            : new DefaultAuthorizationCodeAuthProvider(this);
      case "false":
      default:
        return new DefaultAuthorizationCodeAuthProvider(this);
    }
  }
}
