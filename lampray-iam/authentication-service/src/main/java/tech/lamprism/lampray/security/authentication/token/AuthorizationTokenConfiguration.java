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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.lamprism.lampray.security.token.AuthorizationTokenManagerService;
import tech.lamprism.lampray.security.token.AuthorizationTokenProvider;
import tech.lamprism.lampray.security.token.DelegateTokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.FactoryTokenSubjectProvider;
import tech.lamprism.lampray.security.token.InMemoryRevokeTokenStorage;
import tech.lamprism.lampray.security.token.InheritedAuthorizationScopeHierarchyProvider;
import tech.lamprism.lampray.security.token.RevokeTokenStorage;
import tech.lamprism.lampray.security.token.TokenSubject;
import tech.lamprism.lampray.security.token.TokenSubjectProvider;
import tech.lamprism.lampray.security.token.TokenSubjectScopeProvider;
import tech.lamprism.lampray.security.token.UserTokenSubject;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.user.UserSignatureProvider;

import java.util.List;

/**
 * @author RollW
 */
@Configuration
public class AuthorizationTokenConfiguration {

    @Bean
    public FactoryTokenSubjectProvider factoryTokenSubjectProvider(List<TokenSubject.Factory> factories) {
        return new FactoryTokenSubjectProvider(factories);
    }

    @Bean
    public UserTokenSubject.Factory userTokenSubjectProvider(UserProvider userProvider) {
        return new UserTokenSubject.Factory(userProvider);
    }

    @Bean
    public DelegateTokenSubjectSignKeyProvider delegateTokenSignKeyProvider(UserSignatureProvider userSignatureProvider) {
        return new DelegateTokenSubjectSignKeyProvider(userSignatureProvider);
    }

    @Bean
    public InMemoryRevokeTokenStorage inMemoryTokenRevokeStorage() {
        return new InMemoryRevokeTokenStorage();
    }

    @Bean
    public AuthorizationTokenManagerService authorizationTokenManager(List<AuthorizationTokenProvider> authorizationTokenProviders,
                                                                      RevokeTokenStorage revokeTokenStorage) {
        return new AuthorizationTokenManagerService(authorizationTokenProviders, revokeTokenStorage);
    }


    @Bean
    public InheritedAuthorizationScopeHierarchyProvider inheritedAuthorizationScopeHierarchyProvider(
            TokenSubjectProvider tokenSubjectProvider,
            TokenSubjectScopeProvider tokenSubjectScopeProvider) {
        return new InheritedAuthorizationScopeHierarchyProvider(tokenSubjectProvider, tokenSubjectScopeProvider);
    }

}
