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

package tech.lamprism.lampray.security.authorization.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tech.lamprism.lampray.security.authorization.AuthorizationScopeProvider;
import tech.lamprism.lampray.security.authorization.AuthorizationScopeSupplier;
import tech.lamprism.lampray.security.authorization.CompositedAuthorizationScopeProvider;
import tech.lamprism.lampray.security.authorization.RoleBasedAuthorizationScope;
import tech.lamprism.lampray.security.authorization.SupplierBasedAuthorizationScopeProvider;
import tech.lamprism.lampray.security.authorization.hierarchy.AuthorizationScopeHierarchyProvider;
import tech.lamprism.lampray.security.authorization.hierarchy.AuthorizationScopeHierarchyService;
import tech.lamprism.lampray.security.authorization.hierarchy.LineAuthorizationScopeHierarchyProvider;
import tech.lamprism.lampray.security.token.RefreshTokenAuthorizationScope;

import java.util.List;

@Configuration
public class AuthorizationScopeConfiguration {

    @Bean
    public List<AuthorizationScopeSupplier> authorizationScopeSuppliers() {
        return List.of(
                RoleBasedAuthorizationScope.USER
        );
    }

    @Bean
    public LineAuthorizationScopeHierarchyProvider lineAuthorizationScopeHierarchyProvider() {
        return new LineAuthorizationScopeHierarchyProvider();
    }

    @Bean
    @Primary
    public CompositedAuthorizationScopeProvider compositedAuthorizationScopeProvider(List<AuthorizationScopeProvider> authorizationScopeProviders) {
        return new CompositedAuthorizationScopeProvider(authorizationScopeProviders);
    }

    @Bean
    public SupplierBasedAuthorizationScopeProvider supplierBasedAuthorizationScopeProvider(List<AuthorizationScopeSupplier> suppliers) {
        return new SupplierBasedAuthorizationScopeProvider(suppliers);
    }

    @Bean
    public RefreshTokenAuthorizationScope refreshTokenAuthorizationScope() {
        return RefreshTokenAuthorizationScope.INSTANCE;
    }

    @Bean
    public AuthorizationScopeHierarchyService authorizationScopeHierarchyService(
            List<AuthorizationScopeHierarchyProvider> authorizationScopeHierarchyProviders) {
        return new AuthorizationScopeHierarchyService(authorizationScopeHierarchyProviders);
    }
}
