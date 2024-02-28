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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.mock;

import io.tosit.okdp.spark.authc.exception.AuthenticationException;
import io.tosit.okdp.spark.authc.utils.exception.Try;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

public class TryTest {

  @Test
  public void should_rethrow_original_exception() {
    // Given
    HttpServletResponse response = mock(HttpServletResponse.class);

    // when
    String validCall =
        Try.of(() -> "OK").onException(e -> response.setStatus(e.getHttpStatusCode()));

    Throwable nonValidCall =
        catchThrowable(
            () ->
                Try.of(
                        () -> {
                          throw new AuthenticationException(400, "Bad Request");
                        })
                    .onException(e -> response.setStatus(e.getHttpStatusCode())));

    // Then
    assertThat(validCall).isEqualTo("OK");
    assertThat(nonValidCall)
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("Bad Request");
    assertThat(((AuthenticationException) nonValidCall).getHttpStatusCode()).isEqualTo(400);
  }
}
