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

package tech.lamprism.lampray.web.domain.like;

import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.content.ContentType;
import tech.lamprism.lampray.web.domain.systembased.LampSystemResourceKind;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public final class UserLike implements DataEntity<Long> {
    @Nullable
    private final Long id;
    private final long userId;
    private final OffsetDateTime time;
    private final String contentId;
    private final ContentType contentType;
    private final LikeType likeType;

    public UserLike(@Nullable Long id,
                    long userId,
                    OffsetDateTime time,
                    String contentId,
                    ContentType contentType,
                    LikeType likeType) {
        this.id = id;
        this.userId = userId;
        this.time = time;
        this.contentId = contentId;
        this.contentType = contentType;
        this.likeType = likeType;
    }

    @Nullable
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
        return time;
    }

    public long getUserId() {
        return userId;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public String getContentId() {
        return contentId;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public LikeType getLikeType() {
        return likeType;
    }

    @Override
    @NonNull
    public SystemResourceKind getSystemResourceKind() {
        return LampSystemResourceKind.USER_LIKE;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements LongEntityBuilder<UserLike> {
        private Long id;
        private long userId;
        private OffsetDateTime time;
        private String contentId;
        private ContentType contentType;
        private LikeType likeType;

        public Builder() {
        }

        public Builder(UserLike userLike) {
            this.id = userLike.id;
            this.userId = userLike.userId;
            this.time = userLike.time;
            this.contentId = userLike.contentId;
            this.contentType = userLike.contentType;
            this.likeType = userLike.likeType;
        }

        public Builder setEntityId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setTime(OffsetDateTime time) {
            this.time = time;
            return this;
        }

        public Builder setContentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder setContentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setLikeType(LikeType likeType) {
            this.likeType = likeType;
            return this;
        }

        public UserLike build() {
            return new UserLike(id, userId, time, contentId, contentType, likeType);
        }
    }
}
