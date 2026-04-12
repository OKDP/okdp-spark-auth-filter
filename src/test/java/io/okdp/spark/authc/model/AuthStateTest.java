/*
 *    Copyright 2024 The OKDP Authors.
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

package io.okdp.spark.authc.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

public class AuthStateTest {

  @Test
  public void test() {

    // When
    AuthState authState = AuthState.randomState();

    // Then
    assertThat(authState.state()).isNotBlank();
    assertThat(authState.codeVerifier().length()).isBetween(43, 128);
    assertThat(authState.codeVerifier()).isASCII();
    assertThat(authState.returnUrl()).isNull();
  }

  @Test
  public void should_carry_return_url_through_oidc_roundtrip() {
    // Given a deep-link URL the user initially requested (e.g. a Spark History Server job page)
    String returnUrl = "/history/spark-abc123/jobs/";

    // When
    AuthState authState = AuthState.randomState(returnUrl);

    // Then - the return URL is preserved alongside the PKCE state so the filter can redirect the
    // user back to their original deep-link after a successful OIDC authentication
    assertThat(authState.state()).isNotBlank();
    assertThat(authState.codeVerifier().length()).isBetween(43, 128);
    assertThat(authState.returnUrl()).isEqualTo(returnUrl);
  }

  @Test
  public void should_survive_json_serialization_with_return_url() {
    // Given
    AuthState original = AuthState.randomState("/history/spark-xyz/stages/");

    // When - the state is serialized to JSON (how it is stored in the encrypted state cookie)
    String json = original.toJson();
    AuthState restored =
        io.okdp.spark.authc.utils.JsonUtils.loadJsonFromString(json, AuthState.class);

    // Then - every field, including the return URL, round-trips through JSON
    assertThat(restored.state()).isEqualTo(original.state());
    assertThat(restored.codeVerifier()).isEqualTo(original.codeVerifier());
    assertThat(restored.codeChallenge()).isEqualTo(original.codeChallenge());
    assertThat(restored.returnUrl()).isEqualTo(original.returnUrl());
  }
}
