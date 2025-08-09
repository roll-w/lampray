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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.OverrideSystemPropertiesEnvironment;
import space.lingu.NonNull;
import tech.lamprism.lampray.LampraySystemApplication;
import tech.lamprism.lampray.system.console.CommandSpecification;
import tech.lamprism.lampray.system.console.SimpleCommandSpecification;
import tech.lamprism.lampray.system.console.cli.CommandRunContext;
import tech.lamprism.lampray.system.console.cli.CommandRunner;
import tech.lamprism.lampray.system.console.cli.CommonOptions;
import tech.lamprism.lampray.web.BannerPrintListener;
import tech.lamprism.lampray.web.LamprayEnvKeys;
import tech.lamprism.lampray.web.LoggingPostApplicationPreparedEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for command runners that require Spring application context.
 *
 * <p>This class provides a foundation for commands that need access to Spring beans,
 * services, and other application components. It handles the complexity of bootstrapping
 * a Spring application context with appropriate configuration for CLI execution.</p>
 *
 * <p>The class supports different execution types:</p>
 * <ul>
 *   <li>SERVICE - Full web context, keeps running (for server commands)</li>
 *   <li>SERVICE_EXIT - Full web context, exits after completion</li>
 *   <li>TASK - Limited context without web server, exits after completion (for batch operations)</li>
 *   <li>TASK_KEEP_ALIVE - Limited context without web server, keeps running</li>
 * </ul>
 *
 * <p>Subclasses must implement:</p>
 * <ul>
 *   <li>{@link #doRunCommand(CommandRunContext, ApplicationContext)} - The actual command logic</li>
 *   <li>{@link #getType()} - The execution type determining context behavior</li>
 * </ul>
 *
 * <p>Optional hooks for customization:</p>
 * <ul>
 *   <li>{@link #onProcessProperties(Map)} - Modify Spring properties before context creation</li>
 *   <li>{@link #onConfigureCommandSpecification(SimpleCommandSpecification.Builder)} - Customize command specification</li>
 * </ul>
 *
 * @author RollW
 */
public abstract class AbstractApplicationContextCommandRunner implements CommandRunner {
    /**
     * Execute the command with full Spring application context support.
     *
     * <p>This method handles the complete lifecycle of creating a Spring application context,
     * injecting dependencies, and executing the command logic. The context type and behavior
     * are determined by the {@link #getType()} method implementation.</p>
     *
     * <p>The method automatically configures Spring properties, including ANSI output support.</p>
     */
    @Override
    public final int runCommand(CommandRunContext context) {
        Map<String, Object> overrideProperties = new HashMap<>();
        overrideProperties.put("spring.application.name", "Lampray");
        String configPath = Objects.requireNonNullElse(context.getArguments().get("--config"), "")
                .toString();
        overrideProperties.put(LamprayEnvKeys.RAW_ARGS, context.getRawArgs());
        overrideProperties.put(LamprayEnvKeys.CONFIG_PATH, configPath);
        setupFixedProperties(overrideProperties);
        onProcessProperties(overrideProperties);
        startApplication(new String[0], determineWebApplicationType(),
                new CommandTask(context, getType()), overrideProperties);
        return 0;
    }

    private void startApplication(String[] args,
                                  WebApplicationType webApplication,
                                  CommandTask onApplicationPrepared,
                                  Map<String, Object> additionalProperties) {
        ConfigurableEnvironment environment = new OverrideSystemPropertiesEnvironment(
                false,
                false
        );
        SpringApplicationBuilder builder = new SpringApplicationBuilder(LampraySystemApplication.class)
                .environment(environment)
                .bannerMode(Banner.Mode.OFF)
                .properties(additionalProperties)
                .web(webApplication)
                .listeners(
                        new LoggingPostApplicationPreparedEventListener(),
                        new BannerPrintListener(),
                        new ApplicationStartedListener(onApplicationPrepared));
        SpringApplication application = builder.build();
        application.run(args);
    }

    private WebApplicationType determineWebApplicationType() {
        return switch (getType()) {
            case SERVICE, SERVICE_EXIT -> WebApplicationType.SERVLET;
            case TASK, TASK_KEEP_ALIVE -> WebApplicationType.NONE;
        };
    }

    /**
     * Execute the actual command logic with access to Spring application context.
     *
     * <p>This method is called after the Spring application context has been fully
     * initialized and all beans are available for dependency injection. Implementations
     * should contain the core business logic for the command.</p>
     *
     * <p>Exception handling is managed by the framework - uncaught exceptions will be
     * logged and result in appropriate exit codes.</p>
     *
     * @param context the command execution context with arguments and output streams
     * @param applicationContext the fully initialized Spring application context
     * @return exit code following Unix conventions (0 for success, non-zero for failure)
     */
    protected abstract int doRunCommand(CommandRunContext context, ApplicationContext applicationContext);

    /**
     * Determine the execution type for this command runner.
     *
     * <p>The type controls how the Spring application context is configured and
     * whether the application continues running after command completion:</p>
     *
     * <ul>
     *   <li>{@link Type#SERVICE} - Full web application with embedded server, keeps running</li>
     *   <li>{@link Type#SERVICE_EXIT} - Full web application, exits after command completion</li>
     *   <li>{@link Type#TASK} - Non-web context for batch operations, exits after completion</li>
     *   <li>{@link Type#TASK_KEEP_ALIVE} - Non-web context, keeps running for monitoring</li>
     * </ul>
     *
     * @return the execution type that determines context configuration and lifecycle
     */
    @NonNull
    public abstract Type getType();


    @Override
    public final CommandSpecification getCommandSpecification() {
        SimpleCommandSpecification.Builder builder = SimpleCommandSpecification.builder()
                .setGroup("Application Commands")
                .setOptions(List.of(CommonOptions.CONFIG));
        onConfigureCommandSpecification(builder);
        return builder.build();
    }

    /**
     * Hook for customizing Spring application properties before context creation.
     *
     * <p>This method is called during application context initialization and allows
     * subclasses to add or override Spring configuration properties. Common use cases
     * include setting database connections, enabling specific profiles, or configuring
     * external service connections.</p>
     *
     * <p>Properties set here take precedence over default configuration but may be
     * overridden by command-line arguments or environment variables.</p>
     *
     * @param properties mutable map of Spring application properties that will be
     *                   applied to the application context
     */
    protected void onProcessProperties(Map<String, Object> properties) {
    }

    /**
     * Hook for customizing the command specification with additional options or metadata.
     *
     * <p>This method allows subclasses to define their command structure, including
     * command names, descriptions, options, and help text. The builder is pre-configured
     * with common options like {@code --config}.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * protected void onConfigureCommandSpecification(SimpleCommandSpecification.Builder builder) {
     *     builder.setNames("my-command")
     *            .setDescription("Execute my custom command")
     *            .addOption(SimpleCommandOption.builder()
     *                .setNames("--input", "-i")
     *                .setDescription("Input file path")
     *                .setType(String.class)
     *                .build());
     * }
     * }</pre>
     *
     * @param builder the command specification builder for configuring command metadata
     */
    protected void onConfigureCommandSpecification(SimpleCommandSpecification.Builder builder) {
    }

    private static void setupFixedProperties(Map<String, Object> properties) {
        properties.put("spring.web.resources.add-mappings", false);
        properties.put("spring.output.ansi.enabled", "always");
        properties.put("spring.jackson.mapper.ACCEPT_CASE_INSENSITIVE_ENUMS", true);
        properties.put("web-common.context-initialize-filter", true);
        properties.put("spring.config.location", "");
        properties.put("spring.messages.basename", "messages");
        properties.put("spring.jmx.enabled", false);
        //properties.put("spring.jpa.show-sql", true);
        properties.put("spring.jpa.properties.hibernate.globally_quoted_identifiers", "true");
        properties.put("spring.jpa.hibernate.ddl-auto", "update");

        // Disable all actuator endpoints
        properties.put("management.endpoints.web.exposure.exclude", "*");
        properties.put("management.endpoints.jmx.exposure.exclude", "*");
    }

    /**
     * Enumeration of command execution types that determine Spring context behavior.
     *
     * <p>Each type configures the Spring application context differently to optimize
     * for the specific use case, from full web applications to lightweight batch tasks.</p>
     */
    public enum Type {
        /**
         * Full web application context with embedded server, continues running.
         *
         * <p>Use for server commands that need to handle HTTP requests and
         * should remain active until manually stopped.</p>
         */
        SERVICE,

        /**
         * Full web application context with embedded server, exits after completion.
         *
         * <p>Use for setup or maintenance commands that need web components
         * but should terminate after executing their logic.</p>
         */
        SERVICE_EXIT,

        /**
         * Limited application context without web server, exits after completion.
         *
         * <p>Use for batch operations, data processing, or maintenance tasks
         * that need database access and Spring services but no web interface.</p>
         */
        TASK,

        /**
         * Limited application context without web server, continues running.
         *
         * <p>Use for monitoring, background processing, or daemon-like tasks
         * that need Spring services but should run continuously without web interface.</p>
         */
        TASK_KEEP_ALIVE;

        /**
         * Check if this execution type terminates the application after command completion.
         *
         * @return true if the application should exit after command execution
         */
        public boolean exitsAfterRun() {
            return this == SERVICE_EXIT || this == TASK;
        }
    }

    private class CommandTask {
        private final CommandRunContext context;
        private final Type type;

        public CommandTask(CommandRunContext context, Type type) {
            this.context = context;
            this.type = type;
        }

        public int execute(ApplicationContext applicationContext) {
            try {
                return doRunCommand(context, applicationContext);
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(LampraySystemApplication.class);
                logger.error("Error occurred while running command: {}", context.getRawArgs(), e);
                return -1; // Indicate an error occurred
            }
        }

        public Type getType() {
            return type;
        }
    }

    private static class ApplicationStartedListener implements ApplicationListener<ApplicationStartedEvent> {
        private static final Logger logger = LoggerFactory.getLogger(ApplicationStartedListener.class);

        private final CommandTask task;

        public ApplicationStartedListener(CommandTask task) {
            this.task = task;
        }

        @Override
        public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
            ConfigurableApplicationContext applicationContext = event.getApplicationContext();
            try {
                int code = task.execute(applicationContext);
                if (task.getType().exitsAfterRun()) {
                    logger.debug("Application will exit after running command since the command type is {}.",
                            task.getType());
                    applicationContext.close();
                    System.exit(code);
                    return;
                }
                logger.debug("Command executed for {} with exit code: {}, application will keep running.",
                        task.getType(), code);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
