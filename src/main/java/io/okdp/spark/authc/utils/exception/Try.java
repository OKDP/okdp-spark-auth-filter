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

package io.okdp.spark.authc.utils.exception;

import io.okdp.spark.authc.exception.AuthenticationException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(staticName = "of")
public class Try<T> {

  @NonNull private Supplier<T> supplier;

  public T onException(Consumer<AuthenticationException> onException) {
    try {
      return supplier.get();
    } catch (AuthenticationException e) {
      onException.accept(e);
    }
    return null;
  }
}
