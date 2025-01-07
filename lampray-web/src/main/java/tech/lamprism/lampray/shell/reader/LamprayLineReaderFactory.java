/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.shell.reader;

import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.command.CommandCatalog;

import java.util.regex.Pattern;

/**
 * @author RollW
 */
public class LamprayLineReaderFactory implements LineReaderFactory {
    private final Terminal terminal;
    private final Completer completer;
    private final Parser parser;
    private final CommandCatalog commandRegistry;

    public LamprayLineReaderFactory(Terminal terminal, Completer completer,
                                    Parser parser, CommandCatalog commandRegistry) {
        this.terminal = terminal;
        this.completer = completer;
        this.parser = parser;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public LineReader newLineReader() {
        LineReaderBuilder lineReaderBuilder = newLineReaderBuilder();
        LineReader lineReader = lineReaderBuilder.build();
        lineReader.unsetOpt(LineReader.Option.INSERT_TAB);
        return lineReader;
    }

    private LineReaderBuilder newLineReaderBuilder() {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .appName("Lampray")
                .completer(completer)
                .highlighter(new Highlighter() {
                    @Override
                    public AttributedString highlight(LineReader reader, String buffer) {
                        int l = 0;
                        String best = null;
                        for (String command : commandRegistry.getRegistrations().keySet()) {
                            if (buffer.startsWith(command) && command.length() > l) {
                                l = command.length();
                                best = command;
                            }
                        }
                        if (best != null) {
                            return new AttributedStringBuilder(buffer.length()).append(best, AttributedStyle.DEFAULT).append(buffer.substring(l)).toAttributedString();
                        } else {
                            return new AttributedString(buffer, AttributedStyle.DEFAULT);
                        }
                    }

                    @Override
                    public void setErrorPattern(Pattern errorPattern) {
                    }

                    @Override
                    public void setErrorIndex(int errorIndex) {
                    }
                })
                .parser(parser);
    }
}
