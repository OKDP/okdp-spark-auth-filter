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

package io.okdp.spark.authc.provider.impl;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import com.google.common.annotations.VisibleForTesting;
import io.okdp.spark.authc.config.Constants;
import io.okdp.spark.authc.config.HttpSecurityConfig;
import io.okdp.spark.authc.exception.AuthenticationException;
import io.okdp.spark.authc.provider.AuthProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;

@AllArgsConstructor
public abstract class AbstractAuthorizationCodeAuthProvider implements Constants, AuthProvider {

  @NonNull private HttpSecurityConfig httpSecurityConfig;

  @VisibleForTesting
  public String doExecute(Request request) throws AuthenticationException {
    try {
      return request
          .execute()
          .handleResponse(
              response -> {
                final int status = response.getCode();
                final Optional<HttpEntity> maybeEntity = ofNullable(response.getEntity());
                String content;
                try (HttpEntity entity =
                    maybeEntity.orElseThrow(
                        () ->
                            new ClientProtocolException(
                                format(
                                    "%s %s - The response does not contain content",
                                    status, response.getReasonPhrase())))) {
                  content = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                }
                if (status != HttpStatus.SC_OK) {
                  throw new AuthenticationException(
                      status,
                      format(
                          "%s %s - Unable to retrieve an access token (%s)",
                          status, response.getReasonPhrase(), content));
                }
                return content;
              });
    } catch (IOException e) {
      throw new AuthenticationException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isAuthorized(ServletRequest servletRequest) {
    return httpSecurityConfig.patterns().stream()
        .anyMatch(p -> p.matcher(((HttpServletRequest) servletRequest).getRequestURI()).matches());
  }
}
