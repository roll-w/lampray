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

import tech.lamprism.lampray.security.authorization.AuthorityHelper.distinctByScope

/**
 * A privilege is a set of permitted authorization scopes
 * that a user has.
 *
 * @author RollW
 */
interface Privilege {
    val scopes: List<AuthorizationScope>

    operator fun plus(other: Privilege): Privilege {
        if (this == other) {
            return this
        }
        return SimplePrivilege((scopes + other.scopes).distinctByScope())
    }

    operator fun minus(other: Privilege): Privilege {
        if (this == other) {
            return EmptyPrivilege
        }
        return SimplePrivilege((scopes - other.scopes.toSet()).distinctByScope())
    }

}

object EmptyPrivilege : Privilege {
    override val scopes: List<AuthorizationScope> = emptyList()
}
