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

package tech.lamprism.lampray.system.console.shell;

import org.jline.reader.LineReader;
import org.springframework.shell.context.InteractionMode;

/**
 * @author RollW
 */
public class DefaultTerminalContext implements TerminalContext {
    private final TerminalRegistry terminalRegistry;

    private InteractionMode interactionMode = InteractionMode.ALL;
    private LineReader lineReader;
    private boolean hasPty;
    private String terminalName;

    public DefaultTerminalContext(TerminalRegistry terminalRegistry) {
        this.terminalRegistry = terminalRegistry;
        this.hasPty = false;
    }

    @Override
    public InteractionMode getInteractionMode() {
        return interactionMode;
    }

    @Override
    public void setInteractionMode(InteractionMode interactionMode) {
        this.interactionMode = interactionMode;
    }

    @Override
    public boolean hasPty() {
        return hasPty;
    }

    @Override
    public LineReader getLineReader() {
        return lineReader;
    }

    @Override
    public void setLineReader(LineReader lineReader) {
        this.lineReader = lineReader;
    }

    @Override
    public void setHasPty(boolean hasPty) {
        this.hasPty = hasPty;
    }

    @Override
    public String getTerminalName() {
        return terminalName;
    }

    @Override
    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    @Override
    public TerminalRegistry getTerminalRegistry() {
        return terminalRegistry;
    }

    @Override
    public boolean isMainLoop() {
        return TerminalUtils.isMainLoop();
    }
}
