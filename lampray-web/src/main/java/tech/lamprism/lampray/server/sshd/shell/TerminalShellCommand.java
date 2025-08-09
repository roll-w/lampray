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

package tech.lamprism.lampray.server.sshd.shell;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.AbstractCommandSupport;
import org.apache.sshd.server.session.ServerSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.shell.ShellRunner;
import tech.lamprism.lampray.server.sshd.SshdPasswordAuthenticator;
import tech.lamprism.lampray.system.console.shell.TerminalContext;
import tech.lamprism.lampray.system.console.shell.TerminalContextHolder;
import tech.lamprism.lampray.system.console.shell.TerminalRegistry;
import tech.lamprism.lampray.system.console.shell.reader.LineReaderFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author RollW
 */
public class TerminalShellCommand extends AbstractCommandSupport {
    private final TerminalRegistry terminalRegistry;
    private final LineReaderFactory lineReaderFactory;
    private final ShellRunner shellRunner;

    public TerminalShellCommand(
            TerminalRegistry terminalRegistry, LineReaderFactory lineReaderFactory, ShellRunner shellRunner) {
        super("", null);
        this.terminalRegistry = terminalRegistry;
        this.lineReaderFactory = lineReaderFactory;
        this.shellRunner = shellRunner;
    }

    private String hash(byte[] data) {
        return Integer.toHexString(Arrays.hashCode(data));
    }

    @Override
    public void run() {
        ServerSession serverSession = getSession();
        Authentication authentication = serverSession
                .getAttribute(SshdPasswordAuthenticator.AUTHENTICATION_KEY);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String sessionId = hash(serverSession.getSessionId());
        String terminalName = sessionId + "@sshd";
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .dumb(false)
                    .streams(getInputStream(), getOutputStream())
                    .name(terminalName)
                    .build();
            terminalRegistry.registerTerminal(terminal);
            terminal.writer().println("Welcome to Lampray Command Line Interface!\n");
            terminal.writer().println("Type 'help' for more information.");
            terminal.writer().println("Type 'exit' or 'q' to exit.");
            terminal.writer().println();
            terminal.writer().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TerminalContext shellContext = TerminalContextHolder.getContext();
        shellContext.setTerminalName(terminalName);
        shellContext.setLineReader(lineReaderFactory.newLineReader());
        executorService.addCloseFutureListener(future -> {
            terminalRegistry.unregisterTerminal(terminalName);
            shellContext.setLineReader(null);
            SecurityContextHolder.clearContext();
            TerminalContextHolder.clearContext();
        });

        try {
            shellRunner.run((String[]) null);
        } catch (Exception ignored) {
        }

        getExitCallback().onExit(0);
        log.debug("Connection closed on session: {}", serverSession.getRemoteAddress());
    }

    @Override
    public void destroy(ChannelSession channel) throws Exception {
        super.destroy(channel);
    }
}