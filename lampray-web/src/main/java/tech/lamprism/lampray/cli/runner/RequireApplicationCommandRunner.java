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

package tech.lamprism.lampray.cli.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.OverrideSystemPropertiesEnvironment;
import space.lingu.NonNull;
import tech.lamprism.lampray.cli.CommandRunContext;
import tech.lamprism.lampray.cli.CommandRunner;
import tech.lamprism.lampray.cli.common.CommonOptions;
import tech.lamprism.lampray.shell.CommandSpecification;
import tech.lamprism.lampray.shell.SimpleCommandSpecification;
import tech.lamprism.lampray.web.BannerPrintListener;
import tech.lamprism.lampray.web.LamprayEnvKeys;
import tech.lamprism.lampray.web.LampraySystemApplication;
import tech.lamprism.lampray.web.LoggingPostApplicationPreparedEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author RollW
 */
public abstract class RequireApplicationCommandRunner implements CommandRunner {
    private ConfigurableApplicationContext run(String[] args,
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
                        new StartedApplicationListener(onApplicationPrepared));
        SpringApplication application = builder.build();
        return application.run(args);
    }

    @Override
    public final int runCommand(CommandRunContext context) {
        Map<String, Object> overrideProperties = new HashMap<>();
        overrideProperties.put("spring.application.name", "Lampray");
        overrideProperties.put(LamprayEnvKeys.RAW_ARGS, context.getRawArgs());
        setupFixedProperties(overrideProperties);
        onProcessProperties(overrideProperties);
        run(new String[0], determineWebApplicationType(),
                new CommandTask(context, getType()), overrideProperties);
        return 0;
    }

    private WebApplicationType determineWebApplicationType() {
        return switch (getType()) {
            case SERVICE, SERVICE_EXIT -> WebApplicationType.SERVLET;
            case TASK, TASK_KEEP_ALIVE -> WebApplicationType.NONE;
        };
    }

    /**
     * This method is called to run the command with the given context.
     * It should be implemented by subclasses to provide the actual command logic.
     *
     * @param context the command run context
     * @return an integer representing the exit code of the command
     */
    protected abstract int doRunCommand(CommandRunContext context);

    /**
     * Returns the type of this command runner.
     * The type determines how the command is executed in the application context.
     *
     * @return the type of this command runner
     */
    @NonNull
    public abstract Type getType();


    @Override
    public final CommandSpecification getCommandSpecification() {
        SimpleCommandSpecification.Builder builder = SimpleCommandSpecification.builder()
                .setOptions(List.of(CommonOptions.CONFIG));
        onConfigureCommandSpecification(builder);
        return builder.build();
    }

    /**
     * This method is called to process the properties before running the command.
     * It can be overridden by subclasses to add or modify properties.
     *
     * @param properties the properties to be processed
     */
    protected void onProcessProperties(Map<String, Object> properties) {
    }

    /**
     * This method is called to configure the command specification.
     * It can be overridden by subclasses to add or modify command specifications.
     *
     * @param builder the command specification builder
     */
    protected void onConfigureCommandSpecification(SimpleCommandSpecification.Builder builder) {
    }

    private static void setupFixedProperties(Map<String, Object> properties) {
        properties.put("spring.web.resources.add-mappings", false);
        properties.put("spring.output.ansi.enabled", "always");
        properties.put("spring.jackson.mapper.ACCEPT_CASE_INSENSITIVE_ENUMS", true);
        properties.put("web-common.context-initialize-filter", true);
        properties.put("spring.shell.history.enabled", false);
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

    public enum Type {
        /**
         * Run the command in a full web application context, and don't
         * exit the application after running the command.
         */
        SERVICE,

        /**
         * Run the command in a full web application context, and exit the
         * application after running the command.
         */
        SERVICE_EXIT,

        /**
         * Run the command in a limited web application context (don't start
         * the web server), and exit the application after running the command.
         */
        TASK,

        /**
         * Run the command in a limited web application context (don't start
         * the web server), and don't exit the application after running the
         * command.
         */
        TASK_KEEP_ALIVE;

        public boolean exitsAfterRun() {
            return this == SERVICE_EXIT || this == TASK;
        }
    }

    private class CommandTask implements Callable<Integer> {
        private final CommandRunContext context;
        private final Type type;

        public CommandTask(CommandRunContext context, Type type) {
            this.context = context;
            this.type = type;
        }

        @Override
        public Integer call() {
            return doRunCommand(context);
        }

        public Type getType() {
            return type;
        }
    }

    private static class StartedApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
        private static final Logger logger = LoggerFactory.getLogger(StartedApplicationListener.class);

        private final CommandTask onApplicationPrepared;

        public StartedApplicationListener(CommandTask onApplicationPrepared) {
            this.onApplicationPrepared = onApplicationPrepared;
        }

        @Override
        public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
            ConfigurableApplicationContext applicationContext = event.getApplicationContext();
            try {
                int call = onApplicationPrepared.call();
                if (onApplicationPrepared.getType().exitsAfterRun()) {
                    applicationContext.close();
                    System.exit(call);
                    return;
                }
                logger.info("Command executed successfully, application will keep running.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
