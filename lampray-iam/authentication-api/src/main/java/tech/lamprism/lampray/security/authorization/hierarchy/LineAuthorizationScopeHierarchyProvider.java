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

package tech.lamprism.lampray.security.authorization.hierarchy;

import space.lingu.NonNull;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.LineAuthorizationScope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Provide hierarchy for {@link LineAuthorizationScope}.
 *
 * @author RollW
 */
public class LineAuthorizationScopeHierarchyProvider implements AuthorizationScopeHierarchyProvider {
    @Override
    @NonNull
    public Collection<AuthorizationScope> flattenAuthorizationScopes(
            @NonNull Collection<? extends AuthorizationScope> authorizationScopes) {
        Set<AuthorizationScope> result = new HashSet<>();

        for (AuthorizationScope authorizationScope : authorizationScopes) {
            if (!supports(authorizationScope)) {
                throw new IllegalArgumentException("Unsupported authorization scope: " + authorizationScope);
            }
            LineAuthorizationScope lineAuthorizationScope = (LineAuthorizationScope) authorizationScope;
            AuthorizationScope parent = lineAuthorizationScope.getParent();
            while (parent != null) {
                result.add(parent);
                if (!(parent instanceof LineAuthorizationScope)) {
                    break;
                }
                parent = ((LineAuthorizationScope) parent).getParent();
            }
            result.add(lineAuthorizationScope);
        }

        return result;
    }

    @Override
    public boolean supports(@NonNull AuthorizationScope scope) {
        return scope instanceof LineAuthorizationScope;
    }
}
