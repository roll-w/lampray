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
import tech.lamprism.lampray.system.console.HelpRenderer;
import tech.lamprism.lampray.system.console.SimpleCommandOption;

/**
 * Utility class providing commonly used command-line options across different commands.
 *
 * <p>This class defines standard options that are frequently used by multiple commands
 * in the Lampray CLI system. It promotes consistency in option naming, descriptions,
 * and behavior across the application.</p>
 *
 * <p>All options defined here are designed to be reusable and follow consistent
 * naming conventions and behavior patterns. Command implementations can include
 * these options using the static factory methods provided.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * SimpleCommandSpecification.builder()
 *     .setNames("my-command")
 *     .addOption(CommonOptions.help())
 *     .addOption(CommonOptions.verbose())
 *     .addOption(CommonOptions.config())
 *     .build();
 * }</pre>
 *
 * @author RollW
 */
public final class CommonOptions {
    private CommonOptions() {
    }

    /**
     * Standard help option for displaying command usage and documentation.
     *
     * <p>This option is automatically available for all commands and provides
     * detailed information about command usage, available options, and examples.</p>
     */
    public static final CommandSpecification.Option HELP = SimpleCommandOption.builder()
            .setNames("--help", "-h")
            .setDescription("Display detailed help information for this command, including usage examples and option descriptions")
            .setRequired(false)
            .setLabel(HelpRenderer.NO_PARAM)
            .build();

    /**
     * Configuration file path option for custom application settings.
     *
     * <p>Allows users to specify a custom configuration file path instead of
     * using the default configuration. Supports both absolute and relative paths.</p>
     */
    public static final CommandSpecification.Option CONFIG = SimpleCommandOption.builder()
            .setNames("--config", "-c")
            .setDescription("Path to custom configuration file (absolute or relative to current directory)")
            .setRequired(false)
            .setType(String.class)
            .build();

    /**
     * Version information option for displaying application version details.
     *
     * <p>Shows the current application version, build information, and other
     * relevant version-related details when invoked.</p>
     */
    public static final CommandSpecification.Option VERSION = SimpleCommandOption.builder()
            .setNames("--version", "-v")
            .setDescription("Display application version, build information, and exit")
            .setRequired(false)
            .setLabel(HelpRenderer.NO_PARAM)
            .build();

    /**
     * Verbose output option for detailed logging and progress information.
     *
     * <p>When enabled, commands will provide more detailed output including
     * progress information, debug messages, and detailed operation logs.</p>
     */
    public static final CommandSpecification.Option VERBOSE = SimpleCommandOption.builder()
            .setNames("--verbose")
            .setDescription("Enable verbose output with detailed progress information and debug messages")
            .setRequired(false)
            .setType(Boolean.class)
            .build();

    /**
     * Quiet mode option for minimal output during command execution.
     *
     * <p>When enabled, commands will suppress non-essential output and only
     * display critical information, errors, and final results.</p>
     */
    public static final CommandSpecification.Option QUIET = SimpleCommandOption.builder()
            .setNames("--quiet", "-q")
            .setDescription("Suppress non-essential output, showing only errors and final results")
            .setRequired(false)
            .setType(Boolean.class)
            .build();
}
