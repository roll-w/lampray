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

package tech.lamprism.lampray.security.authentication;

import tech.lamprism.lampray.user.Role;
import tech.lamprism.lampray.user.UserIdentity;

/**
 * @author RollW
 */
public record UserInfoSignature(
        long id,
        String username,
        String signature,
        String email,
        Role role
) implements UserIdentity {
    public static UserInfoSignature from(UserIdentity user, String signature) {
        if (user == null) {
            return null;
        }
        return new UserInfoSignature(
                user.getUserId(),
                user.getUsername(),
                signature,
                user.getEmail(),
                user.getRole()
        );
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
    public String getEmail() {
        return email;
    }

    @Override
    public Role getRole() {
        return role;
    }
}
