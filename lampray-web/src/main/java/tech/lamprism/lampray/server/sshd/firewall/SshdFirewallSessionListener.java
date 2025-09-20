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

import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.security.authorization.PrivilegedUser;
import tech.lamprism.lampray.security.authorization.adapter.PrivilegedUserAuthenticationToken;
import tech.lamprism.lampray.security.firewall.FirewallException;
import tech.lamprism.lampray.security.firewall.FirewallRegistry;
import tech.lamprism.lampray.server.sshd.SshdPasswordAuthenticator;

/**
 * @author RollW
 */
@Service
public class SshdFirewallSessionListener implements SessionListener {
    private static final Logger logger = LoggerFactory.getLogger(SshdFirewallSessionListener.class);

    private final FirewallRegistry firewallRegistry;

    public SshdFirewallSessionListener(FirewallRegistry firewallRegistry) {
        this.firewallRegistry = firewallRegistry;
    }

    @Override
    public void sessionEstablished(Session session) {
        Authentication authentication = session.getAttribute(SshdPasswordAuthenticator.AUTHENTICATION_KEY);
        logger.info("Firewall session established: {}", authentication);
    }

    @Override
    public void sessionCreated(Session session) {
        // First check without user info
        SshdFirewallAccessRequest request = new SshdFirewallAccessRequest(session.getIoSession(), null);
        firewallFilter(session, request);
    }

    @Override
    public void sessionException(Session session, Throwable t) {
    }

    @Override
    public void sessionEvent(Session session, Event event) {
        if (event != Event.Authenticated) {
            return;
        }

        // Check again with user info
        Authentication authentication = session.getAttribute(SshdPasswordAuthenticator.AUTHENTICATION_KEY);
        if (!(authentication instanceof PrivilegedUserAuthenticationToken token)) {
            throw new IllegalStateException("No authentication found in session for IP " + session.getRemoteAddress());
        }
        PrivilegedUser credentials = token.getCredentials();
        SshdFirewallAccessRequest request = new SshdFirewallAccessRequest(session.getIoSession(), credentials);

        firewallFilter(session, request);
    }

    private void firewallFilter(Session session, SshdFirewallAccessRequest request) {
        try {
            firewallRegistry.filter(request);
        } catch (FirewallException e) {
            logger.info("Connection from IP {} (user: {}) denied by firewall: {}",
                    request.getRequestIpAddress(), request.getRequestUser() != null ? request.getRequestUser().getUsername() : null,
                    e.getMessage());
            try {
                session.disconnect(org.apache.sshd.common.SshConstants.SSH2_DISCONNECT_BY_APPLICATION, "Access denied");
            } catch (Exception ex) {
                logger.warn("Failed to disconnect session from IP {}: {}",
                        request.getRequestIpAddress(), ex.getMessage());
            }
            return;
        }
        logger.debug("Connection from IP {} (user: {}) allowed by firewall",
                request.getRequestIpAddress(), request.getRequestUser() != null ? request.getRequestUser().getUsername() : null);
    }
}
