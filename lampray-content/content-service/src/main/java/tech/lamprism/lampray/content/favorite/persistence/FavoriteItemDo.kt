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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.favorite.FavoriteItemResourceKind
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "favorite_item")
class FavoriteItemDo(
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "favorite_group_id", nullable = false)
    var favoriteGroupId: Long = 0,

    @Column(name = "content_id", nullable = false)
    private var contentId: Long = 0,

    @Column(name = "type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private var contentType: ContentType = ContentType.ARTICLE,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
) : DataEntity<Long> {
    override fun getId(): Long = id

    override fun getSystemResourceKind(): SystemResourceKind = FavoriteItemResourceKind

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    class Builder {
        private var id: Long = 0
        private var userId: Long = 0
        private var favoriteGroupId: Long = 0
        private var contentId: Long = 0
        private var contentType: ContentType? = null
        private var createTime: OffsetDateTime? = null
        private var updateTime: OffsetDateTime? = null
        private var deleted: Boolean = false

        constructor()

        constructor(favoriteItemDo: FavoriteItemDo) {
            this.id = favoriteItemDo.id
            this.userId = favoriteItemDo.userId
            this.favoriteGroupId = favoriteItemDo.favoriteGroupId
            this.contentId = favoriteItemDo.contentId
            this.contentType = favoriteItemDo.contentType
            this.createTime = favoriteItemDo.createTime
            this.updateTime = favoriteItemDo.updateTime
            this.deleted = favoriteItemDo.deleted
        }

        fun setId(id: Long) = apply {
            this.id = id
        }

        fun setUserId(userId: Long) = apply {
            this.userId = userId
        }

        fun setFavoriteGroupId(favoriteGroupId: Long) = apply {
            this.favoriteGroupId = favoriteGroupId
        }

        fun setContentId(contentId: Long) = apply {
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

        fun build() = FavoriteItemDo(
            id, userId, favoriteGroupId, contentId, contentType!!,
            createTime!!, updateTime!!, deleted
        )
    }
}