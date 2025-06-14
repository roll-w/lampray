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

package tech.lamprism.lampray.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author RollW
 */
public class FileCommonStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final ThrowableProxyConverter throwableProxyConverter;

    private final long pid;

    public FileCommonStructuredLogFormatter(ThrowableProxyConverter throwableProxyConverter, Environment environment) {
        this.throwableProxyConverter = throwableProxyConverter;
        this.pid = environment.getProperty("spring.application.pid", Long.class, -1L);
    }

    @Override
    public String format(ILoggingEvent event) {
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(event.getInstant(), ZoneId.systemDefault());
        String time = offsetDateTime.format(FORMATTER);
        String level = String.format("%-5s", event.getLevel().toString());
        String thread = event.getThreadName();
        String logger = shortenLoggerName(event.getLoggerName(), 50);
        String message = event.getFormattedMessage();
        String exception = throwableProxyConverter.convert(event);

        return String.format("%s %s %d [%s] %s : %s%n%s",
                time,
                level,
                pid,
                thread,
                logger,
                message,
                exception
        );
    }

    private String shortenLoggerName(String loggerName, int maxLength) {
        if (loggerName == null || loggerName.length() <= maxLength) {
            return loggerName;
        }

        String[] parts = loggerName.split("\\.");
        StringBuilder shortened = new StringBuilder();

        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                if (shortened.length() > 0) {
                    shortened.append(".");
                }
                shortened.append(parts[i].charAt(0));
            }
            shortened.append(".").append(parts[parts.length - 1]);
        }

        if (shortened.length() > maxLength) {
            return shortened.substring(shortened.length() - maxLength);
        }

        return shortened.toString();
    }
}
