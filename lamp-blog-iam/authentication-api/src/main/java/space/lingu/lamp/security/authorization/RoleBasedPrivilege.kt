/*
 * Copyright (C) 2023 RollW
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

package space.lingu.lamp.security.authorization

import space.lingu.lamp.security.authorization.RoleBasedAuthorizationScope.Companion.toScope
import space.lingu.lamp.user.Role

/**
 * @author RollW
 */
data class RoleBasedPrivilege(
    val role: Role
) : Privilege {
    override val scopes: List<AuthorizationScope>
        get() = listOf(role.toScope())

    companion object {
        @JvmStatic
        fun Role.toPrivilege() = RoleBasedPrivilege(this)
    }
}