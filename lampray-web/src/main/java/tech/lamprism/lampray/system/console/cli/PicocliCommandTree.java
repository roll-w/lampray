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

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import tech.lamprism.lampray.system.console.CommandTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author RollW
 */
public class PicocliCommandTree implements CommandTree {

    private PicocliCommandTree parent;
    private final List<PicocliCommandTree> children;
    private final CommandSpec commandSpec;

    private PicocliCommandTree(PicocliCommandTree parent,
                               List<PicocliCommandTree> children,
                               CommandSpec commandSpec) {
        this.parent = parent;
        this.children = children;
        this.commandSpec = commandSpec;
    }

    void setParent(PicocliCommandTree parent) {
        this.parent = parent;
    }

    void addChild(PicocliCommandTree child) {
        children.add(child);
        child.setParent(this);
    }

    public static PicocliCommandTree of(CommandSpec commandSpec) {
        PicocliCommandTree root = new PicocliCommandTree(null,
                new ArrayList<>(), commandSpec);
        processChildren(root, commandSpec);
        return root;
    }

    private static void processChildren(PicocliCommandTree tree, CommandSpec commandSpec) {
        if (commandSpec.subcommands().isEmpty()) {
            return;
        }
        for (Map.Entry<String, CommandLine> entry : commandSpec.subcommands().entrySet()) {
            CommandLine subCommand = entry.getValue();
            CommandSpec subCommandSpec = subCommand.getCommandSpec();
            PicocliCommandTree child = new PicocliCommandTree(tree, new ArrayList<>(), subCommandSpec);
            tree.addChild(child);
            processChildren(child, subCommandSpec);
        }
    }

    @Override
    public List<CommandTree> getChildren() {
        if (children.isEmpty()) {
            return List.of();
        }
        return List.copyOf(children);
    }

    @Override
    public CommandTree getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return commandSpec.name();
    }

    @Override
    public String getDescription() {
        return String.join(" ", commandSpec.usageMessage().description());
    }

    @Override
    public String getHeader() {
        return String.join(" ", commandSpec.usageMessage().header());
    }

    @Override
    public String getGroup() {
        CommandLine.IHelpSectionRenderer helpSectionRenderer = commandSpec.usageMessage().sectionMap().get("group");
        if (helpSectionRenderer == null) {
            return "";
        }
        return helpSectionRenderer.render(null);
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList(commandSpec.aliases());
    }

    @Override
    public boolean isHidden() {
        return commandSpec.usageMessage().hidden();
    }

    @Override
    public List<Option> getOptions() {
        return commandSpec.options()
                .stream()
                .<CommandTree.Option>map(PicocliOption::new)
                .toList();
    }

    private static class PicocliOption implements Option {

        private final OptionSpec optionSpec;

        private PicocliOption(OptionSpec optionSpec) {
            this.optionSpec = optionSpec;
        }

        @Override
        public Set<String> getNames() {
            return Set.of(optionSpec.names());
        }

        @Override
        public String getLabel() {
            return optionSpec.paramLabel();
        }

        @Override
        public String getDescription() {
            return String.join(" ", optionSpec.description());
        }

        @Override
        public boolean isRequired() {
            return optionSpec.required();
        }

        @Override
        public boolean isHidden() {
            return optionSpec.hidden();
        }

        @Override
        public String getDefaultValue() {
            return optionSpec.defaultValue();
        }

        @Override
        public boolean isGlobal() {
            return false;
        }

        @Override
        public Class<?> getType() {
            return optionSpec.type();
        }
    }
}
