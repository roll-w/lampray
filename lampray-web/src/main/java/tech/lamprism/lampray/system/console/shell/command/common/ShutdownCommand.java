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

package tech.lamprism.lampray.system.console.shell.command.common;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.system.console.CommandGroups;

/**
 * @author RollW
 */
@Component
@Command(command = "shutdown", description = "Shutdown the application gracefully",
        group = CommandGroups.COMMON)
public class ShutdownCommand extends AbstractShellComponent {
    private final ConfigurableApplicationContext applicationContext;

    public ShutdownCommand(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Command
    public void shutdown() {
        getTerminal().writer().println("Shutting down the application...");
        getTerminal().writer().flush();
        applicationContext.close();
        System.exit(0);
    }
}
