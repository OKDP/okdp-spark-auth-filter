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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.security.Principal;

import static java.util.Optional.ofNullable;

/**
 * Flow the authenticated user to downstream spark UI/History filter chain
 */
public class PrincipalHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final String authenticatedUser;
    private final HttpServletRequest request;

    public PrincipalHttpServletRequestWrapper(HttpServletRequest request, String authenticatedUser) {
        super(request);
        this.authenticatedUser = authenticatedUser;
        this.request = request;
    }

    @Override
    public Principal getUserPrincipal() {
        return ofNullable(request.getUserPrincipal()).orElse(() -> authenticatedUser);
    }

    @Override
    public String getRemoteUser() {
        return ofNullable(request.getRemoteUser()).orElse(this.authenticatedUser);
    }

}
