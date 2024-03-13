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

import static io.okdp.spark.authc.utils.PreconditionsUtils.assertSupportedScopes;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

public class PreconditionsUtilsTest {

  @Test
  public void should_throw_an_exception_on_null_value() {
    // when
    Throwable emptyValue =
        catchThrowable(
            () -> {
              PreconditionsUtils.checkNotNull("", "label_01");
            });

    Throwable nullValue =
        catchThrowable(
            () -> {
              PreconditionsUtils.checkNotNull(null, "label_01");
            });

    Throwable spacesValue =
        catchThrowable(
            () -> {
              PreconditionsUtils.checkNotNull("    ", "label_01");
            });

    // Then
    assertThat(emptyValue)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("label_01");

    assertThat(nullValue).isInstanceOf(NullPointerException.class).hasMessageContaining("label_01");

    assertThat(spacesValue)
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("label_01");
  }

  @Test
  public void should_not_throw_any_exception() {

    // when
    ThrowingCallable validValue =
        () -> PreconditionsUtils.checkNotNull("label_01_value", "label_01");

    // Then
    assertThatCode(validValue).doesNotThrowAnyException();
  }

  @Test
  public void should_assert_valid_scopes() {
    // when
    ThrowingCallable validScopes =
        () ->
            assertSupportedScopes(
                asList("openid", "profile", "email", "roles", "offline_access"),
                "openid+profile+email",
                "scope");

    ThrowingCallable unsupportedScopes =
        () ->
            assertSupportedScopes(
                asList("openid", "profile", "email", "roles", "offline_access"),
                "openid+profile+email+groups+roles+offline_access",
                "scope");

    // Then
    assertThatCode(validScopes).doesNotThrowAnyException();
    assertThatCode(unsupportedScopes)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("'[groups]'")
        .hasMessageContaining("scope");
  }
}
