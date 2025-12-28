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

package tech.lamprism.lampray.user;

import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.rollw.common.web.system.SystemResourceKind;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
public class User implements AttributedUserDetails, DataEntity<Long> {
    private final Long id;
    private final String username;
    private final String password;
    private final Role role;
    private final OffsetDateTime registerTime;
    private final OffsetDateTime updateTime;
    private final String email;
    private final String phone;
    private final boolean enabled;
    private final boolean locked;
    private final boolean canceled;

    public User(Long id, String username, String password,
                Role role, OffsetDateTime registerTime,
                OffsetDateTime updateTime, String email, String phone,
                boolean enabled, boolean locked, boolean canceled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.registerTime = registerTime;
        this.updateTime = updateTime;
        this.email = email;
        this.phone = phone;
        this.enabled = enabled;
        this.locked = locked;
        this.canceled = canceled;
    }

    public Long getEntityId() {
        return id;
    }

    @NonNull
    @Override
    public Long getResourceId() {
        return Objects.requireNonNull(id, "Id has not be set.");
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return registerTime;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    @Override
    public long getUserId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Role getRole() {
        return role;
    }

    public OffsetDateTime getRegisterTime() {
        return registerTime;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return registerTime == user.registerTime && enabled == user.enabled && locked == user.locked && canceled == user.canceled && Objects.equals(id, user.id) && Objects.equals(username, user.username) && Objects.equals(password, user.password) && role == user.role && Objects.equals(email, user.email) && Objects.equals(phone, user.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, role, registerTime, email, phone, enabled, locked, canceled);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                ", registerTime=" + registerTime +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", enabled=" + enabled +
                ", locked=" + locked +
                ", canceled=" + canceled +
                '}';
    }

    public static boolean isInvalidId(Long userId) {
        if (userId == null) {
            return true;
        }
        return userId <= 0;
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return UserResourceKind.INSTANCE;
    }

    public static final class Builder implements LongEntityBuilder<User> {
        private Long id = null;
        private String username;
        private String password;
        private Role role = Role.USER;
        private OffsetDateTime registerTime;
        private OffsetDateTime updateTime;
        private String email;
        private String phone;
        private boolean enabled;
        private boolean locked = false;
        private boolean canceled = false;

        public Builder() {

        }

        public Builder(User user) {
            this.id = user.id;
            this.username = user.username;
            this.password = user.password;
            this.role = user.role;
            this.registerTime = user.registerTime;
            this.email = user.email;
            this.phone = user.phone;
            this.enabled = user.enabled;
            this.locked = user.locked;
            this.canceled = user.canceled;
        }

        @Override
        public Builder setEntityId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setRole(Role role) {
            this.role = role;
            return this;
        }

        public Builder setRegisterTime(OffsetDateTime registerTime) {
            this.registerTime = registerTime;
            return this;
        }

        public Builder setUpdateTime(OffsetDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setLocked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public Builder setCanceled(boolean canceled) {
            this.canceled = canceled;
            return this;
        }

        @Override
        public User build() {
            return new User(
                    id, username, password,
                    role, registerTime,
                    updateTime, email, phone, enabled, locked,
                    canceled
            );
        }
    }
}
