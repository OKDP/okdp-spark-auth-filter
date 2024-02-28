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
package io.tosit.okdp.spark.authc;

import io.tosit.okdp.spark.authc.common.CommonTest;
import io.tosit.okdp.spark.authc.config.Constants;
import io.tosit.okdp.spark.authc.model.PersistedToken;
import io.tosit.okdp.spark.authc.model.WellKnownConfiguration;
import io.tosit.okdp.spark.authc.provider.OidcAuthProvider;
import io.tosit.okdp.spark.authc.utils.JsonUtils;
import io.tosit.okdp.spark.authz.OidcGroupMappingServiceProvider;
import org.apache.hc.client5.http.fluent.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static scala.collection.JavaConverters.asScalaSet;

@Suite
@SuiteDisplayName("Autc/Autz code flow test Suite")
public class OidcAuthFilterTest implements Constants, CommonTest {

    private final String issuerUri = "https://dex.okdp.local/dex";
    private final String clientId = "dex-oidc";
    private final String clientSecret = "Not@SecurePassw0rd";
    private final String redirectUri = "https://spark.okdp.local/home";
    private final String scope = "openid+profile+email+groups+offline_access";
    private final String cookieEncryptionKey = "E132A72E815F496FFC49B3EC876754F4";
    @InjectMocks
    private final OidcAuthFilter oidcAuthFilter = new OidcAuthFilter();
    private final String accessTokenResponse = "{\n" +
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
    private OidcAuthProvider oidcAuthProvider;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Filter initialization
        WellKnownConfiguration wlc = JsonUtils.loadJsonFromString(TEST_DEX_WELL_KNOWN_CONFIGURATION, WellKnownConfiguration.class);
        FilterConfig filterConfig = mock(FilterConfig.class);
        try (MockedStatic<JsonUtils> jsonUtils = mockStatic(JsonUtils.class)) {
            jsonUtils.when(() -> JsonUtils.loadJsonFromUrl(format("%s%s", issuerUri, AUTH_ISSUER_WELL_KNOWN_CONFIGURATION),
                            WellKnownConfiguration.class))
                    .thenReturn(wlc);
            when(filterConfig.getInitParameter(AUTH_ISSUER_URI)).thenReturn(issuerUri);
            when(filterConfig.getInitParameter(AUTH_REDIRECT_URI)).thenReturn(redirectUri);
            when(filterConfig.getInitParameter(AUTH_CLIENT_ID)).thenReturn(clientId);
            when(filterConfig.getInitParameter(AUTH_CLIENT_SECRET)).thenReturn(clientSecret);
            when(filterConfig.getInitParameter(AUTH_SCOPE)).thenReturn(scope);
            when(filterConfig.getInitParameter(AUTH_COOKIE_ENCRYPTION_KEY)).thenReturn(cookieEncryptionKey);
            oidcAuthFilter.init(filterConfig);
        }

        Field field = oidcAuthFilter.getClass().getDeclaredField("authProvider");
        field.setAccessible(true);
        oidcAuthProvider = spy((OidcAuthProvider) field.get(oidcAuthFilter));
        field.set(oidcAuthFilter, oidcAuthProvider);
    }

    @Test
    void should_skip_authentication_for_static_content() throws IOException, ServletException {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/path/to/css/style.css");

        // When
        oidcAuthFilter.doFilter(request, response, chain);

        // Then - Run the next filter chain
        verify(chain).doFilter(request, response);

    }

    @Test
    void should_run_authentication_flow_authc__on_first_login() throws IOException, ServletException {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/home");

        // When
        oidcAuthFilter.doFilter(request, response, chain);

        // Then - Redirect the user to login and callback with the authz code
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(chain, never()).doFilter(request, response);
        verify(response).sendRedirect(captor.capture());
        assertEquals(format("https://dex.okdp.local/dex/auth?client_id=%s" +
                        "&redirect_uri=%s" +
                        "&response_type=code&scope=openid+profile+email+groups+offline_access", clientId, redirectUri),
                captor.getValue());

    }

    @Test
    void should_run_authentication_flow_authz__and_save_access_token_in_cookie() throws IOException, ServletException {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/home");
        StringWriter out = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(out));
        // Set the authZ code request parameter
        when(request.getParameter(any(String.class))).thenReturn("kpxblxm2si3x6ofxufgo54h4j");
        // Return the access token from code
        doReturn(accessTokenResponse).when(oidcAuthProvider).doExecute(any(Request.class));

        // When
        oidcAuthFilter.doFilter(request, response, chain);

        // Then
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        PersistedToken persistedToken = oidcAuthProvider.httpSecurityConfig().tokenStore().readToken(captor.getValue().getValue());
        assertNotNull(persistedToken);

        // Then
        assertThat(out.toString()).isEqualTo("<script type=\"text/javascript\">window.location.href = '/home'</script>");

    }

    @Test
    void should_run_authentication_flow_authz__map_membership_groups_for_user() throws IOException, ServletException {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/home");
        StringWriter out = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(out));
        // Set the authZ code request parameter
        when(request.getParameter(any(String.class))).thenReturn("kpxblxm2si3x6ofxufgo54h4j");
        // Return the access token from code
        doReturn(accessTokenResponse).when(oidcAuthProvider).doExecute(any(Request.class));
        // Authorization provider
        OidcGroupMappingServiceProvider groupMappingServiceProvider = new OidcGroupMappingServiceProvider();

        // When
        oidcAuthFilter.doFilter(request, response, chain);

        // Then
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        PersistedToken persistedToken = oidcAuthProvider.httpSecurityConfig().tokenStore().readToken(captor.getValue().getValue());
        assertNotNull(persistedToken);

        // Then
        assertThat(groupMappingServiceProvider.getGroups("bob@example.org"))
                .isEqualTo(asScalaSet(new HashSet<>(List.of("superadmins"))).toSet());

    }


}
