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
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class FavoriteGroup implements DataEntity<Long> {
    private final Long id;
    private final String name;
    private final long userId;
    private final FavoriteGroupType type;
    private final boolean isPublic; // add 'is' to avoid conflict with keyword
    private final String description;
    private final String icon;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    private final boolean deleted;

    public FavoriteGroup(Long id, String name, long userId, FavoriteGroupType type,
                         boolean isPublic, String description, String icon,
                         OffsetDateTime createTime, OffsetDateTime updateTime,
                         boolean deleted) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.type = type;
        this.isPublic = isPublic;
        this.description = description;
        this.icon = icon;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.deleted = deleted;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getUserId() {
        return userId;
    }

    public FavoriteGroupType getType() {
        return type;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
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

    public Builder toBuilder() {
        return new Builder(this);
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return FavoriteGroupResourceKind.INSTANCE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public final static class Builder implements LongEntityBuilder<FavoriteGroup> {
        private Long id;
        private String name;
        private long userId;
        private FavoriteGroupType type;
        private boolean isPublic;
        private String description;
        private String icon;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;
        private boolean deleted;

        public Builder() {
        }

        public Builder(FavoriteGroup favoriteGroup) {
            this.id = favoriteGroup.id;
            this.name = favoriteGroup.name;
            this.userId = favoriteGroup.userId;
            this.type = favoriteGroup.type;
            this.isPublic = favoriteGroup.isPublic;
            this.description = favoriteGroup.description;
            this.icon = favoriteGroup.icon;
            this.createTime = favoriteGroup.createTime;
            this.updateTime = favoriteGroup.updateTime;
            this.deleted = favoriteGroup.deleted;
        }

        @Override
        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setType(FavoriteGroupType type) {
            this.type = type;
            return this;
        }

        public Builder setPublic(boolean aPublic) {
            isPublic = aPublic;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setIcon(String icon) {
            this.icon = icon;
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

        @Override
        public FavoriteGroup build() {
            return new FavoriteGroup(id, name, userId, type, isPublic, description,
                    icon, createTime, updateTime, deleted);
        }
    }
}
