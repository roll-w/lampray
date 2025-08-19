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

package tech.lamprism.lampray.security.token;

import space.lingu.Nullable;

/**
 * Token format types for different transport mechanisms.
 * This enum represents HOW tokens are transmitted, not WHAT they are used for.
 *
 * @author RollW
 */
public enum TokenFormat {
    /**
     * Bearer token format (Authorization: Bearer &lt;token&gt;)
     */
    BEARER("Bearer"),

    /**
     * Basic token format (Authorization: Basic &lt;base64-encoded-credentials&gt;)
     */
    BASIC("Basic");

    private final String value;

    TokenFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Nullable
    public static TokenFormat fromValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (TokenFormat tokenFormat : values()) {
            if (tokenFormat.getValue().equals(value)) {
                return tokenFormat;
            }
        }
        return null;
    }
}
