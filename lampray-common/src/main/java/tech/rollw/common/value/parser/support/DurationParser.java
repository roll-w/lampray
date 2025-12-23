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

package tech.rollw.common.value.parser.support;

import tech.rollw.common.value.parser.ValueParseException;
import tech.rollw.common.value.parser.ValueParser;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Parser for {@link Duration} values.
 * Supports both ISO-8601 duration format (e.g., "PT1H30M")
 * and human-readable format (e.g., "1h30m", "90m", "1.5h").
 *
 * @author RollW
 */
public class DurationParser implements ValueParser<String, Duration> {
    private static final DurationParser INSTANCE = new DurationParser();

    private DurationParser() {
    }

    @Override
    public Duration parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValueParseException("Duration value cannot be null or empty");
        }

        String trimmed = value.trim();

        // Try ISO-8601 format first
        if (trimmed.startsWith("P") || trimmed.startsWith("p")) {
            try {
                return Duration.parse(trimmed);
            } catch (DateTimeParseException e) {
                throw new ValueParseException("Invalid ISO-8601 duration format: " + value, e);
            }
        }

        // Try human-readable format without regex
        return parseHumanReadable(trimmed);
    }

    /**
     * Parse human-readable duration format like "1h30m", "90m", "1.5h", "1d 2h 30m 15s".
     */
    private Duration parseHumanReadable(String value) {
        long totalMillis = 0;
        StringBuilder numberBuffer = new StringBuilder();
        StringBuilder unitBuffer = new StringBuilder();
        boolean inNumber = false;
        boolean inUnit = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (Character.isWhitespace(c)) {
                if (inUnit) {
                    // End of unit, process the number-unit pair
                    totalMillis += processNumberUnit(numberBuffer.toString(), unitBuffer.toString(), value);
                    numberBuffer.setLength(0);
                    unitBuffer.setLength(0);
                    inNumber = false;
                    inUnit = false;
                }
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                if (inUnit) {
                    // Transition from unit to new number
                    totalMillis += processNumberUnit(numberBuffer.toString(), unitBuffer.toString(), value);
                    numberBuffer.setLength(0);
                    unitBuffer.setLength(0);
                    inUnit = false;
                }
                numberBuffer.append(c);
                inNumber = true;
            } else if (Character.isLetter(c)) {
                if (!inNumber) {
                    throw new ValueParseException("Invalid duration format: " + value);
                }
                unitBuffer.append(c);
                inUnit = true;
            } else {
                throw new ValueParseException("Invalid character in duration: " + c);
            }
        }

        // Process the last number-unit pair
        if (inUnit) {
            totalMillis += processNumberUnit(numberBuffer.toString(), unitBuffer.toString(), value);
        } else if (inNumber) {
            throw new ValueParseException("Number without unit in duration: " + value);
        }

        if (totalMillis == 0) {
            throw new ValueParseException("Duration must be greater than zero: " + value);
        }

        return Duration.ofMillis(totalMillis);
    }

    /**
     * Process a number-unit pair and return the milliseconds.
     */
    private long processNumberUnit(String numberStr, String unit, String originalValue) {
        if (numberStr.isEmpty() || unit.isEmpty()) {
            throw new ValueParseException("Invalid duration format: " + originalValue);
        }

        double number;
        try {
            number = Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            throw new ValueParseException("Invalid number in duration: " + numberStr, e);
        }

        String unitLower = unit.toLowerCase();
        return switch (unitLower) {
            case "d", "day", "days" -> (long) (number * 24 * 60 * 60 * 1000);
            case "h", "hour", "hours" -> (long) (number * 60 * 60 * 1000);
            case "m", "min", "minute", "minutes" -> (long) (number * 60 * 1000);
            case "s", "sec", "second", "seconds" -> (long) (number * 1000);
            case "ms", "millis", "millisecond", "milliseconds" -> (long) number;
            default -> throw new ValueParseException("Unknown duration unit: " + unit);
        };
    }

    public static DurationParser getInstance() {
        return INSTANCE;
    }
}

