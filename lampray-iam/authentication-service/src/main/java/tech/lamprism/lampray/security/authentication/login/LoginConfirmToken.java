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

package tech.lamprism.lampray.security.authentication.login;

/**
 * @author RollW
 */
public record LoginConfirmToken(
        String token,
        long userId,
        Long expireTime,
        LoginStrategyType strategyType
) implements LoginVerifiableToken {
    // TODO: refactor LoginConfirmToken
    public static LoginConfirmToken emailToken(String token, long userId, long expireTime) {
        return new LoginConfirmToken(token, userId,
                expireTime, LoginStrategyType.EMAIL_TOKEN);
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public boolean isUsable() {
        return expireTime == null || System.currentTimeMillis() < expireTime;
    }
}
