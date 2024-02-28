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

package io.tosit.okdp.spark.authc.utils;

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tosit.okdp.spark.authc.exception.OidcClientException;
import java.io.IOException;
import java.net.URL;

public class JsonUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

  public static <T> T loadJsonFromUrl(String url, Class<T> type) throws RuntimeException {
    try {
      return mapper.readValue(new URL(url), type);
    } catch (IOException e) {
      throw new OidcClientException(format("Unable te fetch json data from Url: %s", url), e);
    }
  }

  public static <T> T loadJsonFromString(String json, Class<T> type) throws RuntimeException {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new OidcClientException(format("Unable to load json data into the class %s", type), e);
    }
  }

  public static <T> String toJson(T type) throws RuntimeException {
    try {
      return mapper.writeValueAsString(type);
    } catch (JsonProcessingException e) {
      throw new OidcClientException(e.getMessage(), e);
    }
  }
}
