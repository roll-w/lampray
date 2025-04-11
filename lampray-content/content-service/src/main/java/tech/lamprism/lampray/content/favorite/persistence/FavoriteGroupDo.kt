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

package tech.lamprism.lampray.content.favorite.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.favorite.FavoriteGroup
import tech.lamprism.lampray.content.favorite.FavoriteGroupResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "favorite_group")
class FavoriteGroupDo(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "public", nullable = false)
    var isPublic: Boolean = false,

    @Lob
    @Column(name = "description", nullable = false, length = 16777215)
    var description: String = "",

    @Column(name = "icon", nullable = false)
    private var icon: String = "",

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
) : DataEntity<Long> {
    override fun getId(): Long = id!!

    override fun getSystemResourceKind() = FavoriteGroupResourceKind

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setId(id: Long) {
        this.id = id
    }

    fun lock(): FavoriteGroup =
        FavoriteGroup(
            id,
            name,
            userId,
            isPublic,
            description,
            icon,
            createTime,
            updateTime,
            deleted
        )

    fun toBuilder() = Builder(this)

    class Builder {
        private var id: Long? = null
        private var name: String = ""
        private var userId: Long = 0L
        private var isPublic: Boolean = false
        private var description: String = ""
        private var icon: String = ""
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()
        private var deleted: Boolean = false

        constructor()

        constructor(favoriteGroup: FavoriteGroupDo) {
            this.id = favoriteGroup.id
            this.name = favoriteGroup.name
            this.userId = favoriteGroup.userId
            this.isPublic = favoriteGroup.isPublic
            this.description = favoriteGroup.description
            this.icon = favoriteGroup.icon
            this.createTime = favoriteGroup.createTime
            this.updateTime = favoriteGroup.updateTime
            this.deleted = favoriteGroup.deleted
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setName(name: String) = apply {
            this.name = name
        }

        fun setUserId(userId: Long) = apply {
            this.userId = userId
        }

        fun setPublic(isPublic: Boolean) = apply {
            this.isPublic = isPublic
        }

        fun setDescription(description: String) = apply {
            this.description = description
        }

        fun setIcon(icon: String) = apply {
            this.icon = icon
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun setDeleted(deleted: Boolean) = apply {
            this.deleted = deleted
        }

        fun build() = FavoriteGroupDo(
            id,
            name,
            userId,
            isPublic,
            description,
            icon,
            createTime,
            updateTime,
            deleted
        )
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()

        @JvmStatic
        fun FavoriteGroup.toDo() = FavoriteGroupDo(
            id,
            name,
            userId,
            isPublic,
            description,
            icon,
            createTime,
            updateTime,
            isDeleted
        )
    }
}
