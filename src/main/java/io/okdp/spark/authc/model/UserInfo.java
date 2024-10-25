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

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

  @JsonProperty("sub")
  private String sub;

  @JsonProperty("name")
  private String name;

  @JsonProperty("email")
  private String email;

  @JsonProperty("groups")
  private List<String> groups = emptyList();

  @JsonProperty("roles")
  private List<String> roles = emptyList();

  /**
   * Return groups or roles depending on the OIDC provider (Ex.: dex -> groups, some others: roles)
   *
   * @return the list of the groups or roles
   */
  @JsonIgnore
  public List<String> getGroupsAndRoles() {
    return Stream.concat(groups.stream(), roles.stream()).collect(Collectors.toList());
  }

  public static UserInfo fromJWTClaim(JWTClaimsSet claim) {
    UserInfo user = new UserInfo();
    user.sub = claim.getSubject();
    try {
      user.name = claim.getStringClaim("name");
    } catch (ParseException e) {
      user.name = null;
    }
    try {
      user.email = claim.getStringClaim("email");
    } catch (ParseException e) {
      user.email = null;
    }
    try {
      List<Object> value = claim.getListClaim("groups");
      if (value != null) user.groups = value.stream().map(v -> (String) v).toList();
    } catch (ParseException | ClassCastException e) {
    }
    try {
      List<Object> value = claim.getListClaim("roles");
      if (value != null) user.roles = value.stream().map(v -> (String) v).toList();
    } catch (ParseException | ClassCastException e) {
    }
    return user;
  }
}
