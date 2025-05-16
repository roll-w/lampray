/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.security.firewall;

import jakarta.servlet.http.HttpServletRequest;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.user.UserIdentity;

/**
 * @author RollW
 */
public class HttpFirewallAccessRequest implements FirewallAccessRequest {
    @NonNull
    private final HttpServletRequest request;

    @Nullable
    private final UserIdentity requestedUser;

    public HttpFirewallAccessRequest(@NonNull HttpServletRequest request,
                                     @Nullable UserIdentity requestedUser) {
        this.request = request;
        this.requestedUser = requestedUser;
    }

    @Nullable
    @Override
    public UserIdentity getRequestUser() {
        return requestedUser;
    }

    @Nullable
    @Override
    public String getRequestIpAddress() {
        return request.getRemoteAddr();
    }

    @Nullable
    @Override
    public String getRequestPath() {
        return request.getRequestURI();
    }

    @Nullable
    @Override
    public String getRequestSource() {
        return request.getHeader("User-Agent");
    }
}
