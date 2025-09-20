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

package tech.lamprism.lampray.security.authentication.registration.service;

import space.lingu.Nullable;
import tech.lamprism.lampray.security.firewall.FirewallAccessRequest;
import tech.lamprism.lampray.user.UserIdentity;

/**
 * A firewall access request for user login.
 *
 * @author RollW
 */
public class UserLoginFirewallAccessRequest implements FirewallAccessRequest {

    private final UserIdentity user;

    public UserLoginFirewallAccessRequest(UserIdentity user) {
        this.user = user;
    }

    @Nullable
    @Override
    public UserIdentity getRequestUser() {
        return user;
    }

    @Nullable
    @Override
    public String getRequestIpAddress() {
        return null;
    }

    @Nullable
    @Override
    public String getRequestPath() {
        return null;
    }

    @Nullable
    @Override
    public String getRequestSource() {
        return null;
    }
}
