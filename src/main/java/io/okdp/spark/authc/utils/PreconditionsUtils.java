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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;

import com.google.common.base.Strings;
import io.okdp.spark.authc.exception.AuthenticationException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletRequest;

/** Preconditions check utility methods */
public class PreconditionsUtils {

  /** Ensures the given string is not null. */
  public static String checkNotNull(String str, String label) {
    if (Strings.nullToEmpty(str).trim().isEmpty()) {
      throw new NullPointerException(
          format("The parameter <%s> should not be null or blank", label));
    } else {
      return str;
    }
  }

  /**
   * Find the unsupported scopes by the OIDC provider
   *
   * @param supported
   * @param provided
   */
  public static void assertSupportedScopes(List<String> supported, String provided, String label) {
    List<String> unsupported =
        Arrays.stream(provided.split("\\+"))
            .filter(element -> !supported.contains(element))
            .collect(toList());
    checkArgument(
        unsupported.isEmpty(),
        format(
            "The parameter '%s' contains an unsupported scopes '%s' by your oidc provider.\n"
                + "The supported scopes are: %s",
            label, unsupported, supported));
  }

  /** Check the oidc provider response to the redirect authentication */
  public static Void checkAuthLogin(ServletRequest servletRequest) {
    Optional<String> maybeError = ofNullable(servletRequest.getParameter("error"));
    String errorDescription = servletRequest.getParameter("error_description");
    if (maybeError.isPresent()) {
      throw new AuthenticationException(
          400, format("Authentication denied: %s (%s)", maybeError.get(), errorDescription));
    }
    return null;
  }
}
