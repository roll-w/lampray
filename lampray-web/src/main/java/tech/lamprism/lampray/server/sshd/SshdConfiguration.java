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

import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.ShellRunner;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.server.sshd.shell.TerminalShellCommandFactory;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.system.console.shell.TerminalRegistry;
import tech.lamprism.lampray.system.console.shell.reader.LineReaderFactory;
import tech.lamprism.lampray.web.common.keys.ServerConfigKeys;
import tech.lamprism.lampray.web.configuration.LocalConfigConfiguration;

import java.nio.file.Path;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.List;

/**
 * @author RollW
 */
@Configuration
public class SshdConfiguration {
    private final TerminalRegistry terminalRegistry;
    private final LineReaderFactory lineReaderFactory;
    private final ShellRunner shellRunner;

    public SshdConfiguration(TerminalRegistry terminalRegistry,
                             LineReaderFactory lineReaderFactory,
                             ShellRunner shellRunner) {
        this.terminalRegistry = terminalRegistry;
        this.lineReaderFactory = lineReaderFactory;
        this.shellRunner = shellRunner;
    }

    @Bean
    public SshServer sshdServer(
            PasswordAuthenticator sshdPasswordAuthenticator,
            List<SessionListener> sessionListeners,
            @Qualifier(LocalConfigConfiguration.LOCAL_CONFIG_PROVIDER)
            ConfigReader configReader
    ) {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(configReader.get(ServerConfigKeys.SSH_PORT));
        sshd.setHost(configReader.get(ServerConfigKeys.SSH_HOST));
        SimpleGeneratorHostKeyProvider generatorHostKeyProvider = new SimpleGeneratorHostKeyProvider(
                Path.of(configReader.get(ServerConfigKeys.SSH_HOST_KEY))
        );
        generatorHostKeyProvider.setAlgorithm("RSA");
        generatorHostKeyProvider.setKeySize(4096);
        generatorHostKeyProvider.setKeySpec(new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4));
        sshd.setKeyPairProvider(generatorHostKeyProvider);
        for (SessionListener listener : sessionListeners) {
            sshd.addSessionListener(listener);
        }
        sshd.setPasswordAuthenticator(sshdPasswordAuthenticator);
        sshd.setKeyboardInteractiveAuthenticator(null);
        sshd.setShellFactory(new TerminalShellCommandFactory(terminalRegistry, lineReaderFactory, shellRunner));

        CoreModuleProperties.SERVER_IDENTIFICATION.set(sshd, "LAMPRAY-" + Version.VERSION);
        CoreModuleProperties.CLIENT_IDENTIFICATION.set(sshd, "LAMPRAY-" + Version.VERSION);

        return sshd;
    }
}