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

package tech.lamprism.lampray.setting;

/**
 * @author RollW
 */
public enum SecretLevel {
    /**
     * No masking, the value is returned as is.
     */
    NONE {
        @Override
        public String maskValue(String value) {
            return value;
        }
    },

    /**
     * Low masking, the center part of the string is masked with '*'.
     */
    LOW {
        @Override
        public String maskValue(String value) {
            if (value == null || value.isEmpty()) {
                return "*";
            }

            // mask the center part of the string
            int length = value.length();
            int maskLength = Math.max(1, length / 2);
            int start = (length - maskLength) / 2;
            StringBuilder maskedValue = new StringBuilder(value);
            for (int i = start; i < start + maskLength; i++) {
                maskedValue.setCharAt(i, '*');
            }
            return maskedValue.toString();
        }
    },

    /**
     * Medium masking, the entire string is masked with '*'.
     */
    MEDIUM {
        @Override
        public String maskValue(String value) {
            if (value == null || value.isEmpty()) {
                return "*";
            }
            return "*".repeat(value.length());
        }
    },

    /**
     * High masking, only 4 '*' characters are returned, regardless of the input string length.
     */
    HIGH {
        @Override
        public String maskValue(String value) {
            return "*".repeat(4);
        }
    };

    /**
     * Masks the given value based on the secret level.
     *
     * @param value the value to be masked
     * @return the masked value
     */
    public abstract String maskValue(String value);

    public String maskValue(ConfigValue<?> value) {
        if (value == null) {
            return null;
        }
        Object v = value.getValue();
        if (v == null) {
            return null;
        }
        return maskValue(String.valueOf(v));
    }
}
