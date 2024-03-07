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

public class EncryptionUtilsTest {

  @Test
  public void should_encrypt_and_decrypt_text() {
    // Given
    String text = "message to encrypt";
    // Generated using: openssl enc -aes-128-cbc -k MyPassPhrapse -P -md sha1 -pbkdf2
    String secretKey = "E132A72E815F496FFC49B3EC876754F4";

    // When
    String encrypted = EncryptionUtils.encryptToString(text, secretKey);
    String decrypted = EncryptionUtils.decrypt(encrypted, secretKey);

    // Then
    assertThat(text).isEqualTo(decrypted);
  }
}
