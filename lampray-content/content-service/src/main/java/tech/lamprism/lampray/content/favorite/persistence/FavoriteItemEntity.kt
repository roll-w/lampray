/*
 * Copyright (C) 2023-2026 RollW
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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.favorite.FavoriteItem
import tech.lamprism.lampray.content.favorite.FavoriteItemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "favorite_item")
class FavoriteItemEntity(
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    private var id: Long? = null,

    @Id
    @Column(name = "resource_id", nullable = false, length = 64, unique = true)
    private var resourceId: String,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "group_id", nullable = false, length = 64)
    var groupId: String,

    @Column(name = "content_id", nullable = false, length = 64)
    var contentId: String = "",

    @Column(name = "content_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var contentType: ContentType = ContentType.ARTICLE,

    @Column(name = "create_time", nullable = false)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
) : DataEntity<String> {
    override fun getEntityId(): String = resourceId

    fun getId(): Long? = id

    override fun getSystemResourceKind() = FavoriteItemResourceKind

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun lock(): FavoriteItem =
        FavoriteItem(
            id,
            resourceId,
            groupId,
            userId,
            contentId,
            contentType,
            createTime,
            updateTime,
            deleted
        )

    fun toBuilder() = Builder(this)

    class Builder {
        private var id: Long? = null
        private var resourceId: String? = null
        private var userId: Long = 0
        private var groupId: String = ""
        private var contentId: String = ""
        private var contentType: ContentType = ContentType.ARTICLE
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()
        private var deleted: Boolean = false

        constructor()

        constructor(favoriteItem: FavoriteItemEntity) {
            this.id = favoriteItem.id
            this.resourceId = favoriteItem.resourceId
            this.userId = favoriteItem.userId
            this.groupId = favoriteItem.groupId
            this.contentId = favoriteItem.contentId
            this.contentType = favoriteItem.contentType
            this.createTime = favoriteItem.createTime
            this.updateTime = favoriteItem.updateTime
            this.deleted = favoriteItem.deleted
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setResourceId(resourceId: String) = apply {
            this.resourceId = resourceId
        }

        fun setUserId(userId: Long) = apply {
            this.userId = userId
        }

        fun setGroupId(groupId: String) = apply {
            this.groupId = groupId
        }

        fun setContentId(contentId: String) = apply {
            this.contentId = contentId
        }

        fun setContentType(contentType: ContentType) = apply {
            this.contentType = contentType
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

        fun build(): FavoriteItemEntity {
            return FavoriteItemEntity(
                id = id,
                resourceId = resourceId!!,
                userId = userId,
                groupId = groupId,
                contentId = contentId,
                contentType = contentType,
                createTime = createTime,
                updateTime = updateTime,
                deleted = deleted
            )
        }
    }

    companion object {
        @JvmStatic
        fun FavoriteItem.toEntity() = FavoriteItemEntity(
            id = id,
            resourceId = entityId,
            userId = userId,
            groupId = groupId,
            contentId = contentId,
            contentType = contentType,
            createTime = createTime,
            updateTime = updateTime,
            deleted = isDeleted
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
