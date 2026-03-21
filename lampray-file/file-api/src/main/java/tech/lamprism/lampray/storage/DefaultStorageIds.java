/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.storage;

import java.util.Set;

public final class DefaultStorageIds {
    public static final String DEFAULT_AVATAR_ID = "user";
    public static final String DEFAULT_USER_COVER_ID = "user-cover";
    public static final String DEFAULT_ARTICLE_COVER_ID = "article-cover";
    public static final String DEFAULT_CATEGORY_COVER_ID = "category-cover";
    public static final String DEFAULT_LOGO_ID = "logo";

    private static final Set<String> BUILTIN_IDS = Set.of(
            DEFAULT_AVATAR_ID,
            DEFAULT_USER_COVER_ID,
            DEFAULT_ARTICLE_COVER_ID,
            DEFAULT_CATEGORY_COVER_ID,
            DEFAULT_LOGO_ID
    );

    public static boolean isBuiltin(String storageId) {
        return BUILTIN_IDS.contains(storageId);
    }

    private DefaultStorageIds() {
    }
}
