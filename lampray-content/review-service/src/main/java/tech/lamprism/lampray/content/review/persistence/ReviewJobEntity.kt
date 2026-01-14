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

package tech.lamprism.lampray.content.review.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.ContentAssociated
import tech.lamprism.lampray.content.ContentIdentity
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.review.ReviewJob
import tech.lamprism.lampray.content.review.ReviewJobResourceKind
import tech.lamprism.lampray.content.review.ReviewJobSummary
import tech.lamprism.lampray.content.review.ReviewMark
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime


/**
 * @author RollW
 */
@Entity
@Table(name = "review_job")
class ReviewJobEntity(
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Generated(event = [EventType.INSERT])
    var id: Long? = null,

    @Id
    @Column(name = "resource_id", nullable = false, length = 64, unique = true)
    var resourceId: String = "",

    @Column(name = "content_id", nullable = false)
    var reviewContentId: Long = 0,

    @Column(name = "content_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var reviewContentType: ContentType = ContentType.ARTICLE,

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    override var status: ReviewStatus = ReviewStatus.PENDING,

    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "review_mark", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    override var reviewMark: ReviewMark = ReviewMark.NORMAL
) : DataEntity<String>, ContentAssociated, ReviewJobSummary {
    override fun getSystemResourceKind(): SystemResourceKind =
        ReviewJobResourceKind

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime
    
    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }
    
    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }
    
    override fun getEntityId(): String? = resourceId

    override val jobId: String
        get() = resourceId

    override fun getAssociatedContent(): ContentIdentity =
        ContentIdentity.of(reviewContentId, reviewContentType)

    fun toBuilder(): Builder {
        return Builder(this)
    }

    fun lock(): ReviewJob {
        return ReviewJob(
            id, resourceId,
            reviewContentId,
            reviewContentType,
            status,
            createTime,
            updateTime,
            reviewMark
        )
    }

    class Builder {
        private var id: Long? = null
        private var resourceId: String? = null
        private var reviewContentId: Long = 0
        private var reviewContentType: ContentType = ContentType.ARTICLE
        private var status: ReviewStatus = ReviewStatus.PENDING
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()
        private var reviewMark: ReviewMark = ReviewMark.NORMAL

        constructor()

        constructor(other: ReviewJobEntity) {
            this.id = other.id
            this.resourceId = other.resourceId
            this.reviewContentId = other.reviewContentId
            this.reviewContentType = other.reviewContentType
            this.status = other.status
            this.createTime = other.createTime
            this.updateTime = other.updateTime
            this.reviewMark = other.reviewMark
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setResourceId(resourceId: String) = apply {
            this.resourceId = resourceId
        }

        fun setReviewContentId(reviewContentId: Long) = apply {
            this.reviewContentId = reviewContentId
        }

        fun setReviewContentType(reviewContentType: ContentType) = apply {
            this.reviewContentType = reviewContentType
        }

        fun setStatus(status: ReviewStatus) = apply {
            this.status = status
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun setReviewMark(reviewMark: ReviewMark) = apply {
            this.reviewMark = reviewMark
        }

        fun build(): ReviewJobEntity {
            return ReviewJobEntity(
                id,
                resourceId = resourceId!!,
                reviewContentId = reviewContentId,
                reviewContentType = reviewContentType,
                status = status,
                createTime = createTime,
                updateTime = updateTime,
                reviewMark = reviewMark
            )
        }
    }

    companion object {
        @JvmStatic
        fun ReviewJob.toDo(): ReviewJobEntity = ReviewJobEntity(
            id, jobId,
            reviewContentId = reviewContentId,
            reviewContentType = reviewContentType,
            status = status,
            createTime = createTime,
            updateTime = updateTime,
            reviewMark = reviewMark
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}