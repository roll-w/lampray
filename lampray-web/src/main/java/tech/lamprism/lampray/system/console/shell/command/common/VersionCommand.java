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

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.system.console.CommandGroups;

/**
 * @author RollW
 */
@Component
@Command(command = "version", description = "Display application version, build information and runtime environment",
        group = CommandGroups.COMMON)
public class VersionCommand extends AbstractShellComponent {
    @Command
    public void version() {
        getTerminal().writer().println(Version.formatVersion());
    }
}
