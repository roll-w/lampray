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

package space.lingu.lamp.web.domain.content.collection;

import space.lingu.Nullable;
import space.lingu.lamp.web.domain.content.Content;
import space.lingu.lamp.web.domain.content.ContentType;
import space.lingu.light.DataColumn;
import space.lingu.light.DataTable;
import space.lingu.light.PrimaryKey;

/**
 * @author RollW
 */
@DataTable(name = "content_collection_metadata")
@SuppressWarnings({"ClassCanBeRecord"})
public class ContentCollectionMetadata {
    /**
     * The order of the top content in the collection.
     */
    public static final int TOP_ORDER = -1;

    @DataColumn(name = "id")
    @PrimaryKey(autoGenerate = true)
    @Nullable
    private final Long id;

    @DataColumn(name = "collection_id")
    private final String collectionId;

    @DataColumn(name = "collection_type")
    private final ContentCollectionType type;

    @DataColumn(name = "content_id")
    private final String contentId;

    @DataColumn(name = "content_type")
    @Nullable
    private final ContentType contentType;

    @DataColumn(name = "order")
    @Nullable
    private final Integer order;

    public ContentCollectionMetadata(@Nullable Long id,
                                     String collectionId,
                                     ContentCollectionType type,
                                     String contentId,
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
    public Long getId() {
        return id;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public ContentCollectionType getType() {
        return type;
    }

    public String getContentId() {
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
                                                      String collectionId,
                                                      ContentCollectionType type) {
        return builder()
                .setId(-1L)
                .setCollectionId(collectionId)
                .setOrder(0)
                .setContentId(content.getContentId())
                .setType(type)
                .setContentType(content.getContentType())
                .build();
    }

    public static final class Builder {
        @Nullable
        private Long id;
        private String collectionId;
        private ContentCollectionType type;
        private String contentId;
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

        public Builder setId(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder setCollectionId(String collectionId) {
            this.collectionId = collectionId;
            return this;
        }

        public Builder setType(ContentCollectionType type) {
            this.type = type;
            return this;
        }

        public Builder setContentId(String contentId) {
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

        public ContentCollectionMetadata build() {
            return new ContentCollectionMetadata(
                    id, collectionId, type, contentId,
                    contentType, order);
        }
    }
}
