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

/**
 * Parser for converting String to Boolean.
 * Accepts: "true", "false", "yes", "no", "1", "0" (case-insensitive).
 *
 * @author RollW
 */
public class StringToBooleanParser implements ValueParser<String, Boolean> {
    private static final StringToBooleanParser INSTANCE = new StringToBooleanParser();

    private StringToBooleanParser() {
    }

    @Override
    public Boolean parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValueParseException("Boolean value cannot be null or empty");
        }

        String trimmedLower = value.trim().toLowerCase();
        return switch (trimmedLower) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> throw new ValueParseException("Invalid boolean format: " + value);
        };
    }

    public static StringToBooleanParser getInstance() {
        return INSTANCE;
    }
}

