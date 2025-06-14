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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import space.lingu.NonNull;
import tech.lamprism.lampray.logging.ColorConverter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Set up logger context after the application environment
 * has been prepared.
 *
 * @author RollW
 */
public class LoggingPostApplicationPreparedEventListener implements
        ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
    public LoggingPostApplicationPreparedEventListener() {
    }

    @Override
    public void onApplicationEvent(
            @NonNull ApplicationEnvironmentPreparedEvent event
    ) {
        LoggerContext loggerContext = getLoggerContext();
        setupConversionRule(loggerContext);
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(
                Logger.ROOT_LOGGER_NAME);

        // Add a no-op appender to avoid the unexpected logging output
        NOPAppender<ILoggingEvent> nopAppender = new NOPAppender<>();
        nopAppender.setName("CONSOLE");
        nopAppender.setContext(loggerContext);
        nopAppender.start();

        Appender<ILoggingEvent> consoleAppender = rootLogger.getAppender("CONSOLE");
        rootLogger.detachAppender(consoleAppender);
        rootLogger.addAppender(nopAppender);

        setupConsoleAppender(consoleAppender, loggerContext);
        rootLogger.detachAppender(nopAppender);
        rootLogger.addAppender(consoleAppender);
    }

    // TODO: support show full logger name by config
    // TODO: support set shown timezone or not by config

    private void setupConsoleAppender(Appender<ILoggingEvent> appender,
                                      LoggerContext loggerContext) {
        // TODO: allow disable console appender by config
        ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) appender;
        if (consoleAppender == null) {
            return;
        }
        // Stop the encoder to avoid a ch.qos.logback.core.encoder.EncoderBase error
        consoleAppender.getEncoder().stop();
        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setContext(loggerContext);

        String pattern = resolve(loggerContext,
                "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}}){faint} "
                        + "%clr(${LOG_LEVEL_PATTERN:-%5p}) "
                        + "%clr(${PID:- }){magenta} %clr([%15.15t]){faint} "
                        + "%clr(${LOG_CORRELATION_PATTERN:-}){faint}%clr(%-50.50logger{49}){cyan} "
                        + "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}");
        patternLayoutEncoder.setPattern(pattern);
        patternLayoutEncoder.setParent(consoleAppender);
        patternLayoutEncoder.setCharset(StandardCharsets.UTF_8);
        patternLayoutEncoder.start();

        consoleAppender.setEncoder(patternLayoutEncoder);
    }

    private String resolve(LoggerContext context, String val) {
        try {
            return OptionHelper.substVars(val, context);
        } catch (ScanException ex) {
            throw new RuntimeException(ex);
        }
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    @SuppressWarnings("unchecked")
    private void setupConversionRule(LoggerContext loggerContext) {
        Map<String, String> ruleRegistry = (Map<String, String>) loggerContext
                .getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (ruleRegistry == null) {
            ruleRegistry = new HashMap<>();
            loggerContext.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);
        }
        ruleRegistry.put("clr", ColorConverter.class.getName());
    }

    @Override
    public int getOrder() {
        // to avoid the LoggingApplicationListener resetting the Logger,
        // set a higher order
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }
}
