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

package tech.lamprism.lampray.cli;

import java.io.PrintStream;
import java.util.Map;

/**
 * @author RollW
 */
public class ParsedCommandRunContext implements CommandRunContext {
    private final String[] args;
    private final Map<String, Object> arguments;
    private PrintStream printStream = System.out;

    private boolean startApplication = true;
    private boolean startWebServer = true;

    public ParsedCommandRunContext(String[] args, Map<String, Object> arguments) {
        this.args = args;
        this.arguments = arguments;
    }

    @Override
    public String[] getRawArgs() {
        return args;
    }

    @Override
    public Map<String, Object> getArguments() {
        return arguments;
    }

    @Override
    public PrintStream getPrintStream() {
        return printStream;
    }

    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    @Override
    public void setStartApplication(boolean startApplication) {
        this.startApplication = startApplication;
    }

    @Override
    public boolean isStartApplication() {
        return startApplication;
    }

    @Override
    public void setStartWebServer(boolean startWebServer) {
        this.startWebServer = startWebServer;
    }

    @Override
    public boolean isStartWebServer() {
        return startWebServer;
    }
}
