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

package io.okdp.spark.authc.provider.impl.store;

import static io.okdp.spark.authc.config.Constants.AUTH_STATE_COOKE_NAME;
import static io.okdp.spark.authc.utils.CompressionUtils.compressToString;
import static io.okdp.spark.authc.utils.EncryptionUtils.encryptToString;
import static java.util.Optional.ofNullable;

import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.AuthState;
import io.okdp.spark.authc.model.PersistedToken;
import io.okdp.spark.authc.provider.SessionStore;
import io.okdp.spark.authc.utils.CompressionUtils;
import io.okdp.spark.authc.utils.EncryptionUtils;
import io.okdp.spark.authc.utils.JsonUtils;
import java.util.Optional;
import javax.servlet.http.Cookie;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cookie token/Auth state based storage implementation
 *
 * @see SessionStore
 */
@Slf4j
@RequiredArgsConstructor(staticName = "of")
public class CookieSessionStore implements SessionStore {

  @NonNull private String cookieName;
  @NonNull private String cookieDomain;
  @NonNull private Boolean isSecure;
  @NonNull private String encryptionKey;
  @NonNull private Integer cookieMaxAgeSeconds;

  /**
   * Compress, encrypt and save the access token in a {@link Cookie}
   *
   * <p>If the provided {@link AccessToken} is null, save an empty value in a cookie.
   *
   * @param accessToken the access token response from the oidc provider
   * @return {@link Cookie} containing the compressed and encrypted access token
   */
  @Override
  @SuppressWarnings("unchecked")
  public Cookie save(PersistedToken persistedToken) {
    // Reduce the token size by saving the token payload part only (user info)
    // Compress the access token to overcome 4KB cookie limit (depends on the OIDC providers and
    // their config)
    // Encrypt the content to prevent token corruption
    String cookieValue =
        ofNullable(persistedToken)
            .map(token -> persistedToken.toJson())
            .map(tokenAsJson -> encryptToString(compressToString(tokenAsJson), encryptionKey))
            .orElse("");

    int maxAge =
        Optional.of(cookieValue).filter(v -> !v.isBlank()).map(v -> cookieMaxAgeSeconds).orElse(0);

    return CookieFactory.of(cookieName, cookieValue, cookieDomain, isSecure, maxAge).newCookie();
  }

  /**
   * Encrypt and save the PKCE state in a {@link Cookie}
   *
   * <p>If the provided {@link AuthState} is null, save an empty value in a cookie.
   *
   * @param authState the random generated PKCE state
   * @return {@link Cookie} containing the compressed and encrypted access token
   */
  @Override
  @SuppressWarnings("unchecked")
  public Cookie save(AuthState authState) {
    // Reduce the token size by saving the token payload part only (user info)
    // Compress the access token to overcome 4KB cookie limit (depends on the OIDC providers and
    // their config)
    // Encrypt the content to prevent token corruption
    String cookieValue =
        ofNullable(authState)
            .map(state -> authState.toJson())
            .map(state -> encryptToString(state, encryptionKey))
            .orElse("");

    int maxAge = Optional.of(cookieValue).filter(v -> !v.isBlank()).map(v -> 5 * 60).orElse(0);

    Cookie cookie =
        CookieFactory.of(AUTH_STATE_COOKE_NAME, cookieValue, cookieDomain, isSecure, maxAge)
            .newCookie();
    cookie.setPath("/");

    return cookie;
  }

  /**
   * Un-encrypt, uncompress and load the access token in a {@link PersistedToken}
   *
   * @param value the access token value saved in the {@link Cookie}
   * @return {@link PersistedToken} containing the access token
   */
  @Override
  @SuppressWarnings("unchecked")
  public PersistedToken readToken(String value) {
    return JsonUtils.loadJsonFromString(
        CompressionUtils.decompress(EncryptionUtils.decrypt(value, encryptionKey)),
        PersistedToken.class);
  }

  /**
   * Un-encrypt and load the access token in a {@link PersistedToken}
   *
   * @param value the PKCE state value saved in the {@link Cookie}
   * @return {@link AuthState} containing the PKCE state
   */
  @Override
  @SuppressWarnings("unchecked")
  public AuthState readPKCEState(String value) {
    return JsonUtils.loadJsonFromString(
        EncryptionUtils.decrypt(value, encryptionKey), AuthState.class);
  }

  @RequiredArgsConstructor(staticName = "of")
  public static class CookieFactory {

    @NonNull private String cookieName;
    @NonNull private String cookieValue;
    @NonNull private String cookieDomain;
    @NonNull private Boolean isSecure;
    @NonNull private Integer cookieMaxAgeSeconds;

    public Cookie newCookie() {
      Cookie cookie = new Cookie(cookieName, cookieValue);
      cookie.setMaxAge(cookieMaxAgeSeconds);
      // Additional enforcements
      cookie.setDomain(cookieDomain);
      cookie.setHttpOnly(true);
      cookie.setSecure(isSecure);
      cookie.setPath("/;SameSite=Strict;");
      return cookie;
    }
  }
}
