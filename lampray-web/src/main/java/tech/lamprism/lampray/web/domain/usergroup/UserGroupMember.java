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
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public class UserGroupMember implements DataEntity<Long> {
    private final Long id;
    private final long groupId;
    private final long userId;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;
    private final boolean deleted;


    public UserGroupMember(Long id, long groupId, long userId,
                           OffsetDateTime createTime, OffsetDateTime updateTime,
                           boolean deleted) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.deleted = deleted;
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
        return LampSystemResourceKind.USER_GROUP_MEMBER;
    }

    public static final class Builder implements LongEntityBuilder<UserGroupMember> {
        private Long id;
        private long groupId;
        private long userId;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;
        private boolean deleted;

        private Builder() {
        }

        private Builder(UserGroupMember usergroupmember) {
            this.id = usergroupmember.id;
            this.groupId = usergroupmember.groupId;
            this.userId = usergroupmember.userId;
            this.createTime = usergroupmember.createTime;
            this.updateTime = usergroupmember.updateTime;
            this.deleted = usergroupmember.deleted;
        }

        @Override
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
        public UserGroupMember build() {
            return new UserGroupMember(
                    id, groupId, userId,
                    createTime, updateTime, deleted
            );
        }
    }
}
