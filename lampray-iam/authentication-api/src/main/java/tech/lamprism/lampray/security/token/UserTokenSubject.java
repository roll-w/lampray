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

package tech.lamprism.lampray.security.token;

import space.lingu.NonNull;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserProvider;

/**
 * Token subject for user-based tokens.
 *
 * @author RollW
 */
public class UserTokenSubject implements TokenSubject {
    private final UserIdentity userIdentity;

    public UserTokenSubject(UserIdentity userIdentity) {
        this.userIdentity = userIdentity;
    }

    @Override
    @NonNull
    public String getId() {
        return String.valueOf(userIdentity.getUserId());
    }

    @Override
    @NonNull
    public String getName() {
        return userIdentity.getUsername();
    }

    @Override
    @NonNull
    public SubjectType getType() {
        return SubjectType.USER;
    }

    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    @Override
    @NonNull
    public Object getDetail() {
        return userIdentity;
    }

    /**
     * Factory for creating UserTokenSubject instances.
     */
    public static class Factory implements TokenSubject.Factory {
        private final UserProvider userProvider;

        public Factory(UserProvider userProvider) {
            this.userProvider = userProvider;
        }

        @NonNull
        @Override
        public TokenSubject fromSubject(@NonNull String id, @NonNull SubjectType subjectType) {
            try {
                long userId = Long.parseLong(id);
                UserIdentity userIdentity = userProvider.getUser(userId);
                return new UserTokenSubject(userIdentity);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid user ID in subject: " + id, e);
            }
        }

        @Override
        public boolean supports(@NonNull SubjectType type) {
            return type == SubjectType.USER;
        }
    }
}
