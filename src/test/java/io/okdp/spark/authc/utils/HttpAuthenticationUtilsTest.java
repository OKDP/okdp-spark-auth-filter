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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class HttpAuthenticationUtilsTest {

  @Test
  public void should_extract_domain_from_url() {
    // Given
    String url = "https://spark-history.example.com/home";

    // When
    String domain = HttpAuthenticationUtils.domain(url);

    // Then
    assertThat(domain).isEqualTo("spark-history.example.com");
  }

  @Test
  public void should_check_secure_connection() {
    // Given
    String secureUrl = "https://spark-history.example.com/home";
    String nonSecureUrl = "http://spark-history.example.com/home";

    // When
    Boolean isSecure = HttpAuthenticationUtils.isSecure(secureUrl);
    Boolean isNotSecure = !HttpAuthenticationUtils.isSecure(nonSecureUrl);

    // Then
    assertThat(isSecure).isTrue();
    assertThat(isNotSecure).isTrue();
  }
}
