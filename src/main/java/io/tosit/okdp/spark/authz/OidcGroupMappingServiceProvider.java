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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.security.GroupMappingServiceProvider;
import scala.collection.immutable.Set;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static scala.collection.JavaConverters.asScalaSet;

/**
 * This class is responsible for getting the groups for a particular authenticated oidc/Oauth2 user.
 * This implementation fetches the list of the groups membership of the user which are already cached during the authentication phase.
 * The groups correspond to the roles or groups claims returned by the OIDC provider.
 */
@Slf4j
public class OidcGroupMappingServiceProvider implements GroupMappingServiceProvider {
    // The user entry is added within the same http request
    private static final Cache<String, List<String>> userGroupsCache = Caffeine.newBuilder()
            .expireAfterWrite(59, MINUTES)
            .maximumSize(1000)
            .build();

    public OidcGroupMappingServiceProvider() {
        log.info("Initializing {} ...", OidcGroupMappingServiceProvider.class);
    }

    public static void addUserAndGroups(String authenticatedUser, List<String> groups) {
        userGroupsCache.put(authenticatedUser, groups);
    }

    @Override
    public Set<String> getGroups(String authenticatedUser) {
        List<String> groups = Optional.ofNullable(userGroupsCache.getIfPresent(authenticatedUser)).orElse(emptyList());
        log.info("Authorization - The user {} is member of the groups: {}", authenticatedUser, groups);
        // scala.collection.JavaConverters is deprecated in scala 2.13
        // and replaced by scala.jdk.CollectionConverters
        return asScalaSet(new HashSet<>(groups)).toSet();
    }

}
