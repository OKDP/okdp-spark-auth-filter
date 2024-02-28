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

package io.tosit.okdp.spark.authz;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static scala.collection.JavaConverters.asScalaSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OidcGroupMappingServiceProviderTest {

  @Test
  public void should_map_membership_groups_for_user() {
    // Given
    OidcGroupMappingServiceProvider groupMappingServiceProvider =
        new OidcGroupMappingServiceProvider();

    // When
    OidcGroupMappingServiceProvider.addUserAndGroups("user1@example.org", asList("team1", "team2"));
    OidcGroupMappingServiceProvider.addUserAndGroups("user2@example.org", List.of("team2"));
    // Then
    assertThat(groupMappingServiceProvider.getGroups("user1@example.org"))
        .isEqualTo(asScalaSet(new HashSet<>(Arrays.asList("team1", "team2"))).toSet());
    assertThat(groupMappingServiceProvider.getGroups("user2@example.org"))
        .isEqualTo(asScalaSet(new HashSet<>(List.of("team2"))).toSet());
  }

  @Test
  public void should_return_an_empty_set_user_with_no_groups() {
    // Given
    OidcGroupMappingServiceProvider groupMappingServiceProvider =
        new OidcGroupMappingServiceProvider();

    // Then
    assertThat(groupMappingServiceProvider.getGroups("user2@example.org"))
        .isEqualTo(asScalaSet(emptySet()).toSet());
  }
}
