/*
 * Copyright 2017-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.lamprism.lampray.shell;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jline.terminal.Terminal;
import org.jline.utils.Signals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.shell.CommandNotFound;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.ResultHandlerService;
import org.springframework.shell.Shell;
import org.springframework.shell.Utils;
import org.springframework.shell.command.CommandAlias;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandExecution;
import org.springframework.shell.command.CommandExecution.CommandExecutionException;
import org.springframework.shell.command.CommandExecution.CommandExecutionHandlerMethodArgumentResolvers;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.shell.command.CommandOption;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.completion.CompletionResolver;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.exit.ExitCodeExceptionProvider;
import org.springframework.shell.exit.ExitCodeMappings;
import org.springframework.util.StringUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main class implementing a shell loop, with support for command operators,
 * like {@code &&} to chain commands.
 *
 * @author Eric Bottard
 * @author Janne Valkealahti
 * @author RollW
 */
public class OperatorSupportedShell extends Shell {

    private final static Logger log = LoggerFactory.getLogger(OperatorSupportedShell.class);
    private final ResultHandlerService resultHandlerService;

    /**
     * Marker object returned to signify that there was no input to turn into a command
     * execution.
     */
    protected static final List<InvokeHandle> NO_INPUT = List.of();

    private final Terminal terminal;
    private final CommandCatalog commandRegistry;
    protected List<CompletionResolver> completionResolvers = new ArrayList<>();
    private CommandExecutionHandlerMethodArgumentResolvers argumentResolvers;
    private ConversionService conversionService = new DefaultConversionService();
    private final ShellContext shellContext;
    private final ExitCodeMappings exitCodeMappings;
    private Exception handlingResultNonInt = null;
    private CommandHandlingResult processExceptionNonInt = null;

    private Validator validator = Utils.defaultValidator();
    private List<CommandExceptionResolver> exceptionResolvers = new ArrayList<>();

    public OperatorSupportedShell(ResultHandlerService resultHandlerService, CommandCatalog commandRegistry, Terminal terminal,
                                  ShellContext shellContext, ExitCodeMappings exitCodeMappings) {
        super(resultHandlerService, commandRegistry, terminal, shellContext, exitCodeMappings);
        this.resultHandlerService = resultHandlerService;
        this.commandRegistry = commandRegistry;
        this.terminal = terminal;
        this.shellContext = shellContext;
        this.exitCodeMappings = exitCodeMappings;
    }

    @Autowired
    @Override
    public void setCompletionResolvers(List<CompletionResolver> resolvers) {
        this.completionResolvers = new ArrayList<>(resolvers);
        AnnotationAwareOrderComparator.sort(completionResolvers);
    }

    @Autowired
    @Override
    public void setArgumentResolvers(CommandExecutionHandlerMethodArgumentResolvers argumentResolvers) {
        this.argumentResolvers = argumentResolvers;
    }

    public void setConversionService(ConversionService shellConversionService) {
        this.conversionService = shellConversionService;
    }

    @Autowired(required = false)
    @Override
    public void setValidatorFactory(ValidatorFactory validatorFactory) {
        this.validator = validatorFactory.getValidator();
    }

    @Autowired(required = false)
    @Override
    public void setExceptionResolvers(List<CommandExceptionResolver> exceptionResolvers) {
        this.exceptionResolvers = exceptionResolvers;
    }

    private ExitCodeExceptionProvider exitCodeExceptionProvider;

    @Autowired(required = false)
    @Override
    public void setExitCodeExceptionProvider(ExitCodeExceptionProvider exitCodeExceptionProvider) {
        this.exitCodeExceptionProvider = exitCodeExceptionProvider;
    }

    /**
     * The main program loop: acquire input, try to match it to a command and evaluate. Repeat
     * until a {@link ResultHandler} causes the process to exit or there is no input.
     * <p>
     * This method has public visibility so that it can be invoked by actual commands
     * (<em>e.g.</em> a {@literal script} command).
     * </p>
     */
    @Override
    public void run(InputProvider inputProvider) throws Exception {
        Object result = null;
        while (!(result instanceof ExitRequest)) { // Handles ExitRequest thrown from Quit command
            Input input;
            try {
                input = inputProvider.readInput();
            } catch (Exception e) {
                if (e instanceof ExitRequest) { // Handles ExitRequest thrown from hitting CTRL-C
                    break;
                }
                resultHandlerService.handle(e);
                continue;
            }
            if (input == null) {
                break;
            }

            List<InvokeHandle> invokeHandles = evaluate(input);
            if (invokeHandles == NO_INPUT) {
                continue;
            }
            for (InvokeHandle invokeHandle : invokeHandles) {
                result = invokeHandle.get();
                if (!(result instanceof ExitRequest)) {
                    resultHandlerService.handle(result);
                }

                // throw if not in interactive mode so that boot's exit code feature
                // can contribute exit code. we can't throw when in interactive mode as
                // that would exit a shell
                if (this.shellContext != null &&
                        this.shellContext.getInteractionMode() != InteractionMode.INTERACTIVE) {
                    if (result instanceof CommandExecution.CommandParserExceptionsException) {
                        throw (CommandExecution.CommandParserExceptionsException) result;
                    } else if (result instanceof Exception) {
                        throw (Exception) result;
                    }
                    if (handlingResultNonInt instanceof CommandExecution.CommandParserExceptionsException) {
                        throw handlingResultNonInt;
                    } else if (processExceptionNonInt != null && processExceptionNonInt.exitCode() != null
                            && exitCodeExceptionProvider != null) {
                        throw exitCodeExceptionProvider.apply(null, processExceptionNonInt.exitCode());
                    }
                }

                if (result instanceof ExitRequest) {
                    break;
                }
            }
        }
    }


    /**
     * Evaluate a single "line" of input from the user by trying to map words to a command and
     * arguments.
     *
     * <p>
     * This method does not throw exceptions, it catches them and returns them as a regular
     * result
     * </p>
     */
    @Override
    protected List<InvokeHandle> evaluate(Input input) {
        if (noInput(input)) {
            return NO_INPUT;
        }
        List<String> words = input.words()
                .stream()
                .filter(w -> w.length() > 0)
                .toList();

        List<List<String>> commands = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String word : words) {
            if (word.equals("&&")) {
                commands.add(current);
                current = new ArrayList<>();
            } else {
                current.add(word);
            }
        }
        commands.add(current);
        return commands.stream()
                .map(InvokeHandle::new)
                .collect(Collectors.toList());
    }

    /**
     * Supplier to handle command execution.
     */
    protected class InvokeHandle implements Supplier<Object> {
        private final List<String> words;

        private InvokeHandle(List<String> words) {
            this.words = words;
        }

        @Override
        public Object get() {
            String line = String.join(" ", words).trim();
            String command = findLongestCommand(line, false);

            Map<String, CommandRegistration> registrations = commandRegistry.getRegistrations();
            if (command == null) {
                return new CommandNotFound(words, new HashMap<>(registrations), line);
            }

            log.debug("Evaluate input with line=[{}], command=[{}]", line, command);

            Optional<CommandRegistration> commandRegistration = registrations.values().stream()
                    .filter(r -> {
                        if (r.getCommand().equals(command)) {
                            return true;
                        }
                        for (CommandAlias a : r.getAliases()) {
                            if (a.getCommand().equals(command)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .findFirst();

            if (commandRegistration.isEmpty()) {
                return new CommandNotFound(words, new HashMap<>(registrations), line);
            }

            if (exitCodeMappings != null) {
                List<Function<Throwable, Integer>> mappingFunctions = commandRegistration.get().getExitCode()
                        .getMappingFunctions();
                exitCodeMappings.reset(mappingFunctions);
            }

            Thread commandThread = Thread.currentThread();
            Object sh = Signals.register("INT", commandThread::interrupt);

            CommandExecution execution = CommandExecution.of(
                    argumentResolvers != null ? argumentResolvers.getResolvers() : null, validator, terminal,
                    shellContext, conversionService, commandRegistry);

            List<CommandExceptionResolver> commandExceptionResolvers = commandRegistration.get().getExceptionResolvers();

            Object evaluate = null;
            Exception e = null;
            try {
                evaluate = execution.evaluate(words.toArray(new String[0]));
            } catch (UndeclaredThrowableException ute) {
                if (ute.getCause() instanceof InterruptedException || ute.getCause() instanceof ClosedByInterruptException) {
                    Thread.interrupted(); // to reset interrupted flag
                }
                return ute.getCause();
            } catch (CommandExecutionException e1) {
                if (e1.getCause() instanceof Exception e11) {
                    e = e11;
                } else {
                    return e1.getCause();
                }
            } catch (Exception e2) {
                e = e2;
            } finally {
                Signals.unregister("INT", sh);
            }
            if (e != null && !(e instanceof ExitRequest)) {
                try {
                    CommandHandlingResult processException = processException(commandExceptionResolvers, e);
                    processExceptionNonInt = processException;
                    if (processException.isPresent()) {
                        handlingResultNonInt = e;
                        if (StringUtils.hasText(processException.message())) {
                            terminal.writer().append(processException.message());
                            terminal.writer().flush();
                        }
                    }
                    return null;
                } catch (Exception e1) {
                    e = e1;
                }
            }
            if (e != null) {
                evaluate = e;
            }
            return evaluate;
        }
    }

    private CommandHandlingResult processException(List<CommandExceptionResolver> commandExceptionResolvers, Exception e)
            throws Exception {
        CommandHandlingResult r = null;
        for (CommandExceptionResolver resolver : commandExceptionResolvers) {
            r = resolver.resolve(e);
            if (r != null) {
                break;
            }
        }
        if (r == null) {
            for (CommandExceptionResolver resolver : exceptionResolvers) {
                r = resolver.resolve(e);
                if (r != null) {
                    break;
                }
            }
        }
        if (r != null) {
            return r;
        }
        throw e;
    }

    /**
     * Return true if the parsed input ends up being empty (<em>e.g.</em> hitting ENTER on an
     * empty line or blank space).
     *
     * <p>
     * Also returns true (<em>i.e.</em> ask to ignore) when input starts with {@literal //},
     * which is used for comments.
     * </p>
     */
    private boolean noInput(Input input) {
        return input.words().isEmpty()
                || (input.words().size() == 1 && input.words().get(0).trim().isEmpty())
                || (input.words().iterator().next().matches("\\s*//.*"));
    }

    /**
     * Gather completion proposals given some (incomplete) input the user has already typed
     * in. When and how this method is invoked is implementation specific and decided by the
     * actual user interface.
     */
    @Override
    public List<CompletionProposal> complete(CompletionContext context) {

        String prefix = context.upToCursor();

        List<CompletionProposal> candidates = new ArrayList<>(
                commandsStartingWith(prefix));

        String best = findLongestCommand(prefix, true);
        if (best != null) {
            context = context.drop(best.split(" ").length);
            CommandRegistration registration = commandRegistry.getRegistrations().get(best);
            CompletionContext argsContext = context.commandRegistration(registration);

            for (CompletionResolver resolver : completionResolvers) {
                List<CompletionProposal> resolved = resolver.apply(argsContext);
                candidates.addAll(resolved);
            }

            // Try to complete arguments
            List<CommandOption> matchedArgOptions = new ArrayList<>();
            if (argsContext.getWords().size() > 0 && argsContext.getWordIndex() > 0 && argsContext.getWords().size() > argsContext.getWordIndex()) {
                matchedArgOptions.addAll(matchOptions(registration.getOptions(), argsContext.getWords().get(argsContext.getWordIndex() - 1)));
            }

            List<CompletionProposal> argProposals = matchedArgOptions.stream()
                    .flatMap(o -> {
                        Function<CompletionContext, List<CompletionProposal>> completion = o.getCompletion();
                        if (completion != null) {
                            List<CompletionProposal> apply = completion.apply(argsContext.commandOption(o));
                            return apply.stream();
                        }
                        return Stream.empty();
                    })
                    .toList();

            candidates.addAll(argProposals);
        }
        return candidates;
    }

    private List<CommandOption> matchOptions(List<CommandOption> options, String arg) {
        List<CommandOption> matched = new ArrayList<>();
        String trimmed = StringUtils.trimLeadingCharacter(arg, '-');
        int count = arg.length() - trimmed.length();
        if (count == 1) {
            if (trimmed.length() == 1) {
                Character trimmedChar = trimmed.charAt(0);
                options.stream()
                        .filter(o -> {
                            for (Character sn : o.getShortNames()) {
                                if (trimmedChar.equals(sn)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .findFirst()
                        .ifPresent(matched::add);
            } else if (trimmed.length() > 1) {
                trimmed.chars().mapToObj(i -> (char) i)
                        .forEach(c -> {
                            options.forEach(o -> {
                                for (Character sn : o.getShortNames()) {
                                    if (c.equals(sn)) {
                                        matched.add(o);
                                    }
                                }
                            });
                        });
            }
        } else if (count == 2) {
            options.stream()
                    .filter(o -> {
                        for (String ln : o.getLongNames()) {
                            if (trimmed.equals(ln)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .findFirst()
                    .ifPresent(matched::add);
        }
        return matched;
    }

    private List<CompletionProposal> commandsStartingWith(String prefix) {
        // Workaround for https://github.com/spring-projects/spring-shell/issues/150
        // (sadly, this ties this class to JLine somehow)
        int lastWordStart = prefix.lastIndexOf(' ') + 1;
        return Utils.removeHiddenCommands(commandRegistry.getRegistrations()).entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> {
                    String c = e.getKey();
                    c = c.substring(lastWordStart);
                    return toCommandProposal(c, e.getValue());
                })
                .collect(Collectors.toList());
    }

    private CompletionProposal toCommandProposal(String command, CommandRegistration registration) {
        return new CompletionProposal(command)
                .dontQuote(true)
                .category("Available commands")
                .description(registration.getDescription());
    }

    /**
     * Returns the longest command that can be matched as first word(s) in the given buffer.
     *
     * @return a valid command name, or {@literal null} if none matched
     */
    private String findLongestCommand(String prefix, boolean filterHidden) {
        Map<String, CommandRegistration> registrations = commandRegistry.getRegistrations();
        if (filterHidden) {
            registrations = Utils.removeHiddenCommands(registrations);
        }
        String result = registrations.keySet().stream()
                .filter(command -> prefix.equals(command) || prefix.startsWith(command + " "))
                .reduce("", (c1, c2) -> c1.length() > c2.length() ? c1 : c2);
        return "".equals(result) ? null : result;
    }
}
