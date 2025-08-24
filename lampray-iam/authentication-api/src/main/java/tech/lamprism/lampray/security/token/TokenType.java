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
 * @author RollW
 */
public enum TokenType {
    /**
     * Access token, used for accessing resources.
     */
    ACCESS,

    /**
     * Refresh token, used for refreshing access tokens.
     */
    REFRESH;


    public String getValue() {
        return name().toLowerCase();
    }

    @Nullable
    public static TokenType fromValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (TokenType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
