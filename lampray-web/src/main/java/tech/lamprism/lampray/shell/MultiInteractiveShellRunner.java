/*
 * Copyright 2017-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.shell;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedString;
import org.springframework.core.annotation.Order;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.ShellRunner;
import org.springframework.shell.Utils;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.jline.NonInteractiveShellRunner;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.jline.ScriptShellRunner;

import java.util.List;

/**
 * A {@link ShellRunner} that bootstraps the shell in interactive mode.
 *
 * <p>Has lower precedence than {@link ScriptShellRunner} and {@link NonInteractiveShellRunner} which makes it the
 * default shell runner when the other runners opt-out of handling the shell.
 *
 * @author Eric Bottard
 * @author Janne Valkealahti
 * @author Chris Bono
 * @author RollW
 */
@Order(MultiInteractiveShellRunner.PRECEDENCE)
public class MultiInteractiveShellRunner implements ShellRunner {

    /**
     * The precedence at which this runner is ordered by the DefaultApplicationRunner - which also controls
     * the order it is consulted on the ability to handle the current shell.
     */
    public static final int PRECEDENCE = 0;

    private final LineReader lineReader;

    private final PromptProvider promptProvider;

    private final Shell shell;

    private final ShellContext shellContext;

    public MultiInteractiveShellRunner(LineReader lineReader, PromptProvider promptProvider,
                                       Shell shell, ShellContext shellContext) {
        this.lineReader = lineReader;
        this.promptProvider = promptProvider;
        this.shell = shell;
        this.shellContext = shellContext;
    }

    @Override
    public boolean run(String[] args) throws Exception {
        shellContext.setInteractionMode(InteractionMode.INTERACTIVE);
        TerminalContext context = TerminalContextHolder.getContext();
        LineReader cur = context.getLineReader();
        if (cur == null) {
            cur = lineReader;
        }
        InputProvider inputProvider = new JLineInputProvider(cur, promptProvider);
        shell.run(inputProvider);
        return true;
    }

    public static class JLineInputProvider implements InputProvider {
        private final LineReader lineReader;
        private final PromptProvider promptProvider;

        public JLineInputProvider(LineReader lineReader, PromptProvider promptProvider) {
            this.lineReader = lineReader;
            this.promptProvider = promptProvider;
        }

        @Override
        public Input readInput() {
            try {
                AttributedString prompt = promptProvider.getPrompt();
                lineReader.readLine(prompt.toAnsi(lineReader.getTerminal()));
            }
            catch (UserInterruptException e) {
                if (e.getPartialLine().isEmpty()) {
                    throw new ExitRequest(1);
                } else {
                    return Input.EMPTY;
                }
            }
            catch (EndOfFileException e) {
                throw new ExitRequest(1);
            }
            return new ParsedLineInput(lineReader.getParsedLine());
        }
    }

    private static class ParsedLineInput implements Input {

        private final ParsedLine parsedLine;

        ParsedLineInput(ParsedLine parsedLine) {
            this.parsedLine = parsedLine;
        }

        @Override
        public String rawText() {
            return parsedLine.line();
        }

        @Override
        public List<String> words() {
            return Utils.sanitizeInput(parsedLine.words());
        }
    }

}