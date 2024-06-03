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

import static io.okdp.spark.authc.utils.PreconditionsUtils.assertSupportePKCE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.List;
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
  public void should_support_pkce_for_public_clients() {

    // when
    ThrowingCallable validConf1 =
        () -> assertSupportePKCE(asList("plain", "S256"), "auto", null, "client-secret");

    ThrowingCallable validConf2 =
        () -> assertSupportePKCE(asList("plain", "S256"), "true", null, "client-secret");

    ThrowingCallable validConf3 =
        () -> assertSupportePKCE(List.of(), "true", null, "client-secret");

    ThrowingCallable validConf4 =
        () -> assertSupportePKCE(List.of(), "false", "ClientSecret@", "client-secret");

    ThrowingCallable invalidConf1 =
        () -> assertSupportePKCE(asList("plain", "S256"), "false", null, "client-secret");

    ThrowingCallable invalidConf2 =
        () -> assertSupportePKCE(List.of(), "auto", null, "client-secret");

    // Then
    assertThatCode(validConf1).doesNotThrowAnyException();
    assertThatCode(validConf2).doesNotThrowAnyException();
    assertThatCode(validConf3).doesNotThrowAnyException();
    assertThatCode(validConf4).doesNotThrowAnyException();

    assertThatCode(invalidConf1)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("use-pkce=false")
        .hasMessageContaining("client-secret");

    assertThatCode(invalidConf2)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("use-pkce=auto")
        .hasMessageContaining("client-secret");
  }
}
