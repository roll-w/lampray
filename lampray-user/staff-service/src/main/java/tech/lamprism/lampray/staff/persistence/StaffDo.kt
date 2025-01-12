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

package tech.lamprism.lampray.staff.persistence

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.staff.AttributedStaff
import tech.lamprism.lampray.staff.Staff
import tech.lamprism.lampray.staff.StaffType
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "staff")
class StaffDo(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    override var userId: Long = 0,

    @ElementCollection(targetClass = StaffType::class, fetch = FetchType.EAGER)
    @CollectionTable(name = "staff_types", joinColumns = [
        JoinColumn(name = "id", referencedColumnName = "id"),
    ], foreignKey = ForeignKey(name = "index__type__id"))
    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    override var types: Set<StaffType> = emptySet(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "as_user", nullable = false)
    var asUser: Boolean = false,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
) : DataEntity<Long>, AttributedStaff {
    override fun getId(): Long? = id
    
    fun setId(id: Long?) {
        this.id = id
    }

    override fun getCreateTime(): OffsetDateTime = createTime
    
    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }
    
    override val staffId: Long
        get() = id!!

    override fun getResourceId(): Long = id!!

    fun lock() = Staff(
        id, userId, types, createTime, updateTime, 
        asUser, deleted
    )

    class Builder {
        private var id: Long? = null
        private var userId: Long = 0
        private var types: MutableSet<StaffType>? = null
        private var createTime: OffsetDateTime? = null
        private var updateTime: OffsetDateTime? = null
        private var asUser = false
        private var deleted = false
        
        constructor()
        
        constructor(staff: StaffDo) {
            this.id = staff.id
            this.userId = staff.userId
            this.types = staff.types.toMutableSet()
            this.createTime = staff.createTime
            this.updateTime = staff.updateTime
            this.asUser = staff.asUser
            this.deleted = staff.deleted
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setUserId(userId: Long) = apply{
            this.userId = userId
        }

        fun setTypes(types: MutableSet<StaffType>) = apply {
            this.types = types
        }

        fun addTypes(vararg types: StaffType) = apply {
            if (this.types == null) {
                this.types = mutableSetOf()
            }
            this.types!!.addAll(types)
        }

        fun removeTypes(vararg types: StaffType) = apply {
            if (this.types == null) {
                this.types = mutableSetOf()
            }
            this.types!!.removeAll(types)
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun setAsUser(asUser: Boolean) = apply {
            this.asUser = asUser
        }

        fun setDeleted(deleted: Boolean) = apply {
            this.deleted = deleted
        }
        
        fun build() = StaffDo(
            id, userId, types ?: emptySet(),
            createTime!!, 
            updateTime!!, 
            asUser, deleted
        )
    }

    companion object {
        @JvmStatic
        fun Staff.toDo(): StaffDo {
            return StaffDo(
                id, userId, types, createTime, updateTime,
                isAsUser, isDeleted
            )
        }
        
        @JvmStatic
        fun builder() = Builder()
    }
    
}