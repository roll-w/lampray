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
import org.jline.utils.AttributedString;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import tech.lamprism.lampray.Version;
import tech.lamprism.lampray.system.console.CommandSpecification;
import tech.lamprism.lampray.system.console.CommandTree;
import tech.lamprism.lampray.system.console.HelpRenderer;
import tech.lamprism.lampray.system.console.SimpleCommandSpecification;

import java.util.ArrayList;
import java.util.Arrays;
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
public class LamprayCommandLineManager implements CommandManager {

    private final List<CommandRunner> commandRunners;
    private final Map<String, CommandRunner> commandRunnerMap;
    private final String commandPrefix;
    private final String headerText;

    /**
     * Creates a new CommandLineManager with default configuration.
     *
     * @param commandRunners the list of command runners to register
     */
    public LamprayCommandLineManager(List<CommandRunner> commandRunners) {
        this(commandRunners, "lampray", "Lampray Command Line Interface");
    }

    /**
     * Creates a new CommandLineManager with customizable prefix and header.
     *
     * @param commandRunners the list of command runners to register
     * @param commandPrefix  the command prefix
     * @param headerText     the header text for help display
     */
    public LamprayCommandLineManager(List<CommandRunner> commandRunners, String commandPrefix, String headerText) {
        this.commandRunners = commandRunners;
        this.commandPrefix = StringUtils.defaultIfEmpty(commandPrefix, "");
        this.headerText = StringUtils.defaultIfEmpty(headerText, "");
        this.commandRunnerMap = commandRunners.stream().collect(Collectors.toMap(
                commandRunner -> commandRunner.getCommandSpecification().getFullName(),
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
                .description("Use this command line interface to manage server and resources. " +
                        "Available commands include server management, data operations, and system configuration.");

        // Add built-in help command with enhanced description
        CommandSpec helpCommand = CommandSpec.create()
                // TODO
                .addOption(OptionSpec.builder("--command", "-C")
                        .paramLabel("COMMAND")
                        .arity("0..*")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .required(false)
                        .build())
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

        root.name("").addMixin("standardOptions", helpMixin);
        commandLine = new CommandLine(root);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);

        commandTree = PicocliCommandTree.of(root);
        commandRunnerMap.put("", helpCommandRunner);
    }


    @Override
    public int execute(String[] args) {
        init();
        commandLine.setUnmatchedArgumentsAllowed(true);
        CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
        CommandRunner commandRunner = findCommandRunner(parseResult);
        ParsedCommandRunContext context = buildParsedCommandRunContext(parseResult);
        return commandRunner.runCommand(context);
    }

    private ParsedCommandRunContext buildParsedCommandRunContext(CommandLine.ParseResult parseResult) {
        String[] rawArgs = parseResult.originalArgs().toArray(new String[0]);
        Map<String, Object> arguments = new HashMap<>();
        CommandLine.ParseResult current = parseResult;
        String command = null;
        while (current != null) {
            for (OptionSpec matchedOption : current.matchedOptions()) {
                Object value = matchedOption.getValue();
                for (String name : matchedOption.names()) {
                    arguments.put(name, value);
                }
            }
            command = current.commandSpec().qualifiedName().trim();
            current = current.subcommand();
        }
        return new ParsedCommandRunContext(command.split(" "), rawArgs, arguments);
    }

    private CommandRunner findCommandRunner(CommandLine.ParseResult parseResult) {
        if (isVersionRequested(parseResult) || isCommandOf(parseResult, "version", false)) {
            return new VersionCommandRunner();
        }
        if (isHelpRequested(parseResult) || isCommandOf(parseResult, "help", true)) {
            return helpCommandRunner;
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
                PicocliHelper.appendHelpOption(newChild);
                commandRunnerMap.put(commandToThisPart, helpCommandRunner);
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
            context.getPrintStream().println(Version.formatVersion());
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

    private final HelpCommandRunner helpCommandRunner = new HelpCommandRunner();

    private class HelpCommandRunner implements CommandRunner {
        @Override
        public int runCommand(CommandRunContext context) {
            HelpRenderer helpRenderer = new HelpRenderer(commandTree, commandPrefix, headerText);
            AttributedString help = helpRenderer.getHelp(processCommand(context.getCommand()));
            context.getPrintStream().println(help);
            return 0;
        }

        private String[] processCommand(String[] args) {
            if (args == null || args.length == 0) {
                return new String[]{};
            }
            List<String> arguments = new ArrayList<>(Arrays.asList(args));
            if (arguments.get(0).equals("help")) {
                arguments.remove(0);
            }
            return arguments.toArray(new String[0]);
        }

        @Override
        public CommandSpecification getCommandSpecification() {
            return SimpleCommandSpecification.builder()
                    .build();
        }
    }
}
