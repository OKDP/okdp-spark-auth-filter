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

package io.okdp.spark.authc.provider.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.okdp.spark.authc.model.AccessToken;
import io.okdp.spark.authc.model.PersistedToken;
import io.okdp.spark.authc.model.UserInfo;
import io.okdp.spark.authc.provider.SessionStore;
import io.okdp.spark.authc.provider.impl.EmailIdentityProvider;
import io.okdp.spark.authc.provider.impl.store.CookieSessionStore;
import io.okdp.spark.authc.utils.TokenUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CookieFactoryStoreTest {

  private AccessToken accessToken;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    try {
      accessToken =
          new ObjectMapper()
              .readValue(
                  "{\n"
                      + "  \"access_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2MxYTFhYTU0OWUyZGRjMjQifQ"
                      + ".eyJpc3MiOiJodHRwczovL2RleC5va2RwLmxvY2FsL2RleCIsInN1YiI6IkNnTmliMklTQkd4a1lYQSIsImF1ZCI6ImRleC1vaWRjIiwiZXhwIjoxNzA4NDc2NzE5LCJpYXQiOjE3MDgzOTAzMTksImF0X2hhc2giOiJ4X2tLSHJqR2ZuU2ZrakR3SUdQUGJnIiwiZW1haWwiOiJib2JAZXhhbXBsZS5vcmciLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZ3JvdXBzIjpbInN1cGVyYWRtaW5zIl0sIm5hbWUiOiJib2IifQ"
                      + ".iQHMPZHQ3Oc-gDSISd73paT-c6B6DlsTiuc4ACbIUpb-auxZsE6k6DhnrrLn7ZzUpb1i7_rGtsXW6W6fiRdFQ9L9JFz51d9XC9eLq_kiacEu7JVDoIQYrAm0xDxxKDN1mxS0H_BOiKVZgv6tJvEniKvH94wqt0ZG3x-YQPHm3C65RQtFH3mOxDSHqQC6pN2xUsz-jGKYIIhEyq1zYruJMYEilv8WITg2oxv6D1FaJmDwnQetEyfDxPxVD_bfANfcXcAomvQg1wCCjlHuNHeEwG4HYOub4HzlqrwRopYCBQLyP61A1D-wa83bPRh5T3ZWBtU5oU3NIxCLi3V6cLigqg\",\n"
                      + "  \"token_type\": \"bearer\",\n"
                      + "  \"expires_in\": 86399,\n"
                      + "  \"refresh_token\": \"ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr\",\n"
                      + "  \"id_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2MxYTFhYTU0OWUyZGRjMjQifQ"
                      + ".eyJpc3MiOiJodHRwczovL2RleC5va2RwLmxvY2FsL2RleCIsInN1YiI6IkNnTmliMklTQkd4a1lYQSIsImF1ZCI6ImRleC1vaWRjIiwiZXhwIjoxNzA4NDc2NzE5LCJpYXQiOjE3MDgzOTAzMTksImF0X2hhc2giOiI1WWdud2ZFNmszSjIxQkFpdm1ZaEZnIiwiY19oYXNoIjoiQlhXVjhrZWR4QkhDTHBCaUY0OUMyUSIsImVtYWlsIjoiYm9iQGV4YW1wbGUub3JnIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImdyb3VwcyI6WyJzdXBlcmFkbWlucyJdLCJuYW1lIjoiYm9iIn0"
                      + ".hGlrvV_xXpc3h29S3KvYt11bftXnJ6cIu9Db_7Z6dgueVfmmBvB5Ml8inGfaUKj5KzBFvVS2YeSxLfr4yu4H0KWOKUyTIjkQqeGXh0JfOrKvIIViTxKi1U1OKnNmZxTYJCjJzjqvwZAgxlZRcEdizbH4wsNCYmQO9NUJDeULVlv0V7AkvS6jX0k2OrseOSh526l-SyhRVx8d4IXLHDRbr5ulnuR3nlhuUiILCtbpJFCHGB-XuwEkETRvL6F8nMpapG0x_Sw1XL5XZ6OQ1NNYDt11mdKDlKtf9cQi5TbIyk_OJ_Oayr4JU-o3Y3ov6tMs1R2RxIVxZnBQlqp7x5_03g\"\n"
                      + "}",
                  AccessToken.class);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void should_convert_access_token_into_persisted_token() throws IOException {
    String expected =
        "{"
            + "\"identity_provider\":{\"type\":\"email\"},"
            + "\"access_token_payload\":{"
            + "\"sub\":\"CgRiaWxsEgRsZGFw\","
            + "\"name\":\"bill\","
            + "\"email\":\"bill@example.org\","
            + "\"groups\":["
            + "\"admins\""
            + "],"
            + "\"roles\":[]"
            + "},"
            + "\"expires_in\":86399,"
            + "\"expires_at\":\"2024-02-22T10:11:11.123Z\","
            + "\"refresh_token\":"
            + "\"ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr\""
            + "}";
    // Given
    String accessToken =
        "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2MxYTFhYTU0OWUyZGRjMjQifQ"
            + ".eyJpc3MiOiJodHRwczovL2RleC5va2RwLmxvY2FsL2RleCIsInN1YiI6IkNnUmlhV3hzRWdSc1pHRnciLCJhdWQiOiJkZXgtb2lkYyIsImV4cCI6MTcwODQ3NzE5NiwiaWF0IjoxNzA4MzkwNzk2LCJhdF9oYXNoIjoiTzZYanpxdXpsSkFVS0ZSc2kwUVFjUSIsImVtYWlsIjoiYmlsbEBleGFtcGxlLm9yZyIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJncm91cHMiOlsiYWRtaW5zIl0sIm5hbWUiOiJiaWxsIn0"
            + ".cyF8yCeTUm5c5xppD0O8gvw0nn2h5S3gdC37U7AfpEKQRaXXZ7KUD8nt9h17MoFKLenAgs5tEPm0aFfgYGIaIkm6S_u35sukVzGQg8sfXlOeuOkn9kYyGh4QUnDGT5y-SfQP6JSLckVqxin3-YtPhsp123rarmob3rPaW0yJrcy6B1kXFxxXMc1bkQNkEwRUfKhhGPPQLoP83BD3NbjyQaB13XAocrH7HcNYlBebLUDEacZIp6V4skqjjfPShg-VlHHOrJ4zW5ChFjak3vcgxg6TKoPkLahAqVpyqW2GzZSquer3jClgMKbAGNNaBYOiIRYkKL5tQNK-lNYuoWD_UQ";
    String refreshToken =
        "ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr";
    int expires_in = 86399;

    // When
    UserInfo userInfo = TokenUtils.userInfo(accessToken);
    PersistedToken persistedToken =
        PersistedToken.builder()
            .userInfo(userInfo)
            .refreshToken(refreshToken)
            .expiresIn(expires_in)
            .expiresAt(Date.from(Instant.parse("2024-02-21T10:11:12.123Z").plusSeconds(expires_in)))
            .identityProvider(new EmailIdentityProvider())
            .build();

    // Then
    assertThat(persistedToken.toJson()).isEqualTo(expected);
    assertThat(persistedToken.isExpired()).isTrue();
  }

  @Test
  public void should_save_and_read_token_stored_in_cookie() {
    // Given
    String cookieName = "spark";
    String cookieDomain = "spark.okdp.local";
    SessionStore sessionStore =
        CookieSessionStore.of(
            cookieName, cookieDomain, true, "E132A72E815F496FFC49B3EC876754F4", 60);

    // When
    PersistedToken originalPersistedToken =
        PersistedToken.builder()
            .userInfo(TokenUtils.userInfo(accessToken.accessToken()))
            .refreshToken(accessToken.refreshToken())
            .expiresIn(accessToken.expiresIn())
            .expiresAt(
                Date.from(
                    Instant.parse("2024-02-21T10:11:12.123Z").plusSeconds(accessToken.expiresIn())))
            .identityProvider(new EmailIdentityProvider())
            .build();
    Cookie cookie = sessionStore.save(originalPersistedToken);
    PersistedToken persistedToken = sessionStore.readToken(cookie.getValue());

    // Then
    assertThat(cookie.getName()).isEqualTo(cookieName);
    assertThat(cookie.getDomain()).isEqualTo(cookieDomain);
    assertThat(cookie.getMaxAge()).isEqualTo(60);
    assertThat(cookie.isHttpOnly()).isEqualTo(true);
    assertThat(cookie.getSecure()).isEqualTo(true);
    assertThat(cookie.getPath()).isEqualTo("/;SameSite=Strict;");
    assertThat(persistedToken.refreshToken()).isEqualTo(accessToken.refreshToken());
    assertThat(persistedToken.expiresIn()).isEqualTo(accessToken.expiresIn());
    assertThat(persistedToken.expiresAt())
        .isAfter(
            Instant.parse("2024-02-21T10:11:12.123Z").plusSeconds(accessToken.expiresIn() - 1));
    assertThat(persistedToken.userInfo()).isEqualTo(TokenUtils.userInfo(accessToken.accessToken()));
  }
}
