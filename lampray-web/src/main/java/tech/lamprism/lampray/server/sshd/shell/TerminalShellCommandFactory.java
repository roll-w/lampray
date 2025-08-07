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
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import org.springframework.shell.ShellRunner;
import tech.lamprism.lampray.system.console.shell.TerminalRegistry;
import tech.lamprism.lampray.system.console.shell.reader.LineReaderFactory;

/**
 * @author RollW
 */
public class TerminalShellCommandFactory implements ShellFactory {
    private final TerminalRegistry terminalRegistry;
    private final LineReaderFactory lineReaderFactory;
    private final ShellRunner shellRunner;

    public TerminalShellCommandFactory(TerminalRegistry terminalRegistry,
                                       LineReaderFactory lineReaderFactory,
                                       ShellRunner shellRunner) {
        this.terminalRegistry = terminalRegistry;
        this.lineReaderFactory = lineReaderFactory;
        this.shellRunner = shellRunner;
    }

    @Override
    public Command createShell(ChannelSession channel) {
        return new TerminalShellCommand(
                terminalRegistry,
                lineReaderFactory,
                shellRunner
        );
    }
}