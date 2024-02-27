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
package io.tosit.okdp.spark.authc.provider.store;

import io.tosit.okdp.spark.authc.model.AccessToken;
import io.tosit.okdp.spark.authc.model.PersistedToken;
import io.tosit.okdp.spark.authc.provider.TokenStore;
import io.tosit.okdp.spark.authc.utils.JsonUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.Cookie;
import java.sql.Date;

import static io.tosit.okdp.spark.authc.utils.CompressionUtils.compressToString;
import static io.tosit.okdp.spark.authc.utils.CompressionUtils.decompress;
import static io.tosit.okdp.spark.authc.utils.EncryptionUtils.decrypt;
import static io.tosit.okdp.spark.authc.utils.EncryptionUtils.encryptToString;
import static io.tosit.okdp.spark.authc.utils.JsonUtils.loadJsonFromString;
import static io.tosit.okdp.spark.authc.utils.TokenUtils.payload;
import static java.time.Instant.now;

@RequiredArgsConstructor(staticName = "of")
public class CookieTokenStore implements TokenStore {

    @NonNull
    private String cookieName;
    @NonNull
    private String cookieDomain;
    @NonNull
    private String encryptionKey;
    @NonNull
    private Integer cookieMaxAgeSeconds;

    /**
     * Compress, encrypt and save the access token in a {@link Cookie}
     *
     * @param accessToken the access token response from the oidc server
     * @return {@link Cookie} containing the compressed and encrypted access token
     */
    @Override
    @SuppressWarnings("unchecked")
    public Cookie save(AccessToken accessToken) {
        // Reduce the token size by saving the token payload part only
        String savedToken = JsonUtils.toJson(PersistedToken.builder()
                .accessTokenPayload(payload(accessToken.accessToken()))
                .refreshToken(accessToken.refreshToken())
                .expiresIn(accessToken.expiresIn())
                .expiresAt(Date.from(now().plusSeconds(accessToken.expiresIn())))
                .build());
        // Compress the access token to overcome 4KB cookie limit (depends on the OIDC providers and their config)
        // Encrypt the content to prevent token exposure
        Cookie cookie = new Cookie(cookieName, encryptToString(compressToString(savedToken), encryptionKey));
        cookie.setMaxAge(cookieMaxAgeSeconds);
        // Additional enforcements
        cookie.setDomain(cookieDomain);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
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
        return loadJsonFromString(decompress(decrypt(value, encryptionKey)), PersistedToken.class);
    }

}
