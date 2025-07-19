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

package tech.lamprism.lampray.web;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import tech.lamprism.lampray.logging.FileCommonStructuredLogFormatter;
import tech.lamprism.lampray.logging.JsonStructuredLogFormatter;
import tech.lamprism.lampray.setting.ConfigProvider;
import tech.lamprism.lampray.setting.ReadonlyConfigProvider;
import tech.lamprism.lampray.setting.TomlConfigReader;
import tech.lamprism.lampray.web.common.keys.LoggingConfigKeys;
import tech.lamprism.lampray.web.common.keys.ServerConfigKeys;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
public class LamprayEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SETUP_PROPERTIES = "lampraySetupProperties";

    private final Log logger;

    public LamprayEnvironmentPostProcessor(DeferredLogFactory deferredLogFactory) {
        this.logger = deferredLogFactory.getLog(LamprayEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        String[] rawArgs = environment.getProperty(LamprayEnvKeys.RAW_ARGS, String[].class);
        logger.debug("Passed rawArgs: " + Arrays.toString(rawArgs));
        if (rawArgs == null) {
            throw new IllegalStateException("Raw arguments not found.");
        }
        ConfigProvider localProvider = createLocalProvider(rawArgs);
        Map<String, Object> setupProperties = new HashMap<>();
        setPropertiesNeedsInStartup(setupProperties, localProvider);

        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource(
                SETUP_PROPERTIES, setupProperties
        ));
    }

    private void setPropertiesNeedsInStartup(Map<String, Object> properties, ConfigProvider localProvider) {
        properties.put("server.port", localProvider.get(ServerConfigKeys.HTTP_PORT));
        properties.put(LamprayEnvKeys.LOCAL_CONFIG_LOADER, localProvider);
        try {
            setupLoggingProperties(properties, localProvider);
        } catch (RuntimeException e) {
            throw new ServerInitializeException(new ServerInitializeException.Detail(
                    "Failed to setup logging configs, due to: " + e.getMessage(),
                    "Check the logging configs in your local config file."
            ), e);
        }
    }

    private void setupLoggingProperties(Map<String, Object> properties,
                                        ConfigProvider localProvider) {
        Set<String> loggingLevel = localProvider.get(LoggingConfigKeys.LOGGING_LEVEL);
        Map<String, String> loggingLevels = LoggingConfigKeys.parseLoggingLevel(loggingLevel);
        for (Map.Entry<String, String> entry : loggingLevels.entrySet()) {
            properties.put("logging.level." + entry.getKey(), entry.getValue());
        }

        String loggingFilePath = localProvider.get(LoggingConfigKeys.LOGGING_FILE_PATH);
        if (Objects.equals(loggingFilePath, LoggingConfigKeys.LOGGING_PATH_CONSOLE)) {
            logger.info("Set logging to console, no file logging.");
            return;
        }

        // set file logging
        properties.put("logging.file.path", loggingFilePath);
        properties.put("logging.file.name", "${logging.file.path}/lampray.log");
        properties.put("logging.logback.rollingpolicy.max-file-size",
                localProvider.get(LoggingConfigKeys.LOGGING_FILE_MAX_SIZE));
        properties.put("logging.logback.rollingpolicy.max-history",
                localProvider.get(LoggingConfigKeys.LOGGING_FILE_MAX_HISTORY));
        properties.put("logging.logback.rollingpolicy.total-size-cap",
                localProvider.get(LoggingConfigKeys.LOGGING_FILE_TOTAL_SIZE_CAP));
        properties.put("logging.logback.rollingpolicy.file-name-pattern",
                "${logging.file.path}/lampray-%d{yyyy-MM-dd}.%i.log");

        String format = localProvider.get(LoggingConfigKeys.LOGGING_FILE_FORMAT);

        if (StringUtils.equalsIgnoreCase(format, LoggingConfigKeys.LOGGING_FORMAT_JSON)) {
            properties.put("logging.structured.format.file", JsonStructuredLogFormatter.class.getCanonicalName());
        } else if (StringUtils.equalsIgnoreCase(format, LoggingConfigKeys.LOGGING_FORMAT_TEXT)) {
            properties.put("logging.structured.format.file", FileCommonStructuredLogFormatter.class.getCanonicalName());
        } else {
            throw new ServerInitializeException(new ServerInitializeException.Detail(
                    "Invalid logging file format: " + format,
                    "Check the logging file format in your local config file."
            ));
        }
    }

    private static ReadonlyConfigProvider createLocalProvider(String[] args) {
        String path = getConfigPath(args);
        boolean allowFail = Strings.isNullOrEmpty(path);

        try {
            return new ReadonlyConfigProvider(
                    TomlConfigReader.loadConfig(
                            LampraySystemApplication.class,
                            path, allowFail
                    )
            );
        } catch (FileNotFoundException e) {
            throw new ServerInitializeException(new ServerInitializeException.Detail(
                    "Failed to find local config file: " + e.getMessage(),
                    "Check the file path of the local config file."
            ), e);
        } catch (IOException e) {
            throw new ServerInitializeException(new ServerInitializeException.Detail(
                    "Failed to load local config file, due to: " + e.getMessage(),
                    "Check the file path and file content."
            ), e);
        }
    }

    public static final String CONFIG_PATH = "--config";
    public static final String SHORTAGE_CONFIG_PATH = "-c";

    private static String getConfigPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(CONFIG_PATH) || args[i].equals(SHORTAGE_CONFIG_PATH)) {
                if (i + 1 >= args.length) {
                    throw configPathNotSpecified(args[i]);
                }
                return args[i + 1];
            }
        }
        return null;
    }

    private static ServerInitializeException configPathNotSpecified(String argName) {
        ServerInitializeException.Detail detail = new ServerInitializeException.Detail(
                "You are using config file path option: '" + argName +
                        "', but not specify the config file path.",
                "Use '" + argName + " <path>' to specify the config file path."
        );
        return new ServerInitializeException(detail);
    }
}
