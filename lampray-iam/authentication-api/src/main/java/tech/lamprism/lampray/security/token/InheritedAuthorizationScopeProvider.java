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
import tech.lamprism.lampray.security.authorization.AuthorizationScopeProvider;

import java.util.List;

/**
 * @author RollW
 */
public class InheritedAuthorizationScopeProvider implements AuthorizationScopeProvider {
    @NonNull
    @Override
    public AuthorizationScope findScope(@NonNull String scope) {
        InheritedAuthorizationScope parsed = InheritedAuthorizationScope.parse(scope);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid inherited scope: " + scope);
        }
        return parsed;
    }

    @Override
    @NonNull
    public List<AuthorizationScope> findScopes(@NonNull List<String> scopes) {
        return scopes.stream()
                .map(this::findScope)
                .toList();
    }

    @Override
    public boolean supports(@NotNull String scope) {
        return scope.startsWith(InheritedAuthorizationScope.PREFIX);
    }
}
