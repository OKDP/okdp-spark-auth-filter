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

package io.tosit.okdp.spark.authc.common;

import java.io.IOException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;

public interface CommonTest {
  String TEST_DEX_WELL_KNOWN_CONFIGURATION =
      "{\n"
          + "  \"issuer\": \"https://dex.okdp.local/dex\",\n"
          + "  \"authorization_endpoint\": \"https://dex.okdp.local/dex/auth\",\n"
          + "  \"token_endpoint\": \"https://dex.okdp.local/dex/token\",\n"
          + "  \"jwks_uri\": \"https://dex.okdp.local/dex/keys\",\n"
          + "  \"userinfo_endpoint\": \"https://dex.okdp.local/dex/userinfo\",\n"
          + "  \"device_authorization_endpoint\": \"https://dex.okdp.local/dex/device/code\",\n"
          + "  \"grant_types_supported\": [\n"
          + "    \"authorization_code\",\n"
          + "    \"refresh_token\",\n"
          + "    \"urn:ietf:params:oauth:grant-type:device_code\"\n"
          + "  ],\n"
          + "  \"response_types_supported\": [\n"
          + "    \"code\"\n"
          + "  ],\n"
          + "  \"subject_types_supported\": [\n"
          + "    \"public\"\n"
          + "  ],\n"
          + "  \"id_token_signing_alg_values_supported\": [\n"
          + "    \"RS256\"\n"
          + "  ],\n"
          + "  \"code_challenge_methods_supported\": [\n"
          + "    \"S256\",\n"
          + "    \"plain\"\n"
          + "  ],\n"
          + "  \"scopes_supported\": [\n"
          + "    \"openid\",\n"
          + "    \"email\",\n"
          + "    \"groups\",\n"
          + "    \"profile\",\n"
          + "    \"offline_access\"\n"
          + "  ],\n"
          + "  \"token_endpoint_auth_methods_supported\": [\n"
          + "    \"client_secret_basic\",\n"
          + "    \"client_secret_post\"\n"
          + "  ],\n"
          + "  \"claims_supported\": [\n"
          + "    \"iss\",\n"
          + "    \"sub\",\n"
          + "    \"aud\",\n"
          + "    \"iat\",\n"
          + "    \"exp\",\n"
          + "    \"email\",\n"
          + "    \"email_verified\",\n"
          + "    \"locale\",\n"
          + "    \"name\",\n"
          + "    \"preferred_username\",\n"
          + "    \"at_hash\"\n"
          + "  ]\n"
          + "}";

  abstract class NoopCloseableHttpClient extends CloseableHttpClient {
    @Override
    protected CloseableHttpResponse doExecute(
        final HttpHost target, final ClassicHttpRequest request, final HttpContext context)
        throws IOException {
      return null;
    }
  }
}
