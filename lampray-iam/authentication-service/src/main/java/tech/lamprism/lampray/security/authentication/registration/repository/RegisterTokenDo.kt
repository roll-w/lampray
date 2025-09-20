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

package tech.lamprism.lampray.security.authentication.registration.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.security.authentication.VerifiableToken
import tech.lamprism.lampray.security.authentication.registration.RegisterTokenResourceKind
import tech.lamprism.lampray.security.authentication.registration.RegisterVerificationToken
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(
    name = "register_verification_token",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["token"], name = "index__token")
    ]
)
class RegisterTokenDo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private var id: Long? = null,

    @Column(name = "token")
    var token: String = "",

    @Column(name = "user_id")
    var userId: Long = 0,

    @Column(name = "expiry_time")
    var expiryTime: Long = 0,

    @Column(name = "used")
    var used: Boolean = false
) : VerifiableToken, DataEntity<Long> {
    override fun token(): String {
        return token
    }

    override fun isUsable(): Boolean {
        return !used && !isExpired()
    }

    fun setId(id: Long?) {
        this.id = id
    }

    override fun getId(): Long? {
        return id
    }

    override fun getCreateTime(): OffsetDateTime = TimeAttributed.NONE_TIME

    override fun getUpdateTime(): OffsetDateTime = TimeAttributed.NONE_TIME

    override fun getSystemResourceKind(): SystemResourceKind =
        RegisterTokenResourceKind

    fun lock(): RegisterVerificationToken = RegisterVerificationToken(
        id, token, userId,
        expiryTime, used
    )

    fun toBuilder(): Builder {
        return Builder(this)
    }

    fun markVerified() {
        used = true
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiryTime
    }

    companion object {
        @JvmStatic
        fun RegisterVerificationToken.toDo(): RegisterTokenDo = RegisterTokenDo(
            id, token, userId, expiryTime, used
        )

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {
        private var id: Long? = null
        private var token: String = ""
        private var userId: Long = 0
        private var expiryTime: Long = 0
        private var used = false

        internal constructor()

        internal constructor(registerTokenDo: RegisterTokenDo) {
            id = registerTokenDo.id
            token = registerTokenDo.token
            userId = registerTokenDo.userId
            expiryTime = registerTokenDo.expiryTime
            used = registerTokenDo.used
        }

        fun setId(id: Long?): Builder {
            this.id = id
            return this
        }

        fun setToken(token: String): Builder {
            this.token = token
            return this
        }

        fun setUserId(userId: Long): Builder {
            this.userId = userId
            return this
        }

        fun setExpiryTime(expiryTime: Long): Builder {
            this.expiryTime = expiryTime
            return this
        }

        fun setUsed(used: Boolean): Builder {
            this.used = used
            return this
        }
    }
}