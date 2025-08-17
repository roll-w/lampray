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

package tech.lamprism.lampray.security.authentication.token;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.security.authorization.AuthorizationScopeProvider;
import tech.lamprism.lampray.security.token.TokenSubjectProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.setting.ConfigReader;

/**
 * @author RollW
 */
@Service
public class AccessJwtAuthorizationTokenProvider extends AbstractJwtAuthorizationTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(AccessJwtAuthorizationTokenProvider.class);

    public AccessJwtAuthorizationTokenProvider(ConfigReader configReader, AuthorizationScopeProvider authorizationScopeProvider,
                                               TokenSubjectProvider tokenSubjectProvider) {
        super(configReader, authorizationScopeProvider, tokenSubjectProvider);
    }

    @Override
    public boolean supports(@NotNull TokenType tokenType) {
        return tokenType == TokenType.ACCESS;
    }
}
