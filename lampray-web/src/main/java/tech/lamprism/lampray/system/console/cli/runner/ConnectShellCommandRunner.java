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

package tech.lamprism.lampray.system.console.cli.runner;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.common.util.io.input.NoCloseInputStream;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.apache.sshd.core.CoreModuleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.system.console.CommandSpecification;
import tech.lamprism.lampray.system.console.SimpleCommandOption;
import tech.lamprism.lampray.system.console.SimpleCommandSpecification;
import tech.lamprism.lampray.system.console.cli.CommandLineRuntimeException;
import tech.lamprism.lampray.system.console.cli.CommandRunContext;
import tech.lamprism.lampray.system.console.cli.CommandRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author RollW
 */
public class ConnectShellCommandRunner implements CommandRunner {
private static final Logger logger = LoggerFactory.getLogger(ConnectShellCommandRunner.class);

    public ConnectShellCommandRunner() {
    }

    @Override
    public int runCommand(CommandRunContext context) {
        String username = (String) context.getArguments().get("--username");
        String password = (String) context.getArguments().get("--password");
        String host = (String) context.getArguments().get("--target");
        String port = (String) context.getArguments().get("--port");

        try (SshClient sshClient = SshClient.setUpDefaultClient()) {
            CoreModuleProperties.CLIENT_IDENTIFICATION.set(sshClient, "LAMPRAY-" + Version.VERSION);
            sshClient.addSessionListener(new SessionListener() {
                @Override
                public void sessionPeerIdentificationReceived(Session session, String version, List<String> extraLines) {
                    if (!version.startsWith("SSH-2.0-LAMPRAY-")) {
                        throw new CommandLineRuntimeException("Unsupported Lampray server version, please use a compatible version of Lampray server. ");
                    }
                }
            });
            sshClient.start();
            ClientSession session = sshClient.connect(username, host, Integer.parseInt(port))
                    .verify()
                    .getSession();
            session.addPasswordIdentity(password);
            session.auth().verify(5000L);

            ChannelShell channel = session.createShellChannel();

            channel.setPtyType("xterm-256color");
            channel.setPtyColumns(80);
            channel.setPtyLines(80);
            channel.setPtyWidth(640);
            channel.setPtyHeight(480);

            Map<PtyMode, Integer> ptyModes = new HashMap<>();
            ptyModes.put(PtyMode.ECHO, PtyMode.TRUE_SETTING);
            ptyModes.put(PtyMode.ICANON, PtyMode.TRUE_SETTING);
            ptyModes.put(PtyMode.ISIG, PtyMode.TRUE_SETTING);
            ptyModes.put(PtyMode.IEXTEN, PtyMode.TRUE_SETTING);
            ptyModes.put(PtyMode.ECHOCTL, PtyMode.TRUE_SETTING);
            ptyModes.put(PtyMode.ECHOE, PtyMode.TRUE_SETTING);
            ptyModes.put(PtyMode.ECHOK, PtyMode.TRUE_SETTING);
            channel.setPtyModes(ptyModes);

            channel.open().verify(10, TimeUnit.SECONDS);

            final InputStream originalIn = System.in;
            final PrintStream originalOut = System.out;
            final PrintStream originalErr = System.err;

            OutputStream channelIn = new NoCloseOutputStream(channel.getInvertedIn());
            InputStream channelOut = new NoCloseInputStream(channel.getInvertedOut());
            InputStream channelErr = new NoCloseInputStream(channel.getInvertedErr());

            Thread outputThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = channelOut.read(buffer)) > 0) {
                        originalOut.write(buffer, 0, read);
                        originalOut.flush();
                    }
                } catch (IOException e) {
                    if (!channel.isClosed()) {
                        e.printStackTrace(originalErr);
                    }
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();

            Thread errorThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = channelErr.read(buffer)) > 0) {
                        originalErr.write(buffer, 0, read);
                        originalErr.flush();
                    }
                } catch (IOException e) {
                    if (!channel.isClosed()) {
                        e.printStackTrace(originalErr);
                    }
                }
            });
            errorThread.setDaemon(true);
            errorThread.start();
            byte[] buffer = new byte[1024];
            int read;
            while (!channel.isClosed() && (read = originalIn.read(buffer)) > 0) {
                channelIn.write(buffer, 0, read);
                channelIn.flush();
            }

            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
        } catch (IOException e) {
            throw new CommandLineRuntimeException(e);
        }

        return 0;
    }

    @Override
    public CommandSpecification getCommandSpecification() {
        return SimpleCommandSpecification.builder()
                .setNames("connect")
                .addOption(SimpleCommandOption.builder()
                        .setNames("--target", "-t")
                        .setDescription("The target host address to connect to")
                        .setRequired(true)
                        .setType(String.class)
                        .setLabel("TARGET")
                        .build())
                .addOption(SimpleCommandOption.builder()
                        .setNames("--port", "-p")
                        .setDescription("The port to connect to")
                        .setRequired(false)
                        .setDefaultValue("8080")
                        .setType(String.class)
                        .setLabel("PORT")
                        .build())
                .addOption(SimpleCommandOption.builder()
                        .setNames("--username", "-u")
                        .setDescription("The username for authentication")
                        .setRequired(false)
                        .setType(String.class)
                        .setLabel("USERNAME")
                        .build())
                .addOption(SimpleCommandOption.builder()
                        .setNames("--password", "-P")
                        .setDescription("The password for authentication")
                        .setRequired(false)
                        .setType(String.class)
                        .setLabel("PASSWORD")
                        .build())
                .setHeader("Connect to a remote shell")
                .setDescription("This command allows you to connect to a remote Lampray shell using the specified target address and port. " +
                        "You can also provide a username and password for authentication if required")
                .build();
    }
}
