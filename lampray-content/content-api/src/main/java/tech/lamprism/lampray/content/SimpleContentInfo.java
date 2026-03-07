/*
 * Copyright (C) 2023 RollW
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

/**
 * @author RollW
 */
public record SimpleContentInfo(
        long userId,
        @NonNull String contentId,
        ContentType contentType
) implements Content {

    public SimpleContentInfo(long userId,
                             @NonNull ContentTrait content) {
        this(userId, content.getContentId(),
                content.getContentType());
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    @NonNull
    public String getContentId() {
        return contentId;
    }

    @NonNull
    @Override
    public ContentType getContentType() {
        return contentType;
    }
}
