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

package tech.lamprism.lampray.cli;

import org.apache.commons.lang3.StringUtils;
import org.jline.utils.AttributedString;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import space.lingu.NonNull;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.shell.CommandSpecification;
import tech.lamprism.lampray.shell.CommandTree;
import tech.lamprism.lampray.shell.HelpRenderer;
import tech.lamprism.lampray.shell.SimpleCommandSpecification;
import tech.lamprism.lampray.shell.adapter.PicocliCommandTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author RollW
 */
public class CommandLineManager implements CommandManager {

    private final List<CommandRunner> commandRunners;

    private final Map<String, CommandRunner> commandRunnerMap;

    public CommandLineManager(List<CommandRunner> commandRunners) {
        this.commandRunners = commandRunners;
        this.commandRunnerMap = commandRunners.stream().collect(Collectors.toMap(
                commandRunner -> "lampray " + commandRunner.getCommandSpecification().getFullName(),
                commandRunner -> commandRunner,
                (existing, replacement) -> {
                    throw new IllegalArgumentException("Duplicate command name: " + existing.getCommandSpecification().getFullName());
                }
        ));
    }

    private CommandSpec root;
    private CommandLine commandLine;
    private CommandTree commandTree;


    private void init() {
        root = CommandSpec.create().name("lampray");
        // TODO
        root.usageMessage()
                .header("Lampray Command Line Interface")
                .description("Use this command line interface to manage Lampray server and resources.");
        root.addSubcommand("help", CommandSpec.create()
                        .helpCommand(true))
                .usageMessage()
                .header("Print help message for a command")
                .description("Print help message and exit. Use this option to display detailed " +
                        "information about the available command-line options and their usage.");
        root.addSubcommand("version", CommandSpec.create())
                .usageMessage()
                .header("Print version information")
                .description("Display the version information of the application and exit. " +
                        "This option provides a quick way to check the current version of " +
                        "the application you are running.");
        registerCommands(root);
        CommandSpec helpMixin = CommandSpec.create()
                .addOption(OptionSpec.builder("-h", "--help")
                        .description("Print help message and exit. Use this option to display detailed " +
                                "information about the available command-line options and their usage.")
                        .usageHelp(true)
                        .defaultValue(null)
                        .build()
                )
                .addOption(OptionSpec.builder("-v", "--version")
                        .description("Display the version information of the application and exit. " +
                                "This option provides a quick way to check the current version of " +
                                "the application you are running.")
                        .versionHelp(true)
                        .defaultValue(null)
                        .build()
                );
        root.name("lampray")
                .addMixin("standardOptions", helpMixin);
        commandLine = new CommandLine(root);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);

        commandTree = PicocliCommandTree.of(root);
    }


    @Override
    public int execute(String[] args) {
        init();
        commandLine.setUnmatchedArgumentsAllowed(true);
        CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
        CommandRunner commandRunner = findCommandRunner(parseResult);
        Map<String, Object> arguments = getArguments(parseResult);
        ParsedCommandRunContext context = new ParsedCommandRunContext(args, arguments);
        return commandRunner.runCommand(context);
    }

    @NonNull
    private Map<String, Object> getArguments(CommandLine.ParseResult parseResult) {
        Map<String, Object> arguments = new HashMap<>();
        CommandLine.ParseResult current = parseResult;
        while (current.hasSubcommand()) {
            for (OptionSpec matchedOption : parseResult.matchedOptions()) {
                Object value = matchedOption.getValue();
                for (String name : matchedOption.names()) {
                    arguments.put(name, value);
                }
            }
            current = current.subcommand();
        }
        return arguments;
    }

    private CommandRunner findCommandRunner(CommandLine.ParseResult parseResult) {
        if (isVersionRequested(parseResult) || isCommandOf(parseResult, "lampray version")) {
            return new VersionCommandRunner();
        }
        if (isHelpRequested(parseResult) || isCommandOf(parseResult, "lampray help")) {
            return new HelpCommandRunner(commandTree);
        }

        return getCommandRunner(parseResult);
    }

    private boolean isVersionRequested(CommandLine.ParseResult parseResult) {
        if (parseResult.isVersionHelpRequested()) {
            return true;
        }
        if (parseResult.hasSubcommand()) {
            return isVersionRequested(parseResult.subcommand());
        }
        return false;
    }

    private boolean isHelpRequested(CommandLine.ParseResult parseResult) {
        if (parseResult.isUsageHelpRequested()) {
            return true;
        }
        if (parseResult.hasSubcommand()) {
            return isHelpRequested(parseResult.subcommand());
        }
        return isCommandOf(parseResult, "lampray help");
    }

    private boolean isCommandOf(CommandLine.ParseResult parseResult, String commandName) {
        if (StringUtils.equalsIgnoreCase(parseResult.commandSpec().qualifiedName().trim(), commandName)) {
            return true;
        }
        if (parseResult.hasSubcommand()) {
            return isCommandOf(parseResult.subcommand(), commandName);
        }
        return false;
    }

    private void registerCommands(CommandSpec root) {
        List<CommandRunner> sortByName = commandRunners
                .stream()
                .sorted(Comparator.comparing(commandRunner -> commandRunner.getCommandSpecification().getFullName()))
                .toList();
        for (CommandRunner commandRunner : sortByName) {
            addChildCommands(root, commandRunner);
        }
    }

    private void addChildCommands(CommandSpec root, CommandRunner child) {
        String[] parts = child.getCommandSpecification().getFullName().split(" ");
        CommandSpec current = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                if (current.name().equals(part)) {
                    return;
                }
                CommandSpecification commandSpecification = child.getCommandSpecification();
                CommandSpec commandSpec = PicocliHelper.from(commandSpecification);
                current.addSubcommand(commandSpec.name(), commandSpec);
                return;
            }
            CommandSpec prev = current;
            // Move to the next part
            current = current.subcommandsCaseInsensitive(false)
                    .subcommands()
                    .values()
                    .stream()
                    .filter(c -> c.getCommandName().equals(part))
                    .findFirst()
                    .map(CommandLine::getCommandSpec)
                    .orElse(null);
            if (current == null) {
                // Create a new child if it doesn't exist
                CommandSpec newChild = CommandSpec.create().name(part);
                String commandToThisPart = StringUtils.join(parts, " ", 0, i + 1);
                newChild.usageMessage()
                        .header("Command: " + commandToThisPart)
                        .description("Print the help message for the command: " + commandToThisPart);
                prev.addSubcommand(newChild.name(), newChild);
                current = newChild;
            }
        }
    }

    private CommandRunner getCommandRunner(CommandLine.ParseResult parseResult) {
        if (!parseResult.hasSubcommand()) {
            return getCommandRunner(parseResult.commandSpec()
                    .qualifiedName().trim());
        }
        return getCommandRunner(parseResult.subcommand());
    }

    private CommandRunner getCommandRunner(String commandName) {
        CommandRunner commandRunner = commandRunnerMap.get(commandName);
        if (commandRunner == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }
        return commandRunner;
    }

    private static class VersionCommandRunner implements CommandRunner {
        @Override
        public int runCommand(CommandRunContext context) {
            System.out.println(Version.formatVersion());
            return 0;
        }

        @Override
        public CommandSpecification getCommandSpecification() {
            return SimpleCommandSpecification.builder().build();
        }
    }

    private static class HelpCommandRunner implements CommandRunner {
        private final CommandTree commandTree;

        private HelpCommandRunner(CommandTree commandTree) {
            this.commandTree = commandTree;
        }

        @Override
        public int runCommand(CommandRunContext context) {
            HelpRenderer helpRenderer = new HelpRenderer(commandTree, "lampray");
            // TODO: enhance
            AttributedString help = helpRenderer.getHelp(processArgs(context.getRawArgs()));
            System.out.println(help.toAnsi());
            return 0;
        }

        private String[] processArgs(String[] args) {
            List<String> arguments = new ArrayList<>(Stream.of(args)
                    .filter(arg -> !arg.startsWith("-"))
                    .filter(arg -> !arg.equals("help"))
                    .filter(StringUtils::isNotBlank)
                    .toList());
            if (arguments.isEmpty()) {
                return new String[]{};
            }
            arguments.add(0, "lampray");
            return arguments.toArray(new String[0]);
        }

        @Override
        public CommandSpecification getCommandSpecification() {
            return SimpleCommandSpecification.builder().build();
        }
    }

}
