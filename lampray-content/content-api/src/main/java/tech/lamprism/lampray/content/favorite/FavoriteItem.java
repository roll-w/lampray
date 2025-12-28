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

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.content.ContentAssociated;
import tech.lamprism.lampray.content.ContentIdentity;
import tech.lamprism.lampray.content.ContentType;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class FavoriteItem implements DataEntity<Long>, ContentAssociated {
    private final Long id;
    private final long groupId;
    private final long userId;
    private final long contentId;
    private final ContentType contentType;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    private final boolean deleted;
    private final ContentIdentity associatedContent;

    public FavoriteItem(Long id, long groupId, long userId,
                        long contentId, ContentType contentType,
                        OffsetDateTime createTime,
                        OffsetDateTime updateTime, boolean deleted) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.contentId = contentId;
        this.contentType = contentType;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.associatedContent = ContentIdentity.of(contentId, contentType);
    }

    @Override
    public Long getEntityId() {
        return id;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getUserId() {
        return userId;
    }

    public long getContentId() {
        return contentId;
    }

    public ContentType getContentType() {
        return contentType;
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public ContentIdentity getAssociatedContent() {
        return associatedContent;
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return FavoriteItemResourceKind.INSTANCE;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements LongEntityBuilder<FavoriteItem> {
        private Long id;
        private long groupId;
        private long userId;
        private long contentId;
        private ContentType contentType;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;
        private boolean deleted;

        private Builder() {
        }

        private Builder(FavoriteItem favoriteitem) {
            this.id = favoriteitem.id;
            this.groupId = favoriteitem.groupId;
            this.userId = favoriteitem.userId;
            this.contentId = favoriteitem.contentId;
            this.contentType = favoriteitem.contentType;
            this.createTime = favoriteitem.createTime;
            this.updateTime = favoriteitem.updateTime;
            this.deleted = favoriteitem.deleted;
        }

        public Builder setEntityId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setGroupId(long groupId) {
            this.groupId = groupId;
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

        public Builder setContentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setCreateTime(OffsetDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder setUpdateTime(OffsetDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public Builder setDeleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public FavoriteItem build() {
            return new FavoriteItem(id, groupId, userId, contentId,
                    contentType, createTime, updateTime, deleted);
        }
    }
}