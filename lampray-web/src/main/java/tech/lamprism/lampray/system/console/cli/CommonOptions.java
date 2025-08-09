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
 * @author RollW
 */
public final class CommonOptions {
    private CommonOptions() {
    }

    /**
     * Help option will append for all commands.
     */
    public static final CommandSpecification.Option HELP = SimpleCommandOption.builder()
            .setNames("--help", "-h")
            .setDescription("Print help message and exit. Use this option to display detailed " +
                    "information about the available commands and options.")
            .setRequired(false)
            .setLabel(HelpRenderer.NO_PARAM)
            .build();

    public static final CommandSpecification.Option CONFIG = SimpleCommandOption.builder()
            .setNames("--config", "-c")
            .setDescription("Specify the path to the configuration file. This option allows " +
                    "you to provide a custom configuration file for the application " +
                    "to use. The path can be absolute or relative to current working " +
                    "directory.")
            .setRequired(false)
            .setType(String.class)
            .build();

    public static final CommandSpecification.Option VERSION = SimpleCommandOption.builder()
            .setNames("--version", "-v")
            .setDescription("Display version information and exit. Shows the current version, " +
                    "build information, and other details.")
            .setRequired(false)
            .setLabel(HelpRenderer.NO_PARAM)
            .build();
}
