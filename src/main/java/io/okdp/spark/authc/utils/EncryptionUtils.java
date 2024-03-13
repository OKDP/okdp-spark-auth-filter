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

import io.okdp.spark.authc.config.Constants;
import io.okdp.spark.authc.exception.CipherException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Encryption utility methods */
public class EncryptionUtils implements Constants {
  private static final int GCM_IV_LENGTH = 12;

  /**
   * @param text the plain text message to encrypt
   * @param secretKey the secret key hash to use for the encryption
   * @return the encrypted text as BASE64 string
   * @see <a
   *     href="https://gist.github.com/patrickfav/7e28d4eb4bf500f7ee8012c4a0cf7bbf">AesGcmTest.java</a>
   */
  public static String encryptToString(String text, String secretKey) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      GCMParameterSpec gcmIv = new GCMParameterSpec(128, iv);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), AES_ENCRYPTION_ALGORITHEM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmIv);
      byte[] cipherText = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
      ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
      byteBuffer.put(iv);
      byteBuffer.put(cipherText);
      return BASE64_ENCODER.encodeToString(byteBuffer.array());
    } catch (Exception e) {
      throw new CipherException(e.getMessage(), e);
    }
  }

  /**
   * @param cipherTextBase64 the base64 encrypted text
   * @param secretKey the secret key hash to use for the encryption
   * @return the unencrypted message as plain text
   * @see <a
   *     href="https://gist.github.com/patrickfav/7e28d4eb4bf500f7ee8012c4a0cf7bbf">AesGcmTest.java</a>
   */
  public static String decrypt(String cipherTextBase64, String secretKey) {
    try {
      byte[] cipherText = BASE64_DECODER.decode(cipherTextBase64.getBytes(StandardCharsets.UTF_8));
      final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      AlgorithmParameterSpec gcmIv = new GCMParameterSpec(128, cipherText, 0, GCM_IV_LENGTH);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), AES_ENCRYPTION_ALGORITHEM);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmIv);
      byte[] plainText =
          cipher.doFinal(cipherText, GCM_IV_LENGTH, cipherText.length - GCM_IV_LENGTH);
      return new String(plainText, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new CipherException(e.getMessage(), e);
    }
  }
}
