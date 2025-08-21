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
import tech.lamprism.lampray.user.UserSignatureProvider;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * @author RollW
 */
public class DelegateTokenSignKeyProvider implements TokenSignKeyProvider {
    private final UserSignatureProvider userSignatureProvider;

    public DelegateTokenSignKeyProvider(UserSignatureProvider userSignatureProvider) {
        this.userSignatureProvider = userSignatureProvider;
    }

    @Override
    @NonNull
    public Key getSignKey(@NonNull TokenSubject tokenSubject) {
        if (tokenSubject.getType() != SubjectType.USER) {
            throw new InvalidTokenException("Only user token subject is supported");
        }
        String signature = userSignatureProvider.getSignature(Long.parseLong(tokenSubject.getId()));
        if (signature == null) {
            throw new InvalidTokenException("User signature not found for user ID: " + tokenSubject.getId());
        }
        return new SecretKeySpec(signature.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
