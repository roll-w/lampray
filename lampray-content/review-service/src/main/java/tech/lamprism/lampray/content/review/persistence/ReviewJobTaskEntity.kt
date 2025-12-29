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
import tech.lamprism.lampray.content.review.ReviewJobTask
import tech.lamprism.lampray.content.review.ReviewJobTaskResourceKind
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * @author RollW
 */
@Entity
@Table(name = "review_job_task")
class ReviewJobTaskEntity(
    @Column(name = "id", nullable = false)
    @Generated(event = [EventType.INSERT])
    var id: Long? = null,

    @Column(name = "resource_id", unique = true, nullable = false, length = 64)
    @Id
    var resourceId: String = "",

    @Column(name = "review_job_id", nullable = false, length = 64)
    var reviewJobId: String = "",

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var taskStatus: ReviewStatus = ReviewStatus.PENDING,

    @Column(name = "reviewer_id", length = 64)
    var reviewerId: Long? = null,

    @Column(name = "detail", length = 16777215)
    var detail: String? = null,

    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private var updateTime: OffsetDateTime = OffsetDateTime.now()
) : DataEntity<String> {
    override fun getSystemResourceKind(): SystemResourceKind =
        ReviewJobTaskResourceKind

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    fun setCreateTime(createTime: OffsetDateTime) {
        this.createTime = createTime
    }

    fun setUpdateTime(updateTime: OffsetDateTime) {
        this.updateTime = updateTime
    }

    override fun getEntityId(): String? = resourceId

    fun toBuilder(): Builder {
        return Builder(this)
    }

    fun lock(): ReviewJobTask {
        return ReviewJobTask(
            id!!,
            resourceId,
            reviewJobId,
            taskStatus,
            reviewerId,
            detail,
            createTime,
            updateTime
        )
    }

    class Builder {
        private var id: Long? = null
        private var resourceId: String? = null
        private var reviewJobId: String? = null
        private var taskStatus: ReviewStatus = ReviewStatus.PENDING
        private var reviewerId: Long? = null
        private var detail: String? = null
        private var createTime: OffsetDateTime = OffsetDateTime.now()
        private var updateTime: OffsetDateTime = OffsetDateTime.now()

        constructor()

        constructor(other: ReviewJobTaskEntity) {
            this.id = other.id
            this.resourceId = other.resourceId
            this.reviewJobId = other.reviewJobId
            this.taskStatus = other.taskStatus
            this.reviewerId = other.reviewerId
            this.detail = other.detail
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

        fun setTaskStatus(taskStatus: ReviewStatus) = apply {
            this.taskStatus = taskStatus
        }

        fun setReviewerId(reviewerId: Long?) = apply {
            this.reviewerId = reviewerId
        }

        fun setDetail(detail: String?) = apply {
            this.detail = detail
        }

        fun setCreateTime(createTime: OffsetDateTime) = apply {
            this.createTime = createTime
        }

        fun setUpdateTime(updateTime: OffsetDateTime) = apply {
            this.updateTime = updateTime
        }

        fun build(): ReviewJobTaskEntity {
            return ReviewJobTaskEntity(
                id,
                resourceId = resourceId!!,
                reviewJobId = reviewJobId!!,
                taskStatus = taskStatus,
                reviewerId = reviewerId,
                detail = detail,
                createTime = createTime,
                updateTime = updateTime
            )
        }
    }

    companion object {
        @JvmStatic
        fun ReviewJobTask.toDo(): ReviewJobTaskEntity = ReviewJobTaskEntity(
            id,
            resourceId = taskId,
            reviewJobId = reviewJobId,
            taskStatus = taskStatus,
            reviewerId = reviewerId,
            detail = detail,
            createTime = createTime,
            updateTime = updateTime
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
