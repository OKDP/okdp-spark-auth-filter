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

package io.okdp.spark.authc.provider;

import io.okdp.spark.authc.model.AuthState;
import io.okdp.spark.authc.model.PersistedToken;

/** Each concrete token store should implement this interface */
public interface SessionStore {

  /**
   * Save the access token in a {@link T}
   *
   * @param PersistedToken the persisted token issued from the response from the oidc provider
   * @return {@link T} containing the saved access token
   */
  <T> T save(PersistedToken PersistedToken);

  /**
   * Save the PKCE state in a {@link T}
   *
   * @param authState the PKCE state
   */
  <T> T save(AuthState authState);

  /**
   * Read the access token in a {@link T}
   *
   * @param value the access token string value saved by the SessionStore provider
   * @return {@link T} containing the resulting access token
   */
  <T> T readToken(String value);

  /**
   * Read the PKCE state in a {@link T}
   *
   * @param value the PKCE state string value saved by the SessionStore provider
   * @return {@link T} containing the resulting access token
   */
  <T> T readPKCEState(String value);
}
