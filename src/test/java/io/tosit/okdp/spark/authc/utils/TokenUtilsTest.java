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

import io.tosit.okdp.spark.authc.model.UserInfo;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TokenUtilsTest {

    @Test
    public void should_decode_access_token() {
        // Given
        String accessToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBkZWEwOTM5NDZjNDIwZjA4YTZjNTVmY2MxYTFhYTU0OWUyZGRjMjQifQ" +
                ".eyJpc3MiOiJodHRwczovL2RleC5va2RwLmxvY2FsL2RleCIsInN1YiI6IkNnUmlhV3hzRWdSc1pHRnciLCJhdWQiOiJkZXgtb2lkYyIsImV4cCI6MTcwODQ3NzE5NiwiaWF0IjoxNzA4MzkwNzk2LCJhdF9oYXNoIjoiTzZYanpxdXpsSkFVS0ZSc2kwUVFjUSIsImVtYWlsIjoiYmlsbEBleGFtcGxlLm9yZyIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJncm91cHMiOlsiYWRtaW5zIl0sIm5hbWUiOiJiaWxsIn0" +
                ".cyF8yCeTUm5c5xppD0O8gvw0nn2h5S3gdC37U7AfpEKQRaXXZ7KUD8nt9h17MoFKLenAgs5tEPm0aFfgYGIaIkm6S_u35sukVzGQg8sfXlOeuOkn9kYyGh4QUnDGT5y-SfQP6JSLckVqxin3-YtPhsp123rarmob3rPaW0yJrcy6B1kXFxxXMc1bkQNkEwRUfKhhGPPQLoP83BD3NbjyQaB13XAocrH7HcNYlBebLUDEacZIp6V4skqjjfPShg-VlHHOrJ4zW5ChFjak3vcgxg6TKoPkLahAqVpyqW2GzZSquer3jClgMKbAGNNaBYOiIRYkKL5tQNK-lNYuoWD_UQ";

        // When
        UserInfo userInfo = TokenUtils.userInfo(accessToken);

        // Then
        assertThat(userInfo.name()).isEqualTo("bill");
        assertThat(userInfo.email()).isEqualTo("bill@example.org");
        assertThat(userInfo.groups()).isEqualTo(singletonList("admins"));
        assertThat(userInfo.roles()).isEqualTo(emptyList());

    }

}
