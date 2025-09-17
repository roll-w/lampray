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

package tech.lamprism.lampray.content;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.user.UserIdentity;

/**
 * Represents a content that has not been created.
 *
 * @author RollW
 */
public interface UncreatedContent {
    @NonNull
    ContentType getContentType();

    @NonNull
    UserIdentity getOperator();

    /**
     * Get the title of the content, if exists.
     *
     * @return title
     */
    @Nullable
    String getTitle();

    /**
     * Get the content of the content, if exists.
     *
     * @return content
     */
    @Nullable
    String getContent();

    @Nullable
    ContentDetailsMetadata getMetadata();
}
