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

package tech.lamprism.lampray.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import tech.lamprism.lampray.system.console.cli.CommandManager;
import tech.lamprism.lampray.system.console.cli.CommandRunner;
import tech.lamprism.lampray.system.console.cli.LamprayCommandLineManager;
import tech.lamprism.lampray.system.console.cli.runner.ResourcesExportCommandRunner;

import java.util.List;

@SpringBootApplication(scanBasePackages = "tech.lamprism.lampray", exclude = {
        FreeMarkerAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class
})
public class LampraySystemApplication {

    private static final List<CommandRunner> COMMAND_RUNNERS = List.of(
            new StartApplicationCommandRunner(),
            new ResourcesExportCommandRunner()
    );

    public static void main(String[] args) {
        CommandManager commandManager = new LamprayCommandLineManager(COMMAND_RUNNERS);
        int exitCode = commandManager.execute(args);

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
