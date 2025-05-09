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

package tech.lamprism.lampray.server.sshd;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.sshd.server.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.ServerInitializeException;
import tech.lamprism.lampray.web.common.keys.ServerConfigKeys;
import tech.lamprism.lampray.web.configuration.LocalConfigConfiguration;

import java.io.IOException;
import java.net.BindException;

/**
 * @author RollW
 */
@Service
public class SshdServerRunner {
    private static final Logger logger = LoggerFactory.getLogger(SshdServerRunner.class);

    private final SshServer sshServer;
    private final ConfigReader configReader;

    public SshdServerRunner(SshServer sshServer,
                            @Qualifier(LocalConfigConfiguration.LOCAL_CONFIG_PROVIDER)
                            ConfigReader configReader) {
        this.sshServer = sshServer;
        this.configReader = configReader;
    }

    @PostConstruct
    public void run() throws ServerInitializeException {
        if (configReader.get(ServerConfigKeys.SSH_PORT) < 0) {
            logger.debug("SSH port set to -1, skipping SSH server startup.");
            return;
        }
        try {
            sshServer.start();
        } catch (BindException e) {
            throw new ServerInitializeException(
                    new ServerInitializeException.Detail(
                            "Failed to start SSH server on port " + sshServer.getPort(),
                            "Please check if the port is occupied by another process. " +
                                    "You can change the port in the configuration file, " +
                                    "or kill the process that occupies the port."
                    ), e
            );
        } catch (IOException e) {
            throw new ServerInitializeException(
                    new ServerInitializeException.Detail(
                            "Failed to start SSH server on port " + sshServer.getPort(),
                            "Please check configuration file and try again."
                    ), e
            );
        }

        logger.info("Sshd server started on port {} at host {}.",
                sshServer.getPort(),
                sshServer.getHost()
        );
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping sshd server on port {}.", sshServer.getPort());
        try {
            sshServer.stop();
        } catch (Exception e) {
            logger.error("Failed to stop sshd server: {}.", e.getMessage());
        }
    }
}
