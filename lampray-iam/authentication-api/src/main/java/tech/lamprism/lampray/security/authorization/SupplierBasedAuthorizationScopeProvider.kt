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

package tech.lamprism.lampray.security.authorization

/**
 * @author RollW
 */
class SupplierBasedAuthorizationScopeProvider(
    authorizationScopeSuppliers: List<AuthorizationScopeSupplier>
) : AuthorizationScopeProvider {
    private val _scopes = authorizationScopeSuppliers
        .map { it.authorizationScopes }
        .flatten()

    override fun findScope(scope: String): AuthorizationScope = _scopes
        .firstOrNull {
            it.scope == scope
        } ?: throw IllegalArgumentException("Scope $scope not found")

    override fun findScopes(scopes: List<String>): List<AuthorizationScope> = _scopes
        .filter {
            scopes.contains(it.scope)
        }

    override val scopes: List<AuthorizationScope>
        get() = _scopes
}