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

import io.okdp.spark.authc.exception.OidcClientException;
import java.lang.reflect.InvocationTargetException;

public class IdentityProviderFactory {
  public static IdentityProvider from(String provider) throws OidcClientException {
    try {
      return (IdentityProvider)
          Class.forName("io.okdp.spark.authc.provider.impl." + provider + "IdentityProvider")
              .getDeclaredConstructor()
              .newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException e) {
      throw new OidcClientException("ID provider not found", e);
    }
  }
}
