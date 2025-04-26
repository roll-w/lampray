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

package tech.lamprism.lampray.content.favorite;

/**
 * Enum representing the type of favorite group.
 *
 * @author RollW
 */
public enum FavoriteGroupType {
    /**
     * Means this is a default group created by the system when the user
     * is created.
     */
    DEFAULT,

    /**
     * Means this is a group created by the user.
     */
    USER,
}
