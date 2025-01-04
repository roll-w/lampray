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

package tech.lamprism.lampray.shell;

import org.jline.reader.LineReader;
import org.springframework.shell.context.ShellContext;

/**
 * Used to isolate contexts between different terminals.
 *
 * @author RollW
 */
public interface TerminalContext extends ShellContext {
    LineReader getLineReader();

    void setLineReader(LineReader lineReader);

    void setHasPty(boolean hasPty);

    /**
     * Get the terminal name of the current terminal.
     *
     * @return the terminal name
     */
    String getTerminalName();

    /**
     * Set the terminal name of the current terminal.
     *
     * @param terminalName the terminal name
     */
    void setTerminalName(String terminalName);

    /**
     * Get the terminal registry related to the current terminal.
     *
     * @return the terminal registry
     */
    TerminalRegistry getTerminalRegistry();

    /**
     * Check if the current thread is the main loop of the shell.
     *
     * @return true if the current thread is the main loop of the shell
     */
    boolean isMainLoop();
}
