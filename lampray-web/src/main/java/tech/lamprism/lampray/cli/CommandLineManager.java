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

/**
 * Command line manager for Lampray CLI operations.
 * Manages command registration, parsing, and execution with support for hierarchical commands.
 *
 * @author RollW
 */
public class CommandLineManager implements CommandManager {

    private final List<CommandRunner> commandRunners;
    private final Map<String, CommandRunner> commandRunnerMap;
    private final String commandPrefix;
    private final String headerText;

    /**
     * Creates a new CommandLineManager with default configuration.
     *
     * @param commandRunners the list of command runners to register
     */
    public CommandLineManager(List<CommandRunner> commandRunners) {
        this(commandRunners, "lampray", "Lampray Command Line Interface");
    }

    /**
     * Creates a new CommandLineManager with customizable prefix and header.
     *
     * @param commandRunners the list of command runners to register
     * @param commandPrefix  the command prefix (default: "lampray")
     * @param headerText     the header text for help display
     */
    public CommandLineManager(List<CommandRunner> commandRunners, String commandPrefix, String headerText) {
        this.commandRunners = commandRunners;
        this.commandPrefix = commandPrefix;
        this.headerText = headerText;
        this.commandRunnerMap = commandRunners.stream().collect(Collectors.toMap(
                commandRunner -> commandPrefix + " " + commandRunner.getCommandSpecification().getFullName(),
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
        root = CommandSpec.create();
        root.usageMessage()
                .header(headerText)
                .description("Use this command line interface to manage " + commandPrefix + " server and resources. " +
                        "Available commands include server management, data operations, and system configuration.");

        // Add built-in help command with enhanced description
        CommandSpec helpCommand = CommandSpec.create()
                .helpCommand(true);
        root.addSubcommand("help", helpCommand);
        helpCommand.usageMessage()
                .header("Display help information for commands")
                .description("Print comprehensive help message and exit. Use this option to display detailed " +
                        "information about available command-line options, their usage patterns, and examples.");

        // Add built-in version command with enhanced description
        CommandSpec version = CommandSpec.create();
        version.usageMessage()
                .header("Display version and build information")
                .description("Show the version information of the application including build date, " +
                        "commit hash, and other version details. This option provides a quick way to " +
                        "verify the current version of the application you are running.");
        root.addSubcommand("version", version);

        registerCommands(root);

        CommandSpec helpMixin = CommandSpec.create()
                .addOption(OptionSpec.builder("-h", "--help")
                        .description("Display help information for the command and exit. Shows detailed " +
                                "information about available options, usage patterns, and examples.")
                        .usageHelp(true)
                        .paramLabel(HelpRenderer.NO_PARAM)
                        .defaultValue(null)
                        .build()
                )
                .addOption(OptionSpec.builder("-v", "--version")
                        .description("Display version information and exit. Shows the current version, " +
                                "build information, and other version-related details.")
                        .versionHelp(true)
                        .paramLabel(HelpRenderer.NO_PARAM)
                        .defaultValue(null)
                        .build()
                );

        root.name(commandPrefix).addMixin("standardOptions", helpMixin);
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
        if (isVersionRequested(parseResult) || isCommandOf(parseResult, commandPrefix + " version", false)) {
            return new VersionCommandRunner();
        }
        if (isHelpRequested(parseResult) || isCommandOf(parseResult, commandPrefix + " help", true)) {
            return new HelpCommandRunner(commandTree, commandPrefix);
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
        return false;
    }

    private boolean isCommandOf(CommandLine.ParseResult parseResult, String commandName, boolean startWith) {
        if (startWith) {
            if (StringUtils.startsWithIgnoreCase(parseResult.commandSpec().qualifiedName().trim(), commandName)) {
                return true;
            }
        } else {
            if (StringUtils.equalsIgnoreCase(parseResult.commandSpec().qualifiedName().trim(), commandName)) {
                return true;
            }
        }
        if (parseResult.hasSubcommand()) {
            return isCommandOf(parseResult.subcommand(), commandName, startWith);
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
            current = current.subcommandsCaseInsensitive(false)
                    .subcommands()
                    .values()
                    .stream()
                    .filter(c -> c.getCommandName().equals(part))
                    .findFirst()
                    .map(CommandLine::getCommandSpec)
                    .orElse(null);
            if (current == null) {
                CommandSpec newChild = CommandSpec.create().name(part);
                String commandToThisPart = StringUtils.join(parts, " ", 0, i + 1);
                newChild.usageMessage()
                        .header("Command Group: " + commandToThisPart)
                        .description("Manage " + commandToThisPart + " operations. Use 'help " +
                                commandToThisPart + "' to see available subcommands and their descriptions.");
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
            return SimpleCommandSpecification.builder()
                    .setName("version")
                    .setDescription("Display version and build information")
                    .build();
        }
    }

    private static class HelpCommandRunner implements CommandRunner {
        private final CommandTree commandTree;
        private final String commandPrefix;

        private HelpCommandRunner(CommandTree commandTree, String commandPrefix) {
            this.commandTree = commandTree;
            this.commandPrefix = commandPrefix;
        }

        @Override
        public int runCommand(CommandRunContext context) {
            HelpRenderer helpRenderer = new HelpRenderer(commandTree, commandPrefix);
            AttributedString help = helpRenderer.getHelp(processArgs(context.getRawArgs()));
            System.out.println(help.toAnsi());
            return 0;
        }

        private String[] processArgs(String[] args) {
            List<String> arguments = new ArrayList<>();
            boolean foundHelp = false;

            for (String arg : args) {
                if ("help".equals(arg) || "-h".equals(arg) || "--help".equals(arg)) {
                    foundHelp = true;
                    continue; // Skip the help command itself
                }
                if (arg.startsWith("-")) {
                    continue; // Skip option flags
                }
                if (StringUtils.isNotBlank(arg)) {
                    arguments.add(arg);
                }
            }

            // If we found help command and there are additional arguments,
            // they represent the command to get help for
            if (foundHelp && !arguments.isEmpty()) {
                arguments.add(0, commandPrefix);
                return arguments.toArray(new String[0]);
            }

            // Default case: show general help
            return new String[]{};
        }

        @Override
        public CommandSpecification getCommandSpecification() {
            return SimpleCommandSpecification.builder()
                    .setName("help")
                    .setDescription("Display help information for commands")
                    .build();
        }
    }

}
