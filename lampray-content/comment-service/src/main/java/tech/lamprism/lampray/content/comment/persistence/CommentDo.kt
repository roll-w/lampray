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

package tech.lamprism.lampray.content.comment.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.ContentAssociated
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentIdentity
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.comment.Comment
import tech.lamprism.lampray.content.comment.CommentDetailsMetadata
import tech.lamprism.lampray.content.comment.CommentResourceKind
import tech.lamprism.lampray.content.comment.CommentStatus
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "comment")
class CommentDo(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    private var userId: Long = 0,

    @Column(name = "parent_id", nullable = false)
    var parentId: Long = 0,

    @Lob
    @Column(name = "content", nullable = false, length = 1000000)
    private var content: StructuralText = StructuralText.EMPTY,

    @Column(name = "create_time", nullable = false)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "comment_on_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var commentOnType: ContentType = ContentType.COMMENT,

    @Column(name = "comment_on_id", nullable = false)
    var commentOnId: Long = 0,

    @Column(name = "comment_status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var commentStatus: CommentStatus = CommentStatus.NONE
) : DataEntity<Long>, ContentDetails, ContentAssociated {
    override fun getEntityId(): Long? = id

    fun setId(id: Long) {
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

    override fun getContentType(): ContentType =
        ContentType.COMMENT

    override fun getUserId(): Long = userId

    fun setUserId(userId: Long) {
        this.userId = userId
    }

    override fun getTitle(): String? = null

    override fun getContent(): StructuralText = content

    fun setContent(content: StructuralText) {
        this.content = content
    }

    override fun getAssociatedContent(): ContentIdentity =
        ContentIdentity.of(commentOnId, commentOnType)

    override fun getSystemResourceKind(): SystemResourceKind =
        CommentResourceKind

    override fun getMetadata(): CommentDetailsMetadata {
        return CommentDetailsMetadata(
            commentOnType, commentOnId, parentId
        )
    }

    fun lock(): Comment = Comment(
        id!!, userId, parentId, content, createTime,
        updateTime, commentOnType, commentOnId, commentStatus
    )

    override fun toString(): String {
        return "CommentDo(" +
                "id=$id, " +
                "userId=$userId, " +
                "parentId=$parentId, " +
                "content='$content', " +
                "createTime=$createTime, " +
                "updateTime=$updateTime, " +
                "commentOnType=$commentOnType, " +
                "commentOnId=$commentOnId, " +
                "commentStatus=$commentStatus" +
                ")"
    }

    class Builder {
        private var id: Long? = null
        private var userId: Long = 0
        private var parentId: Long = 0
        private var content: StructuralText? = null
        private var createTime: OffsetDateTime? = null
        private var updateTime: OffsetDateTime? = null
        private var commentOnType: ContentType? = null
        private var commentOnId: Long = 0
        private var commentStatus: CommentStatus? = null

        constructor()

        constructor(other: CommentDo) {
            this.id = other.id
            this.userId = other.userId
            this.parentId = other.parentId
            this.content = other.content
            this.createTime = other.createTime
            this.updateTime = other.updateTime
            this.commentOnType = other.commentOnType
            this.commentOnId = other.commentOnId
            this.commentStatus = other.commentStatus
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setUserId(userId: Long) = apply {
            this.userId = userId
        }

        fun setParentId(parentId: Long) = apply {
            this.parentId = parentId
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

        fun setCommentOnType(commentOnType: ContentType) = apply {
            this.commentOnType = commentOnType
        }

        fun setCommentOnId(commentOnId: Long) = apply {
            this.commentOnId = commentOnId
        }

        fun setCommentStatus(commentStatus: CommentStatus) = apply {
            this.commentStatus = commentStatus
        }

        fun build(): CommentDo {
            return CommentDo(
                id,
                userId,
                parentId,
                content!!,
                createTime!!,
                updateTime!!,
                commentOnType!!,
                commentOnId,
                commentStatus!!
            )
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmStatic
        fun Comment.toDo(): CommentDo {
            return CommentDo(
                entityId, userId, parentId, content,
                createTime, updateTime,
                commentOnType, commentOnId, commentStatus
            )
        }
    }
}