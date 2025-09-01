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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import space.lingu.Nullable;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.UserErrorCode;

/**
 * @author RollW
 */
@Component
public class PasswordLoginStrategy implements LoginStrategy {
    private static final Logger logger = LoggerFactory.getLogger(PasswordLoginStrategy.class);

    private final PasswordEncoder passwordEncoder;

    public PasswordLoginStrategy(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginVerifiableToken createToken(AttributedUserDetails user) throws LoginTokenException {
        return new LoginPasswordToken(null, user.getUserId());
    }

    @NonNull
    @Override
    public ErrorCode verify(String token, @NonNull AttributedUserDetails user) {
        if (!passwordEncoder.matches(token, user.getPassword())) {
            return UserErrorCode.ERROR_PASSWORD_NOT_CORRECT;
        }

        return AuthErrorCode.SUCCESS;
    }

    @Override
    public void sendToken(LoginVerifiableToken token, AttributedUserDetails user, @Nullable Options requestInfo)
            throws LoginTokenException {
        // no need to send token
    }

    @Override
    public LoginStrategyType getStrategyType() {
        return LoginStrategyType.PASSWORD;
    }
}
