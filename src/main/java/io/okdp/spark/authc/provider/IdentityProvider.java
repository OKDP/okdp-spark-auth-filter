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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.okdp.spark.authc.model.UserInfo;
import io.okdp.spark.authc.provider.impl.EmailIdentityProvider;
import io.okdp.spark.authc.provider.impl.GoogleIdentityProvider;
import io.okdp.spark.authc.provider.impl.SubIdentityProvider;

@JsonTypeInfo(
    include = JsonTypeInfo.As.PROPERTY,
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = EmailIdentityProvider.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = EmailIdentityProvider.class, name = "email"),
  @JsonSubTypes.Type(value = SubIdentityProvider.class, name = "sub"),
  @JsonSubTypes.Type(value = GoogleIdentityProvider.class, name = "google")
})

/** Each concrete Identity provider should implement this interface */
public interface IdentityProvider {

  /**
   * Extract the id form the userInfo ({@link UserInfo})
   *
   * @param UserInfo the userInfo extracted from the user's access token
   * @return the id as a ({@link String})
   */
  String extractId(UserInfo userInfo);
}
