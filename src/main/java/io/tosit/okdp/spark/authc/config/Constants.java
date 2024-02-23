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

import java.util.Base64;

public interface Constants {
    /* OIDC parameters */
    String AUTH_ISSUER_URI = "issuer-uri";
    String AUTH_CLIENT_ID = "client-id";
    String AUTH_CLIENT_SECRET = "client-secret";
    String AUTH_REDIRECT_URI = "redirect-uri";
    String AUTH_SCOPE = "scope";
    String AUTH_ISSUER_WELL_KNOWN_CONFIGURATION = "/.well-known/openid-configuration";
    int OIDC_REQUEST_TIMEOUT_SECONDS = 30;

    /* Encoders */
    Base64.Decoder BASE64_DECODER = Base64.getDecoder();
}
