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
package tech.lamprism.lampray.user.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import space.lingu.NonNull
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.user.AttributedUserDetails
import tech.lamprism.lampray.user.Role
import tech.lamprism.lampray.user.User
import tech.lamprism.lampray.user.UserResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.io.Serializable
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(
    name = "user", uniqueConstraints = [
        UniqueConstraint(columnNames = ["username"], name = "index__username")
    ]
)
class UserDo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private var id: Long? = null,

    @Column(name = "username", length = 120, nullable = false)
    private var username: String = "",

    @Column(name = "password", length = 120, nullable = false)
    private var password: String = "",

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private var role: Role = Role.USER,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "register_time", nullable = false)
    var registerTime: OffsetDateTime = OffsetDateTime.now(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "email", length = 255, nullable = false)
    private var email: String = "",

    // TODO: phone encryption
    @Column(name = "phone", length = 255, nullable = false)
    var phone: String = "",

    @Column(name = "enabled", nullable = false)
    private var enabled: Boolean = false,

    @Column(name = "locked", nullable = false)
    private var locked: Boolean = false,

    @Column(name = "account_canceled", nullable = false)
    private var canceled: Boolean = false
) : Serializable, DataEntity<Long>, AttributedUserDetails {
    override fun getEntityId(): Long? {
        return id
    }

    fun setId(id: Long?) {
        this.id = id
    }

    override fun getResourceId(): Long {
        return id!!
    }

    @NonNull
    override fun getSystemResourceKind(): SystemResourceKind {
        return UserResourceKind
    }

    override fun getCreateTime(): OffsetDateTime = registerTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    override fun getUserId(): Long {
        return id!!
    }

    override fun getEmail(): String {
        return email
    }

    fun setEmail(email: String) {
        this.email = email
    }

    override fun getRole(): Role {
        return role
    }

    fun setRole(role: Role) {
        this.role = role
    }

    override fun isLocked(): Boolean {
        return locked
    }

    fun setLocked(locked: Boolean) {
        this.locked = locked
    }

    override fun isCanceled(): Boolean {
        return canceled
    }

    fun setCanceled(canceled: Boolean) {
        this.canceled = canceled
    }

    override fun getPassword(): String {
        return password
    }

    fun setPassword(password: String) {
        this.password = password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun toString(): String {
        return "UserDo{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                ", registerTime=" + registerTime +
                ", updateTime=" + updateTime +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", enabled=" + enabled +
                ", locked=" + locked +
                ", canceled=" + canceled +
                '}'
    }

    fun lock(): User {
        return User(
            id, username, password, role,
            registerTime, updateTime, email, phone,
            enabled, locked, canceled
        )
    }

    fun toBuilder() = Builder(this)

    class Builder {
        private var id: Long? = null
        private var username: String = ""
        private var password: String = ""
        private var role: Role = Role.USER
        private var registerTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()
        private var email: String = ""
        private var phone: String = ""
        private var enabled = false
        private var locked = false
        private var canceled = false

        constructor()

        constructor(other: UserDo) {
            this.id = other.id
            this.username = other.username
            this.password = other.password
            this.role = other.role
            this.registerTime = other.registerTime
            this.updateTime = other.updateTime
            this.email = other.email
            this.phone = other.phone
            this.enabled = other.isEnabled
            this.locked = other.isLocked
            this.canceled = other.isCanceled
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setUsername(username: String) = apply {
            this.username = username
        }

        fun setPassword(password: String) = apply {
            this.password = password
        }

        fun setRole(role: Role) = apply {
            this.role = role
        }

        fun setRegisterTime(registerTime: OffsetDateTime) = apply {
            this.registerTime = registerTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun setEmail(email: String) = apply {
            this.email = email
        }

        fun setPhone(phone: String) = apply {
            this.phone = phone
        }

        fun setEnabled(enabled: Boolean) = apply {
            this.enabled = enabled
        }

        fun setLocked(locked: Boolean) = apply {
            this.locked = locked
        }

        fun setCanceled(canceled: Boolean) = apply {
            this.canceled = canceled
        }

        fun build(): UserDo {
            return UserDo(
                id,
                username,
                password,
                role,
                registerTime,
                updateTime,
                email,
                phone,
                enabled,
                locked,
                canceled
            )
        }
    }

    companion object {
        @JvmStatic
        fun User.toDo(): UserDo {
            return UserDo(
                entityId, username, password, role,
                registerTime, updateTime, email, phone,
                isEnabled, isLocked, isCanceled
            )
        }

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
