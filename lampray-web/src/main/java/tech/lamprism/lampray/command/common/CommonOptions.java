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

package tech.lamprism.lampray.command.common;

import tech.lamprism.lampray.shell.CommandSpecification;
import tech.lamprism.lampray.shell.SimpleCommandOption;

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
            .setDescription("Print this help message and exit. Use this option to display detailed " +
                    "information about the available command-line options and their usage.")
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
            .setDescription("Display the version information of the application and exit. " +
                    "This option provides a quick way to check the current version of " +
                    "the application you are running.")
            .build();
}
