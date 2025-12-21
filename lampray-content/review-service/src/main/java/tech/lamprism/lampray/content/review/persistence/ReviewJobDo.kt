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
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.ContentAssociated
import tech.lamprism.lampray.content.ContentIdentity
import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.review.ReviewJob
import tech.lamprism.lampray.content.review.ReviewJobResourceKind
import tech.lamprism.lampray.content.review.ReviewMark
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime


/**
 * @author RollW
 */
@Entity
@Table(name = "review_job")
class ReviewJobDo(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,

    @Column(name = "content_id", nullable = false)
    var reviewContentId: Long = 0,

    @Column(name = "content_type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var reviewContentType: ContentType = ContentType.ARTICLE,

    @Column(name = "reviewer_id", nullable = false)
    var reviewerId: Long = 0,

    @Column(name = "operator_id")
    var operatorId: Long? = null,

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var status: ReviewStatus = ReviewStatus.PENDING,

    @Column(name = "result", nullable = false)
    var result: String = "",

    @Column(name = "assigned_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var assignedTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "review_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var reviewTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "review_mark", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var reviewMark: ReviewMark = ReviewMark.NORMAL
) : DataEntity<Long>, ContentAssociated {
    override fun getSystemResourceKind(): SystemResourceKind =
        ReviewJobResourceKind

    override fun getCreateTime(): OffsetDateTime = assignedTime

    override fun getUpdateTime(): OffsetDateTime = reviewTime

    override fun getId(): Long? = id

    fun setId(id: Long) {
        this.id = id
    }

    override fun getAssociatedContent(): ContentIdentity =
        ContentIdentity.of(reviewContentId, reviewContentType)

    fun toBuilder(): Builder {
        return Builder(this)
    }

    fun lock(): ReviewJob {
        return ReviewJob(
            id,
            reviewContentId,
            reviewContentType,
            reviewerId,
            operatorId,
            status,
            result,
            assignedTime,
            reviewTime,
            reviewMark
        )
    }

    fun reviewPass(operator: Long, time: OffsetDateTime) = apply {
        this.operatorId = operator
        this.status = ReviewStatus.APPROVED
        this.reviewTime = time
    }

    fun reviewReject(operator: Long, reason: String, time: OffsetDateTime) = apply {
        this.operatorId = operator
        this.status = ReviewStatus.REJECTED
        this.result = reason
        this.reviewTime = time
    }

    class Builder {
        private var id: Long? = null
        private var reviewContentId: Long = 0
        private var reviewContentType: ContentType = ContentType.ARTICLE
        private var reviewerId: Long = 0
        private var operatorId: Long? = null
        private var status: ReviewStatus = ReviewStatus.PENDING
        private var result: String = ""
        private var assignedTime: OffsetDateTime = OffsetDateTime.now()
        private var reviewTime: OffsetDateTime = OffsetDateTime.now()
        private var reviewMark: ReviewMark = ReviewMark.NORMAL

        constructor()

        constructor(other: ReviewJobDo) {
            this.id = other.id
            this.reviewContentId = other.reviewContentId
            this.reviewContentType = other.reviewContentType
            this.reviewerId = other.reviewerId
            this.operatorId = other.operatorId
            this.status = other.status
            this.result = other.result
            this.assignedTime = other.assignedTime
            this.reviewTime = other.reviewTime
            this.reviewMark = other.reviewMark
        }

        fun setId(id: Long?): Builder {
            this.id = id
            return this
        }

        fun setReviewContentId(reviewContentId: Long) = apply {
            this.reviewContentId = reviewContentId
        }

        fun setReviewContentType(reviewContentType: ContentType) = apply {
            this.reviewContentType = reviewContentType
            return this
        }

        fun setReviewerId(reviewerId: Long) = apply {
            this.reviewerId = reviewerId
        }

        fun setOperatorId(operatorId: Long?) = apply {
            this.operatorId = operatorId
        }

        fun setStatus(status: ReviewStatus) = apply {
            this.status = status
        }

        fun setResult(result: String) = apply {
            this.result = result
        }

        fun setAssignedTime(assignedTime: OffsetDateTime) = apply {
            this.assignedTime = assignedTime
        }

        fun setReviewTime(reviewTime: OffsetDateTime) = apply {
            this.reviewTime = reviewTime
        }

        fun setReviewMark(reviewMark: ReviewMark) = apply {
            this.reviewMark = reviewMark
        }

        fun build(): ReviewJobDo {
            return ReviewJobDo(
                id,
                reviewContentId,
                reviewContentType,
                reviewerId,
                operatorId,
                status,
                result,
                assignedTime,
                reviewTime,
                reviewMark
            )
        }
    }

    companion object {
        @JvmStatic
        fun ReviewJob.toDo(): ReviewJobDo = ReviewJobDo(
            id,
            reviewContentId,
            reviewContentType,
            reviewerId,
            operatorId,
            status,
            result,
            assignedTime,
            reviewTime,
            reviewMark
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}