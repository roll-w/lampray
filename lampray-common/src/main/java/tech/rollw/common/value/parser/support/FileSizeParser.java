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


import tech.rollw.common.value.FileSize;
import tech.rollw.common.value.parser.ValueParseException;
import tech.rollw.common.value.parser.ValueParser;

/**
 * Parser for {@link FileSize} values.
 * Supports formats like "10MB", "1.5GB", "100 KB", etc.
 *
 * @author RollW
 */
public class FileSizeParser implements ValueParser<String, FileSize> {
    public FileSizeParser() {
    }

    @Override
    public FileSize parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValueParseException("File size value cannot be null or empty");
        }

        String trimmed = value.trim();

        StringBuilder numberBuffer = new StringBuilder();
        StringBuilder unitBuffer = new StringBuilder();
        boolean inNumber = true;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (Character.isWhitespace(c)) {
                if (!numberBuffer.isEmpty()) {
                    inNumber = false;
                }
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                if (!inNumber && !unitBuffer.isEmpty()) {
                    throw new ValueParseException("Invalid file size format: " + value);
                }
                numberBuffer.append(c);
            } else if (Character.isLetter(c)) {
                inNumber = false;
                unitBuffer.append(c);
            } else {
                throw new ValueParseException("Invalid character in file size: " + c);
            }
        }

        if (numberBuffer.isEmpty()) {
            throw new ValueParseException("No number found in file size: " + value);
        }

        double number;
        try {
            number = Double.parseDouble(numberBuffer.toString());
        } catch (NumberFormatException e) {
            throw new ValueParseException("Invalid number in file size: " + numberBuffer, e);
        }

        String unit = unitBuffer.toString();

        if (unit.isEmpty() || unit.equalsIgnoreCase("B")) {
            return FileSize.ofBytes((long) number);
        }

        String unitUpper = unit.toUpperCase();
        FileSize.Unit fileSizeUnit = FileSize.Unit.fromString(unitUpper);

        return FileSize.of(number, fileSizeUnit);
    }
}

