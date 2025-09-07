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

package tech.lamprism.lampray.server.sshd.firewall;

import org.apache.sshd.common.io.IoSession;
import space.lingu.Nullable;
import tech.lamprism.lampray.security.firewall.FirewallAccessRequest;
import tech.lamprism.lampray.user.UserIdentity;

import java.net.SocketAddress;

/**
 * SSHD firewall access request implementation.
 *
 * @author RollW
 */
public class SshdFirewallAccessRequest implements FirewallAccessRequest {
    private final IoSession session;
    private final UserIdentity user;

    public SshdFirewallAccessRequest(IoSession session, @Nullable UserIdentity user) {
        this.session = session;
        this.user = user;
    }

    @Override
    @Nullable
    public UserIdentity getRequestUser() {
        return user;
    }

    @Override
    @Nullable
    public String getRequestIpAddress() {
        SocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress == null) {
            return null;
        }
        return remoteAddress.toString();
    }

    @Override
    @Nullable
    public String getRequestPath() {
        return "ssh";
    }

    @Override
    @Nullable
    public String getRequestSource() {
        return "SSHD";
    }

    public IoSession getSession() {
        return session;
    }
}
