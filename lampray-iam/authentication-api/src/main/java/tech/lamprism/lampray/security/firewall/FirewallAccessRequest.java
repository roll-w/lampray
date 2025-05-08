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

import space.lingu.Nullable;
import tech.lamprism.lampray.user.UserIdentity;

/**
 * @author RollW
 */
public interface FirewallAccessRequest {
    /**
     * Get the user that made the request, if exists.
     *
     * @return The user that made the request
     */
    @Nullable
    UserIdentity getRequestUser();

    /**
     * Get the IP address of the request, if exists.
     *
     * @return The IP address of the request
     */
    @Nullable
    String getRequestIpAddress();

    /**
     * Get the request path, if exists.
     *
     * @return The request path
     */
    @Nullable
    String getRequestPath();

    /**
     * Get the request source, if exists.
     * <p>
     * For web requests, this is the user agent.
     *
     * @return The request source
     */
    @Nullable
    String getRequestSource();

    // TODO: add more fields
}
