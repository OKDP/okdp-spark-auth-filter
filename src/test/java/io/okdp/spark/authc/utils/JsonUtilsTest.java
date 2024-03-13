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

package io.okdp.spark.authc.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.okdp.spark.authc.common.CommonTest;
import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.WellKnownConfiguration;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest implements CommonTest {
  @Test
  public void should_parse_json_wel_known_configuration() {
    WellKnownConfiguration configuration =
        JsonUtils.loadJsonFromString(
            TEST_DEX_WELL_KNOWN_CONFIGURATION, WellKnownConfiguration.class);
    assertThat(configuration.authorizationEndpoint()).isEqualTo("https://dex.okdp.local/dex/auth");
    assertThat(configuration.tokenEndpoint()).isEqualTo("https://dex.okdp.local/dex/token");
    assertThat(configuration.userInfoEndpoint()).isEqualTo("https://dex.okdp.local/dex/userinfo");
    assertThat(configuration.scopesSupported())
        .isEqualTo(asList("openid", "email", "groups", "profile", "offline_access"));
  }

  @Test
  public void should_parse_jwt_token_response() {
    String tokenResponse =
        "{\n"
            + "  \"access_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM...\",\n"
            + "  \"token_type\": \"bearer\",\n"
            + "  \"expires_in\": 86399,\n"
            + "  \"refresh_token\": \"ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr\",\n"
            + "  \"id_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2...\"\n"
            + "}";
    AccessToken token = JsonUtils.loadJsonFromString(tokenResponse, AccessToken.class);

    assertThat(token.accessToken()).isEqualTo("eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM...");
    assertThat(token.expiresIn()).isEqualTo(86399);
    assertThat(token.idToken())
        .isEqualTo("eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2...");
    assertThat(token.refreshToken())
        .isEqualTo("ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr");
    assertThat(token.tokenType()).isEqualTo("bearer");
  }
}
