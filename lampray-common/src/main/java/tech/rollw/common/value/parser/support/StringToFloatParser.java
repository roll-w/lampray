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
 * Parser for converting String to Float.
 *
 * @author RollW
 */
public class StringToFloatParser implements ValueParser<String, Float> {
    private static final StringToFloatParser INSTANCE = new StringToFloatParser();

    private StringToFloatParser() {
    }

    @Override
    public Float parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValueParseException("Float value cannot be null or empty");
        }

        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new ValueParseException("Invalid float format: " + value, e);
        }
    }

    public static StringToFloatParser getInstance() {
        return INSTANCE;
    }
}

