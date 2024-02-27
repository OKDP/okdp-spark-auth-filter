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
package io.tosit.okdp.spark.authc.config;

import io.tosit.okdp.spark.authc.provider.AuthProvider;
import io.tosit.okdp.spark.authc.provider.OidcAuthProvider;
import io.tosit.okdp.spark.authc.provider.TokenStore;
import io.tosit.okdp.spark.authc.provider.store.CookieTokenStore;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@RequiredArgsConstructor(staticName = "create")
@Getter
@Accessors(fluent = true)
public class HttpSecurityConfig {

    private final List<Pattern> patterns = new ArrayList<>();
    @NonNull
    private OidcConfig oidcConfig;
    private TokenStore tokenStore;

    /**
     * Skip authentication for the requests with the provided URL patterns
     *
     * @param patterns the URL patterns to skip authentication for
     * @return {@link HttpSecurityConfig}
     */
    public HttpSecurityConfig authorizeRequests(String... patterns) {
        this.patterns.addAll(stream(patterns).map(Pattern::compile).collect(Collectors.toList()));
        return this;
    }

    /**
     * The location where to store the access token
     *
     * @param tokenStore @see {@link CookieTokenStore}
     * @return {@link HttpSecurityConfig}
     */
    public HttpSecurityConfig tokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
        return this;
    }

    /**
     * Configure the security rules and return the {@link AuthProvider}
     *
     * @return {@link OidcAuthProvider}
     */
    public AuthProvider configure() {
        return new OidcAuthProvider(this);
    }

}
