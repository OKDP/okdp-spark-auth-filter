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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.okdp.spark.authc.provider.IdentityProvider;
import io.okdp.spark.authc.utils.JsonUtils;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class PersistedToken {

  @JsonProperty("identity_provider")
  private IdentityProvider identityProvider;

  @JsonProperty("access_token_payload")
  private UserInfo userInfo;

  @JsonProperty("expires_in")
  private int expiresIn;

  @JsonProperty("expires_at")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private Date expiresAt;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonIgnore
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt.toInstant());
  }

  @JsonIgnore
  public boolean hasRefreshToken() {
    return !Strings.nullToEmpty(refreshToken).trim().isEmpty();
  }

  /** Convert this object to json */
  public String toJson() {
    return JsonUtils.toJson(this);
  }

  /** Extract the id from UserInfo */
  public String id() {
    return identityProvider.extractId(this.userInfo);
  }

  /**
   * Ignore refresh token
   *
   * @param ignore: whether to ignore refresh token storage
   */
  public PersistedToken ignoreRefreshToken(boolean ignore) {
    if (ignore) {
      this.refreshToken = "";
    }
    return this;
  }
}
