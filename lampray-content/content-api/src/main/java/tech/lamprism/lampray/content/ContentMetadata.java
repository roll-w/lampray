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
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@SuppressWarnings({"ClassCanBeRecord"})
public class ContentMetadata implements DataEntity<Long>, ContentTrait {
    // only maintains the metadata of the content.
    private final Long id;
    private final long userId;
    private final long contentId;

    @NonNull
    private final ContentType contentType;

    @NonNull
    private final ContentStatus contentStatus;

    @NonNull
    private final ContentAccessAuthType contentAccessAuthType;

    public ContentMetadata(Long id, long userId,
                           long contentId,
                           @NonNull ContentType contentType,
                           @NonNull ContentStatus contentStatus,
                           @NonNull ContentAccessAuthType contentAccessAuthType) {
        this.id = id;
        this.userId = userId;
        this.contentId = contentId;
        this.contentType = contentType;
        this.contentStatus = contentStatus;
        this.contentAccessAuthType = contentAccessAuthType;
        checkForNull();
    }

    @SuppressWarnings("all")
    private void checkForNull() {
        if (contentType == null) {
            throw new NullPointerException("contentType is null");
        }
        if (contentStatus == null) {
            throw new NullPointerException("contentStatus is null");
        }
        if (contentAccessAuthType == null) {
            throw new NullPointerException("contentAccessAuthType is null");
        }
    }

    @Override
    public Long getEntityId() {
        return id;
    }

    /**
     * ContentMetadata does not have create time.
     *
     * @return {@code NONE_TIME} only.
     */
    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return NONE_TIME;
    }

    /**
     * ContentMetadata does not have update time.
     *
     * @return {@code NONE_TIME} only.
     */
    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return NONE_TIME;
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public long getContentId() {
        return contentId;
    }

    @NonNull
    @Override
    public ContentType getContentType() {
        return contentType;
    }

    @NonNull
    public ContentStatus getContentStatus() {
        return contentStatus;
    }

    @NonNull
    public ContentAccessAuthType getContentAccessAuthType() {
        return contentAccessAuthType;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return ContentMetadataResourceKind.INSTANCE;
    }

    public static class Builder implements LongEntityBuilder<ContentMetadata> {
        private Long id;
        private long userId;
        private long contentId;
        private ContentType contentType;
        private ContentStatus contentStatus;
        private ContentAccessAuthType contentAccessAuthType;

        public Builder() {
        }

        public Builder(ContentMetadata contentMetadata) {
            this.id = contentMetadata.id;
            this.userId = contentMetadata.userId;
            this.contentId = contentMetadata.contentId;
            this.contentType = contentMetadata.contentType;
            this.contentStatus = contentMetadata.contentStatus;
            this.contentAccessAuthType = contentMetadata.contentAccessAuthType;
        }

        @Override
        public Builder setEntityId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setContentId(long contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder setContentType(@NonNull ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setContentStatus(@NonNull ContentStatus contentStatus) {
            this.contentStatus = contentStatus;
            return this;
        }

        public Builder setContentAccessAuthType(@NonNull ContentAccessAuthType contentAccessAuthType) {
            this.contentAccessAuthType = contentAccessAuthType;
            return this;
        }

        @Override
        public ContentMetadata build() {
            return new ContentMetadata(id, userId, contentId,
                    contentType, contentStatus, contentAccessAuthType);
        }
    }
}
