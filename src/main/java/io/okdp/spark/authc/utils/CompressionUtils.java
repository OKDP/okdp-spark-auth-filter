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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.ByteStreams;
import io.okdp.spark.authc.config.Constants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Compression utility methods */
public class CompressionUtils implements Constants {

  /**
   * @param text the plain text message to compress
   * @return the compressed text as BASE64 string
   */
  public static String compressToString(String text) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(out);
      gzip.write(text.getBytes(UTF_8));
      gzip.close();
      return BASE64_ENCODER.encodeToString(out.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param base64Compressed the base64 compressed text
   * @return the uncompressed message as plain text
   */
  public static String decompress(String base64Compressed) {
    try {
      byte[] decoded = BASE64_DECODER.decode(base64Compressed.getBytes(UTF_8));
      ByteArrayInputStream in = new ByteArrayInputStream(decoded);
      GZIPInputStream gunzip = new GZIPInputStream(in);
      return new String(ByteStreams.toByteArray(gunzip), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
