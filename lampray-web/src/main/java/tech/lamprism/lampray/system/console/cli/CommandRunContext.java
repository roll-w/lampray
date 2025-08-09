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

import java.io.PrintStream;
import java.util.Map;

/**
 * Context for running a command in the CLI environment.
 *
 * <p>This interface provides access to all necessary information and resources
 * for command execution, including raw arguments, parsed command structure,
 * processed arguments with their values, and output streams for user interaction.</p>
 *
 * <p>The context is designed to be immutable and thread-safe, allowing
 * command runners to focus on business logic without worrying about
 * argument parsing or output management.</p>
 *
 * @author RollW
 */
public interface CommandRunContext {

    /**
     * Get the raw command-line arguments as they were provided by the user.
     *
     * <p>This includes all arguments before any parsing or processing,
     * useful for debugging or when custom argument handling is needed.</p>
     *
     * @return array of raw command-line arguments, never null
     */
    String[] getRawArgs();

    /**
     * Get the parsed command hierarchy.
     *
     * <p>Returns the command path that led to this runner being invoked.
     * For example, if the user ran "lampray resources export", this would
     * return ["resources", "export"].</p>
     *
     * @return array representing the command hierarchy, never null
     */
    String[] getCommand();

    /**
     * Get the processed arguments and their values.
     *
     * <p>This map contains all parsed options and their values, where keys
     * are the option names (including dashes) and values are the parsed
     * option values. For boolean flags, the value will be a Boolean object.</p>
     *
     * <p>Example: For command "export --path /tmp --verbose", the map would contain:
     * {"--path": "/tmp", "--verbose": true}</p>
     *
     * @return map of argument names to their parsed values, never null
     */
    Map<String, Object> getArguments();

    /**
     * Get the print stream for command output.
     *
     * <p>Use this stream for all command output including success messages,
     * progress updates, and error messages. This ensures consistent output
     * handling and proper integration with the CLI framework.</p>
     *
     * @return print stream for command output, never null
     */
    PrintStream getPrintStream();
}
