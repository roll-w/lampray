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

package tech.lamprism.lampray.security.authentication.registration;

import tech.lamprism.lampray.security.authentication.VerifiableToken;
import tech.lamprism.lampray.user.UserIdentity;
import tech.rollw.common.web.CommonRuntimeException;

/**
 * @author RollW
 */
public interface RegisterTokenProvider {
    VerifiableToken createRegisterToken(UserIdentity userIdentity);

    VerifiableToken getRegisterToken(String token);

    void resendRegisterToken(UserIdentity user);

    void verifyRegisterToken(String token) throws CommonRuntimeException;
}
