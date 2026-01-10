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
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.proxy.HibernateProxy
import org.hibernate.type.SqlTypes
import tech.lamprism.lampray.DataEntity
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.ReviewTask
import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.ReviewTaskResourceKind
import tech.lamprism.lampray.content.review.ReviewTaskStatus
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * Review job task entity that represents a single review task assigned to a reviewer.
 *
 * @author RollW
 */
@Entity
@Table(name = "review_job_task")
class ReviewTaskEntity(
    /**
     * Auto-generated primary key for database.
     */
    @Column(name = "id", nullable = false)
    @Generated(event = [EventType.INSERT])
    @JdbcTypeCode(SqlTypes.BIGINT)
    var id: Long? = null,

    /**
     * Unique identifier for this review task.
     */
    @Id
    @Column(name = "resource_id", unique = true, nullable = false, length = 64)
    var resourceId: String,

    /**
     * Reference to the parent review job that this task belongs to.
     * Links to [tech.lamprism.lampray.content.review.ReviewJob.jobId].
     */
    @Column(name = "review_job_id", nullable = false, length = 64)
    override var reviewJobId: String,

    /**
     * Current status of this review task.
     * See [ReviewStatus] for possible values (PENDING, APPROVED, REJECTED, CANCELED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    override var status: ReviewTaskStatus = ReviewTaskStatus.PENDING,

    /**
     * ID of the reviewer assigned to this task.
     */
    @Column(name = "reviewer_id", length = 64)
    override var reviewerId: Long = -1,

    /**
     * Review feedback submitted by the reviewer.
     */
    @Convert(converter = ReviewFeedbackAttributeConverter::class)
    @Lob
    @Column(name = "feedback")
    override var feedback: ReviewFeedback? = null,

    /**
     * Timestamp when this task was created.
     */
    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    /**
     * Timestamp when this task was last updated. Take as the finish time when status is final.
     */
    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now()
) : DataEntity<String>, ReviewTaskDetails {

    override fun getSystemResourceKind(): SystemResourceKind =
        ReviewTaskResourceKind

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    override fun getEntityId(): String = resourceId

    fun toBuilder(): Builder {
        return Builder(this)
    }

    /**
     * Locks the entity into an immutable [tech.lamprism.lampray.content.review.ReviewTask] value object.
     */
    fun lock(): ReviewTask {
        return ReviewTask(
            id!!,
            resourceId,
            reviewJobId,
            status,
            reviewerId,
            feedback,
            createTime,
            updateTime
        )
    }

    override val taskId: String
        get() = resourceId

    /**
     * Builder for creating new [ReviewTaskEntity] instances.
     */
    class Builder {
        private var id: Long? = null
        private var resourceId: String? = null
        private var reviewJobId: String? = null
        private var taskStatus: ReviewTaskStatus = ReviewTaskStatus.PENDING
        private var reviewerId: Long = -1
        private var feedback: ReviewFeedback? = null
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()

        constructor()

        constructor(other: ReviewTaskEntity) {
            this.id = other.id
            this.resourceId = other.resourceId
            this.reviewJobId = other.reviewJobId
            this.taskStatus = other.status
            this.reviewerId = other.reviewerId
            this.feedback = other.feedback
            this.createTime = other.createTime
            this.updateTime = other.updateTime
        }

        fun setId(id: Long?) = apply {
            this.id = id
        }

        fun setResourceId(resourceId: String) = apply {
            this.resourceId = resourceId
        }

        fun setReviewJobId(reviewJobId: String) = apply {
            this.reviewJobId = reviewJobId
        }

        fun setTaskStatus(taskStatus: ReviewTaskStatus) = apply {
            this.taskStatus = taskStatus
        }

        fun setReviewerId(reviewerId: Long) = apply {
            this.reviewerId = reviewerId
        }

        fun setFeedback(feedback: ReviewFeedback?) = apply {
            this.feedback = feedback
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): ReviewTaskEntity {
            return ReviewTaskEntity(
                id,
                resourceId = resourceId!!,
                reviewJobId = reviewJobId!!,
                status = taskStatus,
                reviewerId = reviewerId,
                feedback = feedback,
                createTime = createTime,
                updateTime = updateTime
            )
        }
    }

    companion object {
        /**
         * Converts immutable [ReviewTask] to mutable entity.
         */
        @JvmStatic
        fun ReviewTask.toDo(): ReviewTaskEntity = ReviewTaskEntity(
            id,
            resourceId = taskId,
            reviewJobId = reviewJobId,
            status = status,
            reviewerId = reviewerId,
            feedback = feedback,
            createTime = createTime,
            updateTime = updateTime
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as ReviewTaskEntity

        return resourceId == other.resourceId
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}

