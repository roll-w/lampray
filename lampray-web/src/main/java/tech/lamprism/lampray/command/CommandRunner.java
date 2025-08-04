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

package tech.lamprism.lampray.command;

import space.lingu.NonNull;
import tech.lamprism.lampray.shell.CommandSpecification;

/**
 * @author RollW
 */
public interface CommandRunner {

    /**
     * Run a command.
     *
     * @param context the context of the command to run, which contains
     *                the arguments and other information.
     * @return exit code of the command. 0 means success, non-zero
     * means failure.
     */
    int runCommand(CommandRunContext context);

    @NonNull
    Type getType();

    CommandSpecification getCommandSpecification();

    enum Type {
        /**
         * Run the command in a standalone mode. Do not use the
         * application context.
         */
        STANDALONE,

        /**
         * Run the command in a full web application context, and don't
         * exit the application after running the command.
         */
        SERVICE,

        /**
         * Run the command in a full web application context, and exit the
         * application after running the command.
         */
        SERVICE_EXIT,

        /**
         * Run the command in a limited web application context (don't start
         * the web server), and exit the application after running the command.
         */
        TASK,

        /**
         * Run the command in a limited web application context (don't start
         * the web server), and don't exit the application after running the
         * command.
         */
        TASK_KEEP_ALIVE,
    }
}
