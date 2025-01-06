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

package tech.lamprism.lampray.shell.terminal;

import com.google.common.base.Strings;
import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.ColorPalette;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;
import space.lingu.NonNull;
import tech.lamprism.lampray.shell.TerminalContext;
import tech.lamprism.lampray.shell.TerminalContextHolder;
import tech.lamprism.lampray.shell.TerminalRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * @author RollW
 */
@SuppressWarnings("resource")
public class MultiTerminal implements Terminal, TerminalRegistry {
    private final List<Terminal> terminals;
    private Terminal fallbackTerminal;

    public MultiTerminal(List<Terminal> terminals) {
        this(terminals, 0);
    }

    public MultiTerminal(List<Terminal> terminals, int fallbackTerminalIndex) {
        if (terminals.size() < fallbackTerminalIndex + 1) {
            throw new IllegalArgumentException("Fallback terminal index is out of bounds.");
        }
        this.terminals = new ArrayList<>(terminals);
        this.fallbackTerminal = terminals.get(fallbackTerminalIndex);
    }

    public Terminal getFallbackTerminal() {
        return fallbackTerminal;
    }

    public void setFallbackTerminal(Terminal fallbackTerminal) {
        this.fallbackTerminal = fallbackTerminal;
    }

    @NonNull
    private Terminal findCurrentTerminal() {
        TerminalContext shellContext = TerminalContextHolder.getContext();
        if (Strings.isNullOrEmpty(shellContext.getTerminalName())) {
            return fallbackTerminal;
        }
        for (Terminal terminal : terminals) {
            if (terminal.getName().equals(shellContext.getTerminalName())) {
                return terminal;
            }
        }
        return fallbackTerminal;
    }

    @Override
    public String getName() {
        return findCurrentTerminal().getName();
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        return findCurrentTerminal().handle(signal, handler);
    }

    @Override
    public void raise(Signal signal) {
        findCurrentTerminal().raise(signal);
    }

    @Override
    public NonBlockingReader reader() {
        return findCurrentTerminal().reader();
    }

    @Override
    public PrintWriter writer() {
        return findCurrentTerminal().writer();
    }

    @Override
    public Charset encoding() {
        return findCurrentTerminal().encoding();
    }

    @Override
    public InputStream input() {
        return findCurrentTerminal().input();
    }

    @Override
    public OutputStream output() {
        return findCurrentTerminal().output();
    }

    @Override
    public boolean canPauseResume() {
        return findCurrentTerminal().canPauseResume();
    }

    @Override
    public void pause() {
        findCurrentTerminal().pause();
    }

    @Override
    public void pause(boolean wait) throws InterruptedException {
        findCurrentTerminal().pause(wait);
    }

    @Override
    public void resume() {
        findCurrentTerminal().resume();
    }

    @Override
    public boolean paused() {
        return findCurrentTerminal().paused();
    }

    @Override
    public Attributes enterRawMode() {
        return findCurrentTerminal().enterRawMode();
    }

    @Override
    public boolean echo() {
        return findCurrentTerminal().echo();
    }

    @Override
    public boolean echo(boolean echo) {
        return findCurrentTerminal().echo(echo);
    }

    @Override
    public Attributes getAttributes() {
        return findCurrentTerminal().getAttributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        findCurrentTerminal().setAttributes(attr);
    }

    @Override
    public Size getSize() {
        return findCurrentTerminal().getSize();
    }

    @Override
    public void setSize(Size size) {
        findCurrentTerminal().setSize(size);
    }

    @Override
    public int getWidth() {
        return findCurrentTerminal().getWidth();
    }

    @Override
    public int getHeight() {
        return findCurrentTerminal().getHeight();
    }

    @Override
    public Size getBufferSize() {
        return findCurrentTerminal().getBufferSize();
    }

    @Override
    public void flush() {
        findCurrentTerminal().flush();
    }

    @Override
    public String getType() {
        return findCurrentTerminal().getType();
    }

    @Override
    public boolean puts(InfoCmp.Capability capability, Object... params) {
        return findCurrentTerminal().puts(capability, params);
    }

    @Override
    public boolean getBooleanCapability(InfoCmp.Capability capability) {
        return findCurrentTerminal().getBooleanCapability(capability);
    }

    @Override
    public Integer getNumericCapability(InfoCmp.Capability capability) {
        return findCurrentTerminal().getNumericCapability(capability);
    }

    @Override
    public String getStringCapability(InfoCmp.Capability capability) {
        return findCurrentTerminal().getStringCapability(capability);
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return findCurrentTerminal().getCursorPosition(discarded);
    }

    @Override
    public boolean hasMouseSupport() {
        return findCurrentTerminal().hasMouseSupport();
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        return findCurrentTerminal().trackMouse(tracking);
    }

    @Override
    public MouseEvent readMouseEvent() {
        return findCurrentTerminal().readMouseEvent();
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader) {
        return findCurrentTerminal().readMouseEvent(reader);
    }

    @Override
    public boolean hasFocusSupport() {
        return findCurrentTerminal().hasFocusSupport();
    }

    @Override
    public boolean trackFocus(boolean tracking) {
        return findCurrentTerminal().trackFocus(tracking);
    }

    @Override
    public ColorPalette getPalette() {
        return findCurrentTerminal().getPalette();
    }

    @Override
    public void close() throws IOException {
        Terminal currentTerminal = findCurrentTerminal();
        if (currentTerminal == fallbackTerminal) {
            return;
        }
        currentTerminal.close();
        terminals.remove(currentTerminal);
    }

    public void closeAll() throws IOException {
        for (Terminal terminal : terminals) {
            terminal.close();
        }
    }

    @Override
    public void registerTerminal(Terminal terminal) {
        terminals.add(terminal);
    }

    @Override
    public void unregisterTerminal(Terminal terminal) {
        terminals.remove(terminal);
    }

    @Override
    public void unregisterTerminal(String terminalName) {
        for (Terminal terminal : terminals) {
            if (terminal.getName().equals(terminalName)) {
                unregisterTerminal(terminal);
                break;
            }
        }
    }

    @Override
    public Terminal findTerminal(String terminalName) {
        return terminals.stream()
                .filter(terminal -> terminal.getName().equals(terminalName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean hasTerminal(String terminalName) {
        for (Terminal terminal : terminals) {
            if (terminal.getName().equals(terminalName)) {
                return true;
            }
        }
        return false;
    }
}
