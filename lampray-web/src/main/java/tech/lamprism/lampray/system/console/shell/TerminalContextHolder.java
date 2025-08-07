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
public class TerminalContextHolder implements TerminalContext {
    private static TerminalContextHolderStrategy strategy;

    public static void setStrategy(TerminalContextHolderStrategy strategy) {
        TerminalContextHolder.strategy = strategy;
    }

    public static TerminalContext getContext() {
        return strategy.getContext();
    }

    public static void setContext(TerminalContext context) {
        strategy.setContext(context);
    }

    public static void clearContext() {
        strategy.clearContext();
    }

    @Override
    public InteractionMode getInteractionMode() {
        return getContext().getInteractionMode();
    }

    @Override
    public void setInteractionMode(InteractionMode interactionMode) {
        getContext().setInteractionMode(interactionMode);
    }

    @Override
    public boolean hasPty() {
        return getContext().hasPty();
    }

    @Override
    public LineReader getLineReader() {
        return getContext().getLineReader();
    }

    @Override
    public void setLineReader(LineReader lineReader) {
        getContext().setLineReader(lineReader);
    }

    @Override
    public void setHasPty(boolean hasPty) {
        getContext().setHasPty(hasPty);
    }

    @Override
    public String getTerminalName() {
        return getContext().getTerminalName();
    }

    @Override
    public void setTerminalName(String terminalName) {
        getContext().setTerminalName(terminalName);
    }

    @Override
    public TerminalRegistry getTerminalRegistry() {
        return getContext().getTerminalRegistry();
    }

    @Override
    public boolean isMainLoop() {
        return getContext().isMainLoop();
    }
}
