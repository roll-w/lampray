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

import tech.lamprism.lampray.system.console.CommandSpecification;

/**
 * Interface for command runners that execute CLI commands.
 *
 * <p>Command runners are responsible for executing specific commands with their
 * associated arguments and options. Each runner must provide both execution logic
 * and command specification for help and validation purposes.</p>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * public class MyCommandRunner implements CommandRunner {
 *     public int runCommand(CommandRunContext context) {
 *         // Execute command logic
 *         return 0; // Success
 *     }
 *
 *     public CommandSpecification getCommandSpecification() {
 *         return SimpleCommandSpecification.builder()
 *             .setNames("my-command")
 *             .setDescription("Description of my command")
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @author RollW
 */
public interface CommandRunner {

    /**
     * Execute a command with the provided context.
     *
     * <p>The context contains all necessary information for command execution,
     * including parsed arguments, options, and output streams. Implementations
     * should handle errors gracefully and provide meaningful feedback to users.</p>
     *
     * @param context the command execution context containing arguments,
     *                options, and output streams for user interaction
     * @return exit code following Unix conventions: 0 for success,
     * non-zero values for various failure conditions
     */
    int runCommand(CommandRunContext context);

    /**
     * Get the command specification for this runner.
     *
     * <p>The specification defines the command structure, including names,
     * descriptions, options, and help text. This information is used for
     * command registration, help generation, and argument validation.</p>
     *
     * @return the command specification containing all metadata
     * and option definitions for this command
     */
    CommandSpecification getCommandSpecification();
}
