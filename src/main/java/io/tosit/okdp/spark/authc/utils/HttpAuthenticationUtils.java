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
package io.tosit.okdp.spark.authc.utils;

import io.tosit.okdp.spark.authc.config.Constants;
import io.tosit.okdp.spark.authc.exception.OidcClientException;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Slf4j
public class HttpAuthenticationUtils implements Constants {

    public static Optional<String> getCookieValue(String cookieName, ServletRequest request) {
        Optional<Cookie[]> maybeCookie = ofNullable(((HttpServletRequest) request).getCookies());
        return maybeCookie.flatMap(cookies -> Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieName))
                .map(Cookie::getValue)
                .findAny());
    }

    public static String domain(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new OidcClientException(e.getMessage(), e);
        }
    }
}
