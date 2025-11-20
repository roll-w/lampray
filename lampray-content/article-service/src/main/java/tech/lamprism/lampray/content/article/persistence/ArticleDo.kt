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

package tech.lamprism.lampray.content.article.persistence

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
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentDetailsMetadata
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.article.Article
import tech.lamprism.lampray.content.article.ArticleDetailsMetadata
import tech.lamprism.lampray.content.structuraltext.StructuralText
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "article")
class ArticleDo(
    @Column(name = "id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    private var userId: Long = 0,

    @Column(name = "title", nullable = false, length = 255)
    private var title: String = "",

    @Column(name = "cover", nullable = false, length = 255)
    var cover: String = "",

    @Lob
    @Column(name = "content", nullable = false, length = 20000000)
    private var content: StructuralText = StructuralText.EMPTY,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "createTime", nullable = false)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now()
) : DataEntity<Long>, ContentDetails {
    override fun getId(): Long? = id

    fun setId(id: Long?) {
        this.id = id
    }

    override fun getResourceId(): Long = id!!

    override fun getCreateTime(): OffsetDateTime = createTime

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    override fun getContentId(): Long = id!!

    override fun getContentType(): ContentType = ContentType.ARTICLE

    override fun getUserId(): Long = userId

    fun setUserId(userId: Long) {
        this.userId = userId
    }

    override fun getTitle(): String? = title

    fun setTitle(title: String) {
        this.title = title
    }

    override fun getContent(): StructuralText = content

    fun setContent(content: StructuralText) {
        this.content = content
    }

    override fun getMetadata(): ContentDetailsMetadata =
        ArticleDetailsMetadata(cover)

    fun lock(): Article = Article(
        id!!, userId, title, cover, content, createTime, updateTime
    )

    fun toBuilder(): Builder = Builder(this)

    override fun toString(): String {
        return "ArticleDo(" +
                "id=$id, " +
                "userId=$userId, " +
                "title='$title', " +
                "cover='$cover', " +
                "content='$content', " +
                "createTime=$createTime, " +
                "updateTime=$updateTime" +
                ")"
    }

    class Builder {
        private var id: Long? = null
        private var userId: Long = 0
        private var title: String? = null
        private var cover: String = ""
        private var content: StructuralText? = null
        private var createTime: OffsetDateTime? = null
        private var updateTime: OffsetDateTime? = null

        constructor()

        constructor(other: ArticleDo) {
            this.id = other.id
            this.userId = other.userId
            this.title = other.title
            this.cover = other.cover
            this.content = other.content
            this.createTime = other.createTime
            this.updateTime = other.updateTime
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setUserId(userId: Long) = apply {
            this.userId = userId
        }

        fun setTitle(title: String) = apply {
            this.title = title
        }

        fun setCover(cover: String) = apply {
            this.cover = cover
        }

        fun setContent(content: StructuralText) = apply {
            this.content = content
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): ArticleDo {
            return ArticleDo(
                id, userId, title!!, cover,
                content!!, createTime!!, updateTime!!
            )
        }
    }

    companion object {
        @JvmStatic
        fun Article.toDo(): ArticleDo {
            return ArticleDo(
                id, userId, title, cover, content,
                createTime, updateTime
            )
        }

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}