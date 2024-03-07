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

import io.okdp.spark.authc.config.HttpSecurityConfig;
import io.okdp.spark.authc.exception.AuthenticationException;
import io.okdp.spark.authc.model.AccessToken;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/** Each concrete authentication provider should implement this interface */
public interface AuthProvider {
  /**
   * Redirect the user to the OIDC provider in order to get an authorization code
   *
   * @param servletResponse the {@link ServletResponse}
   * @throws AuthenticationException
   */
  void redirectUserToAuthorizationEndpoint(ServletResponse servletResponse)
      throws AuthenticationException;

  /**
   * Retrieve an access token for the authenticated user using his authorization code
   *
   * @param code the authorization code obtained on the first time user login
   * @return the access token ({@link AccessToken}) from the OIDC provider
   * @throws AuthenticationException
   */
  AccessToken requestAccessToken(String code) throws AuthenticationException;

  /**
   * Retrieve an access token for the authenticated user using his refresh token
   *
   * @param refreshToken the refresh token obtained by the user on the first successful
   *     authentication
   * @return the access token ({@link AccessToken}) from the OIDC provider
   * @throws AuthenticationException
   */
  AccessToken refreshToken(String refreshToken) throws AuthenticationException;

  /**
   * Check if the request URI is authorized without authentication
   *
   * @param servletRequest the {@link ServletRequest}
   * @return true if authorised, false otherwise
   */
  boolean isAuthorized(ServletRequest servletRequest);

  /**
   * Return the oidc provider http security configuration
   *
   * @return {@link HttpSecurityConfig}
   */
  HttpSecurityConfig httpSecurityConfig();
}
