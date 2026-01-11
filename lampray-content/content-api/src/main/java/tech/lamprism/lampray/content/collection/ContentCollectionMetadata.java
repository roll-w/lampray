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
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.content.Content;
import tech.lamprism.lampray.content.ContentType;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@SuppressWarnings({"ClassCanBeRecord"})
public class ContentCollectionMetadata implements DataEntity<Long> {
    /**
     * The order of the top content in the collection.
     */
    public static final int TOP_ORDER = -1;

    @Nullable
    private final Long id;

    private final long collectionId;

    private final ContentCollectionType type;

    private final long contentId;

    @Nullable
    private final ContentType contentType;

    @Nullable
    private final Integer order;

    public ContentCollectionMetadata(@Nullable Long id,
                                     long collectionId,
                                     ContentCollectionType type,
                                     long contentId,
                                     @Nullable ContentType contentType,
                                     @Nullable Integer order) {
        this.id = id;
        this.collectionId = collectionId;
        this.type = type;
        this.contentId = contentId;
        this.contentType = contentType;
        this.order = order;
    }

    @Nullable
    @Override
    public Long getEntityId() {
        return id;
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return NONE_TIME;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return NONE_TIME;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public ContentCollectionType getType() {
        return type;
    }

    public long getContentId() {
        return contentId;
    }

    @Nullable
    public ContentType getContentType() {
        return contentType;
    }

    public ContentType getInContentType() {
        if (contentType == null) {
            return type.getContentType();
        }
        return contentType;
    }

    @Nullable
    public Integer getOrder() {
        return order;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ContentCollectionMetadata defaultOf(Content content,
                                                      long collectionId,
                                                      ContentCollectionType type) {
        return builder()
                .setEntityId(-1L)
                .setCollectionId(collectionId)
                .setOrder(0)
                .setContentId(content.getContentId())
                .setType(type)
                .setContentType(content.getContentType())
                .build();
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return ContentCollectionMetadataResourceKind.INSTANCE;
    }

    public static final class Builder implements LongEntityBuilder<ContentCollectionMetadata> {
        @Nullable
        private Long id;
        private long collectionId;
        private ContentCollectionType type;
        private long contentId;
        @Nullable
        private ContentType contentType;
        @Nullable
        private Integer order;

        public Builder() {
        }

        public Builder(ContentCollectionMetadata contentCollectionMetadata) {
            this.id = contentCollectionMetadata.id;
            this.collectionId = contentCollectionMetadata.collectionId;
            this.type = contentCollectionMetadata.type;
            this.contentId = contentCollectionMetadata.contentId;
            this.contentType = contentCollectionMetadata.contentType;
            this.order = contentCollectionMetadata.order;
        }

        @Override
        public Builder setEntityId(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder setCollectionId(long collectionId) {
            this.collectionId = collectionId;
            return this;
        }

        public Builder setType(ContentCollectionType type) {
            this.type = type;
            return this;
        }

        public Builder setContentId(long contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder setContentType(@Nullable ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setOrder(@Nullable Integer order) {
            this.order = order;
            return this;
        }

        @Override
        public ContentCollectionMetadata build() {
            return new ContentCollectionMetadata(
                    id, collectionId, type, contentId,
                    contentType, order);
        }
    }
}
