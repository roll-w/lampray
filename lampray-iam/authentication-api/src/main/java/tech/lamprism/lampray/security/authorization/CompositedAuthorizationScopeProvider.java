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

package tech.lamprism.lampray.security.authorization;

import org.jetbrains.annotations.NotNull;
import space.lingu.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
public class CompositedAuthorizationScopeProvider implements AuthorizationScopeProvider {
    private final List<AuthorizationScopeProvider> providers;

    public CompositedAuthorizationScopeProvider(List<AuthorizationScopeProvider> providers) {
        this.providers = providers;
    }

    @Override
    @NotNull
    public AuthorizationScope findScope(@NonNull String scope) {
        AuthorizationScopeProvider provider = providers.stream()
                .filter(p -> p.supports(scope))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported scope: " + scope));
        return provider.findScope(scope);
    }

    @NonNull
    @Override
    public List<AuthorizationScope> findScopes(@NonNull List<String> scopes) {
        if (scopes.isEmpty()) {
            return List.of();
        }
        Map<AuthorizationScopeProvider, List<String>> map = new HashMap<>();
        for (String scope : scopes) {
            AuthorizationScopeProvider provider = providers.stream()
                    .filter(p -> p.supports(scope))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported scope: " + scope));
            map.computeIfAbsent(provider, k -> new ArrayList<>()).add(scope);
        }
        List<AuthorizationScope> result = new ArrayList<>();
        for (Map.Entry<AuthorizationScopeProvider, List<String>> entry : map.entrySet()) {
            AuthorizationScopeProvider provider = entry.getKey();
            List<String> scopeList = entry.getValue();
            List<AuthorizationScope> foundScopes = provider.findScopes(scopeList);
            result.addAll(foundScopes);
        }
        return result;
    }

    @Override
    public boolean supports(@NonNull String scope) {
        return providers.stream().anyMatch(provider -> provider.supports(scope));
    }
}
