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

package tech.lamprism.lampray.shell.adapter;

import org.springframework.shell.command.CommandAlias;
import org.springframework.shell.command.CommandOption;
import org.springframework.shell.command.CommandRegistration;
import tech.lamprism.lampray.shell.CommandTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author RollW
 */
public class ShellCommandTree implements CommandTree {

    private final CommandRegistration commandRegistration;
    private final List<ShellCommandTree> children;
    private final String name;
    private ShellCommandTree parent;

    private ShellCommandTree(CommandRegistration commandRegistration) {
        this.commandRegistration = commandRegistration;
        this.children = new ArrayList<>();
        this.name = forLastPart(commandRegistration.getCommand());
    }

    @Override
    public List<CommandTree> getChildren() {
        return Collections.unmodifiableList(children);
    }

    private static void addChild(ShellCommandTree root, ShellCommandTree child) {
        String[] parts = child.commandRegistration.getCommand().split(" ");
        ShellCommandTree current = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                if (current.name.equals(part)) {
                    return;
                }
                ShellCommandTree newChild = new ShellCommandTree(child.commandRegistration);
                newChild.setParent(current);
                current.children.add(newChild);
                return;
            }
            ShellCommandTree prev = current;
            // Move to the next part
            current = current.children.stream()
                    .filter(c -> c.name.equals(part))
                    .findFirst()
                    .orElse(null);
            if (current == null) {
                // Create a new child if it doesn't exist
                ShellCommandTree newChild = new ShellCommandTree(
                        CommandRegistration.builder()
                                .command(String.join(" ", Arrays.copyOf(parts, i + 1)))
                                .withTarget()
                                .consumer((context) -> {})
                                .and()
                                .build()

                );
                newChild.setParent(prev);
                prev.children.add(newChild);
                current = newChild;
            }
        }
    }

    void setParent(ShellCommandTree parent) {
        this.parent = parent;
    }

    @Override
    public CommandTree getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullName() {
        if (commandRegistration == null) {
            return CommandTree.super.getFullName();
        }
        return commandRegistration.getCommand();
    }

    @Override
    public String getDescription() {
        if (commandRegistration == null) {
            return "";
        }
        return commandRegistration.getDescription();
    }

    @Override
    public String getHeader() {
        return "";
    }

    @Override
    public String getGroup() {
        if (commandRegistration == null) {
            return "";
        }
        return commandRegistration.getGroup();
    }

    @Override
    public List<String> getAliases() {
        if (commandRegistration == null) {
            return List.of();
        }
        return commandRegistration.getAliases()
                .stream()
                .map(CommandAlias::getCommand)
                .toList();
    }

    @Override
    public boolean isHidden() {
        if (commandRegistration == null) {
            return false;
        }
        return commandRegistration.isHidden();
    }

    @Override
    public List<Option> getOptions() {
        if (commandRegistration == null) {
            return List.of();
        }
        return commandRegistration.getOptions()
                .stream()
                .<Option>map(ShellOption::new)
                .toList();
    }

    private static String forLastPart(String command) {
        int lastIndex = command.lastIndexOf(' ');
        if (lastIndex == -1) {
            return command;
        }
        return command.substring(lastIndex + 1);
    }

    public static ShellCommandTree of(Map<String, CommandRegistration> commandRegistrations) {
        // Build command tree from command registrations

        // Raw:
        //  - main1
        //  - main1 main1-1
        //  - main1 main1-2
        //  - main1 main1-1 main1-1-1
        //  - main1 main1-1 main1-1-2
        //  - main2
        //  - main2 main2-1
        //
        // Transform to tree:
        //  - main1
        //    - main1-1
        //      - main1-1-1
        //      - main1-1-2
        //    - main1-2
        //  - main2
        //    - main2-1

        CommandRegistration rootRegistration = CommandRegistration.builder()
                .command("")
                .withTarget()
                .consumer((context) -> {
                })
                .and()
                .build();

        ShellCommandTree root = new ShellCommandTree(rootRegistration);
        // The smaller the command, the higher the priority
        List<CommandRegistration> sortByName = commandRegistrations.values()
                .stream()
                .sorted(Comparator.comparing(CommandRegistration::getCommand))
                .toList();
        for (CommandRegistration commandRegistration : sortByName) {
            ShellCommandTree child = new ShellCommandTree(commandRegistration);
            addChild(root, child);
        }
        return root;
    }

    static class ShellOption implements Option {
        private final CommandOption commandOption;

        private ShellOption(CommandOption commandOption) {
            this.commandOption = commandOption;
        }

        @Override
        public Set<String> getNames() {
            String[] longNames = commandOption.getLongNames();
            String[] shortNames = Arrays.stream(commandOption.getShortNames())
                    .map(String::valueOf)
                    .toArray(String[]::new);
            String[] names = new String[longNames.length + shortNames.length];
            System.arraycopy(longNames, 0, names, 0, longNames.length);
            System.arraycopy(shortNames, 0, names, longNames.length, shortNames.length);
            return Set.of(names);
        }

        @Override
        public String getLabel() {
            return commandOption.getLabel();
        }

        @Override
        public String getDescription() {
            return commandOption.getDescription();
        }

        @Override
        public boolean isRequired() {
            return commandOption.isRequired();
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public String getDefaultValue() {
            return commandOption.getDefaultValue();
        }

        @Override
        public boolean isGlobal() {
            return false;
        }
    }
}
