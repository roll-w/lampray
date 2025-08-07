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

import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import tech.lamprism.lampray.system.console.CommandSpecification;
import tech.lamprism.lampray.system.console.HelpRenderer;

/**
 * @author RollW
 */
public final class PicocliHelper {
    private PicocliHelper() {
    }

    public static CommandSpec from(CommandSpecification commandSpecification) {
        CommandSpec commandSpec = CommandSpec.create();
        commandSpec.name(commandSpecification.getName());
        commandSpec.usageMessage()
                .header(commandSpecification.getHeader())
                .description(commandSpecification.getDescription());

        commandSpecification.getOptions().stream()
                .map(PicocliHelper::from)
                .forEach(commandSpec::addOption);

        // Auto append help option to all commands
        appendHelpOption(commandSpec);

        return commandSpec;
    }

    public static void appendHelpOption(CommandSpec commandSpec) {
        commandSpec.addOption(OptionSpec.builder("-h", "--help")
                .usageHelp(true)
                .paramLabel(HelpRenderer.NO_PARAM)
                .description("Print help for current command.")
                .build());
    }

    private static OptionSpec from(CommandSpecification.Option option) {
        OptionSpec.Builder builder = OptionSpec
                .builder(option.getNames().toArray(new String[0]))
                .description(option.getDescription())
                .required(option.isRequired())
                .hidden(option.isHidden())
                .defaultValue(option.getDefaultValue())
                .type(option.getType());
        if (option.getLabel() != null && !option.getLabel().isEmpty()) {
            builder.paramLabel(option.getLabel());
        }
        if (StringUtils.isBlank(option.getLabel())) {
            builder.paramLabel(HelpRenderer.NO_PARAM);
        }

        return builder.build();
    }

}
