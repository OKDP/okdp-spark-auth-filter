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

package io.okdp.spark.authc.model;

import static java.nio.charset.StandardCharsets.US_ASCII;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.okdp.spark.authc.exception.OidcClientException;
import io.okdp.spark.authc.utils.JsonUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AuthState {

  @JsonProperty("state")
  private String state;

  @JsonProperty("code_verifier")
  private String codeVerifier;

  @JsonProperty("code_challenge")
  private String codeChallenge;

  /** Converts this object to json string */
  public String toJson() {
    return JsonUtils.toJson(this);
  }

  /**
   * Generates a random state containing: state: a random string code_verifier: A random string
   * which conform to <a href="https://tools.ietf.org/html/rfc7636#section-4.1">specification</a>
   * code_challenge: derived from the code_verifier
   */
  public static AuthState randomState() {
    String state = randomString(16);
    String codeVerifier = randomString(64);
    String codeChallenge = createCodeChallenge(codeVerifier);
    return new AuthState(state, codeVerifier, codeChallenge);
  }

  /** Generates a random PKCE code_verifier as stated */
  private static String randomString(int nbBytes) {
    SecureRandom random = new SecureRandom();
    byte[] array = new byte[nbBytes];
    random.nextBytes(array);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(array);
  }

  /** Creates an SHA-256 challenge from a code verifier */
  private static String createCodeChallenge(String codeVerifier) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(US_ASCII)));
    } catch (NoSuchAlgorithmException e) {
      throw new OidcClientException(e.getMessage(), e);
    }
  }
}
