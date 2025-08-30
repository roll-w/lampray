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

import org.jetbrains.annotations.NotNull;
import space.lingu.NonNull;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.hierarchy.AuthorizationScopeHierarchyProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author RollW
 */
public class InheritedAuthorizationScopeHierarchyProvider implements AuthorizationScopeHierarchyProvider {
    private final TokenSubjectProvider tokenSubjectProvider;
    private final TokenSubjectScopeProvider tokenSubjectScopeProvider;

    public InheritedAuthorizationScopeHierarchyProvider(TokenSubjectProvider tokenSubjectProvider,
                                                        TokenSubjectScopeProvider tokenSubjectScopeProvider) {
        this.tokenSubjectProvider = tokenSubjectProvider;
        this.tokenSubjectScopeProvider = tokenSubjectScopeProvider;
    }

    @Override
    @NonNull
    public Collection<AuthorizationScope> flattenAuthorizationScopes(
            @NonNull Collection<? extends AuthorizationScope> authorizationScopes) {
        Set<AuthorizationScope> result = new HashSet<>();
        for (AuthorizationScope authorizationScope : authorizationScopes) {
            if (!(authorizationScope instanceof InheritedAuthorizationScope inheritedAuthorizationScope)) {
                throw new IllegalArgumentException("Only support InheritedAuthorizationScope");
            }
            String id = inheritedAuthorizationScope.getId();
            SubjectType type = inheritedAuthorizationScope.getType();
            TokenSubject tokenSubject = tokenSubjectProvider.getTokenSubject(id, type);
            Collection<AuthorizationScope> subjectScopes = tokenSubjectScopeProvider.getAuthorizationScopes(tokenSubject);
            result.addAll(subjectScopes);
        }
        return result;
    }

    @Override
    public boolean supports(@NotNull AuthorizationScope scope) {
        return scope instanceof InheritedAuthorizationScope;
    }
}
