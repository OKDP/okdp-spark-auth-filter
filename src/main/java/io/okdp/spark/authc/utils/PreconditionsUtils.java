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
import static io.okdp.spark.authc.utils.HttpAuthenticationUtils.isSecure;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;

import com.google.common.base.Strings;
import io.okdp.spark.authc.exception.AuthenticationException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;

/** Preconditions check utility methods */
@Slf4j
public class PreconditionsUtils {

  /** Ensures the given string is not null. */
  public static String checkNotNull(String str, String label) {
    if (Strings.nullToEmpty(str).trim().isEmpty()) {
      throw new AuthenticationException(
          HttpStatus.SC_BAD_REQUEST,
          format("The parameter <%s> should not be null or blank", label));
    } else {
      return str;
    }
  }

  /** Ensures the provided state matches the expected one */
  public static void checkState(String provided, String expected, Object errorMessage) {
    if (!provided.contentEquals(expected)) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  /**
   * Find the unsupported scopes by the OIDC provider
   *
   * @param supported
   * @param provided
   */
  public static void warnUnsupportedScopes(List<String> supported, String provided, String label) {
    List<String> unsupported =
        Arrays.stream(provided.split("\\+"))
            .filter(element -> !supported.contains(element))
            .collect(toList());
    if (!unsupported.isEmpty()) {
      log.warn(
          "The parameter may '{}' contain an unsupported scopes '{}' by your oidc provider. The supported scopes are: {}",
          label,
          unsupported,
          supported);
    }
  }

  /** The OIDC provider should support PKCE for public clients */
  public static void assertSupportePKCE(
      List<String> pkceMethods, String usePkce, String clientSecret, String label) {
    boolean pkceSupported =
        (pkceMethods.stream().anyMatch(m -> m.equalsIgnoreCase("S256"))
                && usePkce.equalsIgnoreCase("auto")
            || usePkce.equalsIgnoreCase("true"));
    checkArgument(
        pkceSupported || clientSecret != null,
        format(
            "The client secret %s parameter is mandatory as the OIDC provider does not support PKCE (use-pkce=%s)",
            label, usePkce));
  }

  /** The provided redirectUri should be in https if the provided isCookieSecure is true */
  public static void assertCookieSecure(String redirectUri, Boolean isCookieSecure, String label) {
    checkArgument(
        isSecure(redirectUri) == isCookieSecure,
        format(
            "The redirect url '%s' should be in https as the cookie secure flag is enabled '%s=%s'",
            redirectUri, label, isCookieSecure));
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
