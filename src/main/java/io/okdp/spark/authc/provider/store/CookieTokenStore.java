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

package io.okdp.spark.authc.provider.store;

import static io.okdp.spark.authc.utils.CompressionUtils.compressToString;
import static io.okdp.spark.authc.utils.EncryptionUtils.encryptToString;
import static java.time.Instant.now;
import static java.util.Date.*;
import static java.util.Optional.ofNullable;

import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.PersistedToken;
import io.okdp.spark.authc.provider.TokenStore;
import io.okdp.spark.authc.utils.CompressionUtils;
import io.okdp.spark.authc.utils.EncryptionUtils;
import io.okdp.spark.authc.utils.JsonUtils;
import io.okdp.spark.authc.utils.TokenUtils;
import javax.servlet.http.Cookie;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cookie token based storage implementation
 *
 * @see TokenStore
 */
@Slf4j
@RequiredArgsConstructor(staticName = "of")
public class CookieTokenStore implements TokenStore {

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
  public Cookie save(AccessToken accessToken) {
    // Reduce the token size by saving the token payload part only (user info)
    // Compress the access token to overcome 4KB cookie limit (depends on the OIDC providers and
    // their config)
    // Encrypt the content to prevent token corruption
    String cookieValue =
        ofNullable(accessToken)
            .map(
                token ->
                    JsonUtils.toJson(
                        PersistedToken.builder()
                            .userInfo(TokenUtils.userInfo(token.accessToken()))
                            .refreshToken(token.refreshToken())
                            .expiresIn(token.expiresIn())
                            .expiresAt(from(now().plusSeconds(token.expiresIn())))
                            .build()))
            .map(tokenAsJson -> encryptToString(compressToString(tokenAsJson), encryptionKey))
            .orElse("");

    Cookie cookie = new Cookie(cookieName, cookieValue);
    cookie.setMaxAge(cookieMaxAgeSeconds);
    // Additional enforcements
    cookie.setDomain(cookieDomain);
    cookie.setHttpOnly(true);
    cookie.setSecure(isSecure);
    cookie.setPath("/;SameSite=Strict;");
    return cookie;
  }

  /**
   * Un-encrypt, uncompress and load the access token in a {@link PersistedToken}
   *
   * @param value the access token value saved in the {@link Cookie}
   * @return {@link PersistedToken} containing the access token
   */
  @Override
  public PersistedToken readToken(String value) {
    return JsonUtils.loadJsonFromString(
        CompressionUtils.decompress(EncryptionUtils.decrypt(value, encryptionKey)),
        PersistedToken.class);
  }
}
