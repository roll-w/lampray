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

package tech.lamprism.lampray.content.collection;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.content.ContentAccessAuthType;
import tech.lamprism.lampray.content.ContentDetails;
import tech.lamprism.lampray.content.ContentDetailsMetadata;
import tech.lamprism.lampray.content.ContentMetadata;
import tech.lamprism.lampray.content.ContentStatus;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.content.structuraltext.StructuralText;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class ContentCollectionDetails<T extends ContentDetails> implements ContentDetails {
    @NonNull
    private final ContentMetadata contentMetadata;
    @NonNull
    private final ContentCollectionMetadata contentCollectionMetadata;
    @NonNull
    private final T contentDetails;


    public ContentCollectionDetails(@NonNull T contentDetails,
                                    @NonNull ContentMetadata contentMetadata,
                                    @NonNull ContentCollectionMetadata contentCollectionMetadata) {
        this.contentMetadata = contentMetadata;
        this.contentDetails = contentDetails;
        this.contentCollectionMetadata = contentCollectionMetadata;
    }

    @NonNull
    public ContentMetadata getContentMetadata() {
        return contentMetadata;
    }

    @NonNull
    public T getContentDetails() {
        return contentDetails;
    }

    @NonNull
    public ContentCollectionMetadata getContentCollectionMetadata() {
        return contentCollectionMetadata;
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

    public long getCollectionId() {
        return contentCollectionMetadata.getCollectionId();
    }

    public ContentCollectionType getType() {
        return contentCollectionMetadata.getType();
    }

    public ContentType getInContentType() {
        return contentCollectionMetadata.getInContentType();
    }

    @Nullable
    public Integer getOrder() {
        return contentCollectionMetadata.getOrder();
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
