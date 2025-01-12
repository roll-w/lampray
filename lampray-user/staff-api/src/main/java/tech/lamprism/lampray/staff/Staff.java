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

package tech.lamprism.lampray.staff;

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author RollW
 */
public class Staff implements DataEntity<Long>, AttributedStaff {
    private final Long id;
    private final long userId;
    private final Set<StaffType> types;
    private final OffsetDateTime createTime;
    private final OffsetDateTime updateTime;

    /**
     * Allow the staff to act as a user (has the same
     * permissions as the user, like posting a blog).
     */
    private final boolean asUser;
    private final boolean deleted;

    public Staff(Long id, long userId,
                 Set<StaffType> types, OffsetDateTime createTime,
                 OffsetDateTime updateTime, boolean asUser,
                 boolean deleted) {
        this.id = id;
        this.userId = userId;
        this.types = types.isEmpty() ? EnumSet.noneOf(StaffType.class) : types;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.asUser = asUser;
        this.deleted = deleted;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public long getStaffId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    @NonNull
    public Set<StaffType> getTypes() {
        return types;
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

    public boolean isAsUser() {
        return asUser;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean hasType(StaffType type) {
        return types.contains(type);
    }

    @NonNull
    @Override
    public Long getResourceId() {
        return id;
    }

    @Override
    @NonNull
    public SystemResourceKind getSystemResourceKind() {
        return StaffResourceKind.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Staff staff)) return false;
        return userId == staff.userId && createTime == staff.createTime && updateTime == staff.updateTime && asUser == staff.asUser && deleted == staff.deleted && Objects.equals(id, staff.id) && Objects.equals(types, staff.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, types, createTime, updateTime, asUser, deleted);
    }

    @Override
    public String toString() {
        return "Staff{" +
                "id=" + id +
                ", userId=" + userId +
                ", types=" + types +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", allowUser=" + asUser +
                ", deleted=" + deleted +
                '}';
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }


    public final static class Builder implements LongEntityBuilder<Staff> {
        private Long id;
        private long userId;
        private Set<StaffType> type;
        private OffsetDateTime createTime;
        private OffsetDateTime updateTime;
        private boolean asUser;
        private boolean deleted;

        public Builder() {
        }

        public Builder(Staff staff) {
            this.id = staff.id;
            this.userId = staff.userId;
            this.type = staff.types;
            this.createTime = staff.createTime;
            this.updateTime = staff.updateTime;
            this.asUser = staff.asUser;
            this.deleted = staff.deleted;
        }

        @Override
        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setTypes(StaffType... types) {
            this.type = EnumSet.of(types[0], types);
            return this;
        }

        public Builder addType(StaffType type) {
            if (this.type == null) {
                this.type = EnumSet.of(type);
                return this;
            }
            this.type.add(type);
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

        public Builder setAsUser(boolean asUser) {
            this.asUser = asUser;
            return this;
        }

        public Builder setDeleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        @Override
        public Staff build() {
            return new Staff(id, userId, type, createTime,
                    updateTime, asUser, deleted);
        }
    }
}
