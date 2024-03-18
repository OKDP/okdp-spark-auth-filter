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

package io.okdp.spark.authc.config;

import java.util.Base64;

/** This interface contains the authentication filter configuration parameters */
public interface Constants {

  /** OIDC provider issuer url */
  String AUTH_ISSUER_URI = "issuer-uri";

  /** OIDC provider client id */
  String AUTH_CLIENT_ID = "client-id";

  /** OIDC provider client secret */
  String AUTH_CLIENT_SECRET = "client-secret";

  /** OIDC provider redirect url where the oidc provider sends back the user once authenticated */
  String AUTH_REDIRECT_URI = "redirect-uri";

  /** The oidc scope (ex.: openid+profile+email+groups+offline_access) */
  String AUTH_SCOPE = "scope";

  /** OIDC standard well-known configuration endpoint */
  String AUTH_ISSUER_WELL_KNOWN_CONFIGURATION = "/.well-known/openid-configuration";

  /** The time to wait to receive a response after sending a request to the oidc provider */
  int OIDC_REQUEST_TIMEOUT_SECONDS = 30;

  /** The http cookie name where the access token is saved */
  String AUTH_COOKE_NAME = "OKDP_AUTH_SPARK_UI";

  /** The http auth state cookie name which holds the auth state and PKCE data */
  String AUTH_STATE_COOKE_NAME = AUTH_COOKE_NAME + "_STATE";

  /** Transmit the cookie over HTTPS only */
  String AUTH_COOKE_IS_SECURE = "cookie-is-secure";

  /** The default secure cookie flag */
  String AUTH_COOKE_DEFAULT_IS_SECURE = "true";

  /** The cookie expiration period parameter name in minutes */
  String AUTH_COOKE_MAX_AGE_MINUTES = "cookie-max-age-minutes";

  /** The default cookie expiration period minutes */
  int AUTH_COOKE_DEFAULT_MAX_AGE_MINUTES = 12 * 60;

  /** Use PKCE (true|false|auto) */
  String AUTH_USE_PKCE = "use-pkce";

  /** The cookie encryption key parameter name */
  String AUTH_COOKIE_ENCRYPTION_KEY = "cookie-cipher-secret-key";

  /** The content encryption algorithm */
  String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

  /** The secret key encryption algorithm */
  String AES_ENCRYPTION_ALGORITHEM = "AES";

  /** BASE64 encoder */
  Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

  /** BASE64 decoder */
  Base64.Decoder BASE64_DECODER = Base64.getDecoder();
}
