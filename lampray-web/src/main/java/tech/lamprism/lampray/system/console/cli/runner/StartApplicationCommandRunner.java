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

package tech.lamprism.lampray.system.console.cli.runner;

import org.springframework.context.ApplicationContext;
import space.lingu.NonNull;
import tech.lamprism.lampray.system.console.CommandGroups;
import tech.lamprism.lampray.system.console.SimpleCommandSpecification;
import tech.lamprism.lampray.system.console.cli.CommandRunContext;

import java.util.Map;

/**
 * Command runner for starting the Lampray application.
 * This command is used to start the application with the specified configuration.
 *
 * @author RollW
 */
public class StartApplicationCommandRunner extends AbstractApplicationContextCommandRunner {

    @NonNull
    @Override
    public Type getType() {
        return Type.SERVICE;
    }

    @Override
    protected int doRunCommand(CommandRunContext context, ApplicationContext applicationContext) {
        return 0;
    }

    @Override
    protected void onProcessProperties(Map<String, Object> properties) {
    }

    @Override
    protected void onConfigureCommandSpecification(SimpleCommandSpecification.Builder builder) {
        builder.setNames("start")
                .setDescription("Start the Lampray application with the specified configuration.")
                .setHeader("Start the Lampray application.")
                .setGroup(CommandGroups.APPLICATION);
    }
}
