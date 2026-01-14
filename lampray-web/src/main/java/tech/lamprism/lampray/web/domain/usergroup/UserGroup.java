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

package tech.lamprism.lampray.web.domain.usergroup;

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.web.domain.systembased.LampSystemResourceKind;
import tech.rollw.common.web.system.SystemResource;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author RollW
 */
public class UserGroup implements DataEntity<Long>, SystemResource<Long> {
    private final Long id;
    private final String name;
    private final String description;
    private final Map<String, String> settings;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    private final boolean deleted;

    public UserGroup(Long id, String name, String description,
                     Map<String, String> settings,
                     OffsetDateTime createTime, OffsetDateTime updateTime, boolean deleted) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.settings = settings;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.deleted = deleted;
    }

    @Override
    public Long getEntityId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getSettings() {
        return settings;
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

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return LampSystemResourceKind.USER_GROUP;
    }

    public static final class Builder implements LongEntityBuilder<UserGroup> {
        private Long id;
        private String name;
        private String description;
        private Map<String, String> settings;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;
        private boolean deleted;

        private Builder() {
            this.settings = new HashMap<>();
        }

        private Builder(UserGroup usergroup) {
            this.id = usergroup.id;
            this.name = usergroup.name;
            this.description = usergroup.description;
            if (usergroup.settings == null) {
                this.settings = new HashMap<>();
            } else {
                this.settings = new HashMap<>(usergroup.settings);
            }
            this.createTime = usergroup.createTime;
            this.updateTime = usergroup.updateTime;
            this.deleted = usergroup.deleted;
        }

        @Override
        public Builder setEntityId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setSettings(Map<String, String> settings) {
            if (settings == null) {
                this.settings = new HashMap<>();
                return this;
            }

            this.settings = settings;
            return this;
        }

        public Builder setSetting(String key, String value) {
            if (this.settings == null) {
                this.settings = new HashMap<>();
            }

            this.settings.put(key, value);
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
        public UserGroup build() {
            return new UserGroup(id, name, description,
                    settings, createTime, updateTime, deleted);
        }
    }
}
