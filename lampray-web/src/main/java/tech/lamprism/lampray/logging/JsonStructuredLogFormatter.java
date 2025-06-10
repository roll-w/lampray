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
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author RollW
 */
public class JsonStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final JsonWriter.PairExtractor<KeyValuePair> keyValuePairExtractor = JsonWriter.PairExtractor.of((pair) -> pair.key,
            (pair) -> pair.value);

    public JsonStructuredLogFormatter(ThrowableProxyConverter throwableProxyConverter,
                                      StructuredLoggingJsonMembersCustomizer<?> customizer) {
        super((members) -> jsonMembers(throwableProxyConverter, members), customizer);
    }

    private static void jsonMembers(ThrowableProxyConverter throwableProxyConverter,
                                    JsonWriter.Members<ILoggingEvent> members) {
        members.add("timestamp", ILoggingEvent::getInstant).as(JsonStructuredLogFormatter::formatInstant);
        members.add("level", ILoggingEvent::getLevel);
        members.add("thread", ILoggingEvent::getThreadName);
        members.add("logger", ILoggingEvent::getLoggerName);
        members.add("message", ILoggingEvent::getFormattedMessage);
        members.addMapEntries(ILoggingEvent::getMDCPropertyMap);
        members.from(ILoggingEvent::getKeyValuePairs)
                .whenNotEmpty()
                .usingExtractedPairs(Iterable::forEach, keyValuePairExtractor);
        members.add("error").whenNotNull(ILoggingEvent::getThrowableProxy).usingMembers((throwableMembers) -> {
            throwableMembers.add("type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
            throwableMembers.add("message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
            throwableMembers.add("stacktrace", throwableProxyConverter::convert);
        });
    }

    private static String formatInstant(Instant instant) {
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
        return FORMATTER.format(offsetDateTime);
    }
}
