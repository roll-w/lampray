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

package tech.lamprism.lampray.security.authorization.adapter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import tech.lamprism.lampray.security.authorization.AuthorityHelper;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.PrivilegedUser;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * @author RollW
 */
public class PrivilegedUserAuthenticationToken extends AbstractAuthenticationToken {
    @Serial
    private static final long serialVersionUID = 212716237026129180L;

    private final PrivilegedUser user;

    public PrivilegedUserAuthenticationToken(PrivilegedUser user) {
        super(toAuthorities(user));
        this.user = user;
    }

    public PrivilegedUserAuthenticationToken(PrivilegedUser user, Collection<? extends AuthorizationScope> scopes) {
        super(AuthorityHelper.toAuthorities(scopes));
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    @Override
    public PrivilegedUser getCredentials() {
        return user;
    }

    @Override
    public PrivilegedUser getPrincipal() {
        return user;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
    }

    private static List<GrantedAuthority> toAuthorities(PrivilegedUser user) {
        return AuthorityHelper.toAuthorities(user);
    }
}
