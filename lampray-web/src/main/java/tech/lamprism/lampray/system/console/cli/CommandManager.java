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

package tech.lamprism.lampray.system.console.cli;

/**
 * Central manager for command-line interface execution in the application.
 *
 * <p>This interface provides the main entry point for CLI command processing.
 * It handles command parsing, routing to appropriate command runners, and
 * managing the overall execution lifecycle.</p>
 *
 * <p>The command manager is responsible for:</p>
 * <ul>
 *   <li>Parsing raw command-line arguments</li>
 *   <li>Resolving command hierarchies and sub-commands</li>
 *   <li>Routing commands to their appropriate runners</li>
 *   <li>Handling global options and error conditions</li>
 *   <li>Managing command execution context and resources</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * CommandManager manager = new LamprayCommandLineManager();
 * int exitCode = manager.execute(new String[]{"resources", "export", "--path", "/tmp"});
 * }</pre>
 *
 * @author RollW
 */
public interface CommandManager {

    /**
     * Execute a command with the provided arguments.
     *
     * <p>This method processes the complete command-line input, from argument
     * parsing through command execution. It handles all aspects of command
     * processing including option validation, help generation, and error handling.</p>
     *
     * <p>The method follows Unix conventions for exit codes:</p>
     * <ul>
     *   <li>0 - Successful execution</li>
     *   <li>1 - General error or command failure</li>
     *   <li>2 - Invalid arguments or usage error</li>
     *   <li>Other non-zero values - Specific error conditions</li>
     * </ul>
     *
     * @param args the command-line arguments array, starting with the command name.
     *             For example: ["resources", "export", "--path", "/tmp"]
     * @return exit code indicating the result of command execution.
     *         Zero indicates success, non-zero indicates various error conditions
     */
    int execute(String[] args);
}
