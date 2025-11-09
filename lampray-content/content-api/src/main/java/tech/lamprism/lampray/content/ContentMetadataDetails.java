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
import space.lingu.Nullable;
import tech.lamprism.lampray.content.structuraltext.StructuralText;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class ContentMetadataDetails<T extends ContentDetails> implements ContentDetails {
    @NonNull
    private final ContentMetadata contentMetadata;
    @NonNull
    private final T contentDetails;

    public ContentMetadataDetails(@NonNull T contentDetails,
                                  @NonNull ContentMetadata contentMetadata) {
        this.contentMetadata = contentMetadata;
        this.contentDetails = contentDetails;
    }

    @NonNull
    public ContentMetadata getContentMetadata() {
        return contentMetadata;
    }

    @NonNull
    public T getContentDetails() {
        return contentDetails;
    }

    @Override
    public long getUserId() {
        return contentMetadata.getUserId();
    }

    @Override
    public long getContentId() {
        return contentMetadata.getContentId();
    }

    @Override
    @NonNull
    public ContentType getContentType() {
        return contentMetadata.getContentType();
    }

    @NonNull
    public ContentStatus getContentStatus() {
        return contentMetadata.getContentStatus();
    }

    @NonNull
    public ContentAccessAuthType getContentAccessAuthType() {
        return contentMetadata.getContentAccessAuthType();
    }

    @Override
    @Nullable
    public String getTitle() {
        return contentDetails.getTitle();
    }

    @Override
    @Nullable
    public StructuralText getContent() {
        return contentDetails.getContent();
    }

    @Override
    @Nullable
    public ContentDetailsMetadata getMetadata() {
        return contentDetails.getMetadata();
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return contentDetails.getCreateTime();
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
       return contentDetails.getUpdateTime();
    }
}
