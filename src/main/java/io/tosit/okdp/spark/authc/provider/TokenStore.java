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

package io.tosit.okdp.spark.authc.provider;

import io.tosit.okdp.spark.authc.model.AccessToken;

public interface TokenStore {

  /**
   * Save the access token in a {@link T}
   *
   * @param accessToken the access token response from the oidc server
   * @return {@link T} containing the saved access token
   */
  <T> T save(AccessToken accessToken);

  /**
   * Read the access token in a {@link T}
   *
   * @param value the access token string value saved by the TokenStore provider
   * @return {@link T} containing the resulting access token
   */
  <T> T readToken(String value);
}
