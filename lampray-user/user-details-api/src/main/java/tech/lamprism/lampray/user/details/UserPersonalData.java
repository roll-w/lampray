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

package tech.lamprism.lampray.user.details;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import space.lingu.NonNull;
import tech.lamprism.lampray.DataEntity;
import tech.lamprism.lampray.LongEntityBuilder;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserResourceKind;
import tech.rollw.common.web.system.SystemResourceKind;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * User personal data.
 *
 * @author RollW
 */
public final class UserPersonalData implements Serializable, DataEntity<Long> {
    private final long userId;
    private final String nickname;
    private final String avatar;
    private final String cover;
    private final Birthday birthday;
    private final String introduction;
    private final Gender gender;
    private final String location;
    private final String website;
    private final OffsetDateTime updateTime;

    public UserPersonalData(long userId, String nickname, String avatar,
                            String cover, Birthday birthday,
                            String introduction,
                            Gender gender, String location,
                            String website, OffsetDateTime updateTime) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.cover = cover;
        this.birthday = birthday;
        this.introduction = introduction;
        this.gender = gender;
        this.location = location;
        this.website = website;
        this.updateTime = updateTime;
    }

    public long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getCover() {
        return cover;
    }

    public Birthday getBirthday() {
        return birthday;
    }

    public String getIntroduction() {
        return introduction;
    }

    public Gender getGender() {
        return gender;
    }

    public String getLocation() {
        return location;
    }

    public String getWebsite() {
        return website;
    }

    @Override
    public Long getEntityId() {
        return getUserId();
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return NONE_TIME;
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    @NonNull
    @Override
    public SystemResourceKind getSystemResourceKind() {
        return UserResourceKind.INSTANCE;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static UserPersonalData defaultOf(UserIdentity user) {
        return new Builder()
                .setUserId(user.getUserId())
                .setAvatar("user")
                .setBirthday(null)
                .setCover("user-cover")
                .setIntroduction(null)
                .setGender(Gender.PRIVATE)
                .setNickname(user.getUsername())
                .build();
    }


    public static boolean checkNecessaryFields(
            @NonNull UserPersonalData userPersonalData) {
        Preconditions.checkNotNull(userPersonalData);
        return !Strings.isNullOrEmpty(userPersonalData.getNickname());
    }

    public static UserPersonalData replaceWithDefault(UserIdentity userIdentity,
                                                      UserPersonalData userPersonalData) {
        Builder builder = userPersonalData.toBuilder();
        if (Strings.isNullOrEmpty(userPersonalData.getAvatar())) {
            builder.setAvatar("user");
        }
        if (userPersonalData.getNickname() == null) {
            builder.setNickname(userIdentity.getUsername());
        }
        if (Strings.isNullOrEmpty(userPersonalData.getCover())) {
            builder.setCover("user-cover");
        }
        return builder.build();
    }

    public static final class Builder implements LongEntityBuilder<UserPersonalData> {
        private long userId;
        private String nickname;
        private String avatar;
        private String cover;
        private Birthday birthday;
        private String introduction;
        private Gender gender;
        private String location;
        private String website;
        private OffsetDateTime updateTime;

        public Builder() {
        }

        public Builder(UserPersonalData userPersonalData) {
            this.userId = userPersonalData.userId;
            this.nickname = userPersonalData.nickname;
            this.avatar = userPersonalData.avatar;
            this.gender = userPersonalData.gender;
            this.birthday = userPersonalData.birthday;
            this.introduction = userPersonalData.introduction;
            this.location = userPersonalData.location;
            this.website = userPersonalData.website;
        }

        @Override
        public Builder setEntityId(Long id) {
            return this;
        }

        public Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setNickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public Builder setAvatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public Builder setCover(String cover) {
            this.cover = cover;
            return this;
        }

        public Builder setBirthday(Birthday birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder setIntroduction(String introduction) {
            this.introduction = introduction;
            return this;
        }

        public Builder setGender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder setWebsite(String website) {
            this.website = website;
            return this;
        }

        public String getCover() {
            return cover;
        }

        public OffsetDateTime getUpdateTime() {
            return updateTime;
        }

        public Builder setUpdateTime(OffsetDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        @Override
        public UserPersonalData build() {
            return new UserPersonalData(userId, nickname, avatar,
                    cover, birthday, introduction, gender,
                    location, website, updateTime);
        }
    }
}
