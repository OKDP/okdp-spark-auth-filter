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
package io.tosit.okdp.spark.authc.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tosit.okdp.spark.authc.common.CommonTest;
import io.tosit.okdp.spark.authc.config.Constants;
import io.tosit.okdp.spark.authc.config.HttpSecurityConfig;
import io.tosit.okdp.spark.authc.config.OidcConfig;
import io.tosit.okdp.spark.authc.model.AccessToken;
import io.tosit.okdp.spark.authc.model.WellKnownConfiguration;
import io.tosit.okdp.spark.authc.utils.JsonUtils;
import org.apache.hc.client5.http.fluent.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.tosit.okdp.spark.authc.utils.JsonUtils.loadJsonFromUrl;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class OidcAuthProviderTest implements Constants, CommonTest {

    private final String clientId = "dex-oidc";
    private final String redirectUri = "https://spark.okdp.local/home";

    @InjectMocks
    private AuthProvider authProvider;
    private String accessTokenResponse;

    @BeforeEach
    public void setUp() throws IOException {
        String issuerUri = "https://dex.okdp.local/dex";
        String clientSecret = "Not@SecurePassw0rd";
        String scope = "openid+profile+email+groups+offline_access";
        accessTokenResponse = "{\n" +
                "  \"access_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2MxYTFhYTU0OWUyZGRjMjQifQ" +
                ".eyJpc3MiOiJodHRwczovL2RleC5va2RwLmxvY2FsL2RleCIsInN1YiI6IkNnTmliMklTQkd4a1lYQSIsImF1ZCI6ImRleC1vaWRjIiwiZXhwIjoxNzA4NDc2NzE5LCJpYXQiOjE3MDgzOTAzMTksImF0X2hhc2giOiJ4X2tLSHJqR2ZuU2ZrakR3SUdQUGJnIiwiZW1haWwiOiJib2JAZXhhbXBsZS5vcmciLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZ3JvdXBzIjpbInN1cGVyYWRtaW5zIl0sIm5hbWUiOiJib2IifQ" +
                ".iQHMPZHQ3Oc-gDSISd73paT-c6B6DlsTiuc4ACbIUpb-auxZsE6k6DhnrrLn7ZzUpb1i7_rGtsXW6W6fiRdFQ9L9JFz51d9XC9eLq_kiacEu7JVDoIQYrAm0xDxxKDN1mxS0H_BOiKVZgv6tJvEniKvH94wqt0ZG3x-YQPHm3C65RQtFH3mOxDSHqQC6pN2xUsz-jGKYIIhEyq1zYruJMYEilv8WITg2oxv6D1FaJmDwnQetEyfDxPxVD_bfANfcXcAomvQg1wCCjlHuNHeEwG4HYOub4HzlqrwRopYCBQLyP61A1D-wa83bPRh5T3ZWBtU5oU3NIxCLi3V6cLigqg\",\n" +
                "  \"token_type\": \"bearer\",\n" +
                "  \"expires_in\": 86399,\n" +
                "  \"refresh_token\": \"ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr\",\n" +
                "  \"id_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2MxYTFhYTU0OWUyZGRjMjQifQ" +
                ".eyJpc3MiOiJodHRwczovL2RleC5va2RwLmxvY2FsL2RleCIsInN1YiI6IkNnTmliMklTQkd4a1lYQSIsImF1ZCI6ImRleC1vaWRjIiwiZXhwIjoxNzA4NDc2NzE5LCJpYXQiOjE3MDgzOTAzMTksImF0X2hhc2giOiI1WWdud2ZFNmszSjIxQkFpdm1ZaEZnIiwiY19oYXNoIjoiQlhXVjhrZWR4QkhDTHBCaUY0OUMyUSIsImVtYWlsIjoiYm9iQGV4YW1wbGUub3JnIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImdyb3VwcyI6WyJzdXBlcmFkbWlucyJdLCJuYW1lIjoiYm9iIn0" +
                ".hGlrvV_xXpc3h29S3KvYt11bftXnJ6cIu9Db_7Z6dgueVfmmBvB5Ml8inGfaUKj5KzBFvVS2YeSxLfr4yu4H0KWOKUyTIjkQqeGXh0JfOrKvIIViTxKi1U1OKnNmZxTYJCjJzjqvwZAgxlZRcEdizbH4wsNCYmQO9NUJDeULVlv0V7AkvS6jX0k2OrseOSh526l-SyhRVx8d4IXLHDRbr5ulnuR3nlhuUiILCtbpJFCHGB-XuwEkETRvL6F8nMpapG0x_Sw1XL5XZ6OQ1NNYDt11mdKDlKtf9cQi5TbIyk_OJ_Oayr4JU-o3Y3ov6tMs1R2RxIVxZnBQlqp7x5_03g\"\n" +
                "}";

        WellKnownConfiguration wlc = JsonUtils.loadJsonFromString(TEST_DEX_WELL_KNOWN_CONFIGURATION, WellKnownConfiguration.class);
        try (MockedStatic<JsonUtils> jsonUtils = mockStatic(JsonUtils.class)) {

            jsonUtils.when(() -> JsonUtils.loadJsonFromUrl(format("%s%s", issuerUri, AUTH_ISSUER_WELL_KNOWN_CONFIGURATION),
                            WellKnownConfiguration.class))
                    .thenReturn(wlc);

            OidcConfig oidcConfig = OidcConfig
                    .builder()
                    .issuerUri(issuerUri)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .redirectUri(redirectUri)
                    .responseType("code")
                    .scope(scope)
                    .wellKnownConfiguration(loadJsonFromUrl(format("%s%s", issuerUri, AUTH_ISSUER_WELL_KNOWN_CONFIGURATION), WellKnownConfiguration.class))
                    .build();

            authProvider = HttpSecurityConfig
                    .create(oidcConfig)
                    .authorizeRequests(".*/\\.js", ".*/\\.png")
                    .configure();
        }
    }

    @Test
    public void should_redirect_to_login_page_on_first_login() throws IOException {
        // Given
        HttpServletResponse response = mock(HttpServletResponse.class);

        // When
        authProvider.redirectUserToAuthorizationEndpoint(response);

        // Then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(captor.capture());
        assertEquals(format("https://dex.okdp.local/dex/auth?client_id=%s" +
                        "&redirect_uri=%s" +
                        "&response_type=code&scope=openid+profile+email+groups+offline_access", clientId, redirectUri),
                captor.getValue());
    }

    @Test
    void should_get_access_token_from_authz_code() throws IOException {
        // Given
        OidcAuthProvider oidcAuthProvider = spy((OidcAuthProvider) authProvider);
        doReturn(accessTokenResponse).when(oidcAuthProvider).doExecute(any(Request.class));
        String authzCode = "kpxblxm2si3x6ofxufgo54h4j";

        // When
        AccessToken accessToken = oidcAuthProvider.requestAccessToken(authzCode);

        // Then
        assertThat(accessToken).isEqualTo(new ObjectMapper().readValue(accessTokenResponse, AccessToken.class));

    }

    @Test
    void should_get_access_token_from_refresh_token() throws IOException {
        // Given
        OidcAuthProvider oidcAuthProvider = spy((OidcAuthProvider) authProvider);
        doReturn(accessTokenResponse).when(oidcAuthProvider).doExecute(any(Request.class));
        String refreshToken = "ChlvaWJmNXBuaG1rdWN0enppaGltaWp1MnJkEhlndmdzZ2tmcnVhd2x6cGV1a2ZnajNqdjJr";

        // When
        AccessToken accessToken = oidcAuthProvider.refreshToken(refreshToken);

        // Then
        assertThat(accessToken).isEqualTo(new ObjectMapper().readValue(accessTokenResponse, AccessToken.class));
    }

    @Test
    void should_bypass_authentication_for_authorized_uri() throws IOException {
        // Given
        HttpSecurityConfig httpSecurityConfig = mock(HttpSecurityConfig.class);
        when(httpSecurityConfig.patterns()).thenReturn(Stream.of("/path/to/css/style.css",
                        ".*/.*\\.js", "/path/to/image/spark-logo.png", "/path/to/authorized/.*")
                .map(Pattern::compile).collect(toList()));

        when(httpSecurityConfig.configure()).thenReturn(new OidcAuthProvider(httpSecurityConfig));
        AuthProvider auth = httpSecurityConfig.configure();

        HttpServletRequest request1 = mock(HttpServletRequest.class);
        when(request1.getRequestURI()).thenReturn("/path/to/css/style.css");

        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request2.getRequestURI()).thenReturn("/path/to/js/script.js");

        HttpServletRequest request3 = mock(HttpServletRequest.class);
        when(request3.getRequestURI()).thenReturn("/path/to/authorized/any/logo.png");

        HttpServletRequest request4 = mock(HttpServletRequest.class);
        when(request4.getRequestURI()).thenReturn("/home");

        // When
        boolean isAuthorized1 = auth.isAuthorized(request1);
        boolean isAuthorized2 = auth.isAuthorized(request2);
        boolean isAuthorized3 = auth.isAuthorized(request3);
        boolean isAuthorized4 = auth.isAuthorized(request4);

        // Then
        assertThat(isAuthorized1).isTrue();
        assertThat(isAuthorized2).isTrue();
        assertThat(isAuthorized3).isTrue();
        assertThat(isAuthorized4).isFalse();
    }

}
