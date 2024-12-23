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

package tech.lamprism.lampray.web.controller.user.model;

import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.Role;
import tech.lamprism.lampray.user.details.Birthday;
import tech.lamprism.lampray.user.details.Gender;
import tech.lamprism.lampray.user.details.UserPersonalData;

import java.time.OffsetDateTime;

/**
 * User details VO.
 * <p>
 * For admin to view user details.
 *
 * @author RollW
 */
public record UserDetailsVo(
        long userId,
        Role role,
        String username,
        String email,
        boolean enabled,
        boolean locked,
        boolean canceled,
        OffsetDateTime createTime,
        OffsetDateTime updateTime,
        String nickname,
        String avatar,
        String cover,
        String introduction,
        Gender gender,
        Birthday birthday,
        String website,
        String location
) {


    public static UserDetailsVo of(AttributedUser user,
                                   UserPersonalData data,
                                   String avatar,
                                   String cover) {
        return new UserDetailsVo(
                user.getUserId(),
                user.getRole(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.isLocked(),
                user.isCanceled(),
                user.getCreateTime(),
                user.getUpdateTime(),
                data.getNickname(),
                avatar,
                cover,
                data.getIntroduction(),
                data.getGender(),
                data.getBirthday(),
                data.getWebsite(),
                data.getLocation()
        );
    }

}
