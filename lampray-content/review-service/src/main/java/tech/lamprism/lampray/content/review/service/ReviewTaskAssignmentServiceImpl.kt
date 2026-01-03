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
package tech.lamprism.lampray.content.review.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tech.lamprism.lampray.common.data.ResourceIdGenerator
import tech.lamprism.lampray.content.review.ReviewFeedback
import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.ReviewTaskResourceKind
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.ReviewTaskAction
import tech.lamprism.lampray.content.review.ReviewTaskAssignmentService
import tech.lamprism.lampray.content.review.ReviewVerdict
import tech.lamprism.lampray.content.review.persistence.ReviewTaskEntity
import tech.lamprism.lampray.content.review.persistence.ReviewTaskRepository
import java.time.OffsetDateTime

/**
 * Implementation of review task assignment service that uses task cancellation
 * and creation for reassignment to maintain audit trail.
 *
 * @author RollW
 */
@Service
class ReviewTaskAssignmentServiceImpl(
    private val reviewTaskRepository: ReviewTaskRepository,
    private val resourceIdGenerator: ResourceIdGenerator
) : ReviewTaskAssignmentService {

    private val logger = LoggerFactory.getLogger(ReviewTaskAssignmentServiceImpl::class.java)

    override fun reassignTask(
        taskId: String,
        currentReviewerId: Long,
        newReviewerId: Long,
        reason: String?
    ): ReviewTaskDetails {
        val currentTask = findReviewJobTask(taskId)

        require(currentTask.reviewerId == currentReviewerId) {
            "Task is not assigned to reviewer $currentReviewerId"
        }
        require(currentTask.status == ReviewStatus.PENDING) {
            "Only pending tasks can be reassigned"
        }

        // Cancel the current task
        currentTask.status = ReviewStatus.CANCELED
        currentTask.setUpdateTime(OffsetDateTime.now())
        if (reason != null) {
            // Store reason in feedback for audit trail
            currentTask.feedback = ReviewFeedback(
                verdict = ReviewVerdict.PENDING,
                summary = "Task reassigned: $reason"
            )
        }
        reviewTaskRepository.save(currentTask)

        // Create new task for the new reviewer
        val newTask = ReviewTaskEntity.builder()
            .setResourceId(resourceIdGenerator.nextId(ReviewTaskResourceKind))
            .setReviewJobId(currentTask.reviewJobId)
            .setTaskStatus(ReviewStatus.PENDING)
            .setReviewerId(newReviewerId)
            .setCreateTime(OffsetDateTime.now())
            .setUpdateTime(OffsetDateTime.now())
            .build()

        val savedTask = reviewTaskRepository.save(newTask)
        logger.info(
            "Task {} reassigned from reviewer {} to {} as new task {} (reason: {})",
            taskId, currentReviewerId, newReviewerId, savedTask.resourceId, reason ?: "none"
        )

        return savedTask.lock()
    }

    override fun returnTaskForReassignment(
        taskId: String,
        reviewerId: Long,
        reason: String
    ): ReviewTaskDetails {
        val taskEntity = findReviewJobTask(taskId)

        require(taskEntity.reviewerId == reviewerId) {
            "Task is not assigned to reviewer $reviewerId"
        }
        require(taskEntity.status == ReviewStatus.PENDING) {
            "Only pending tasks can be returned"
        }

        // Cancel the task and store reason
        taskEntity.apply {
            status = ReviewStatus.CANCELED
            feedback = ReviewFeedback(
                verdict = ReviewVerdict.PENDING,
                summary = "Task returned for reassignment: $reason"
            )
            updateTime = OffsetDateTime.now()
        }

        val updated = reviewTaskRepository.save(taskEntity)
        logger.info(
            "Task {} returned by reviewer {} for reassignment (reason: {})",
            taskId, reviewerId, reason
        )

        return updated.lock()
    }

    override fun claimTask(
        taskId: String,
        reviewerId: Long
    ): ReviewTaskDetails {
        val taskEntity = findReviewJobTask(taskId)

        require(taskEntity.reviewerId == null) {
            "Task is already assigned to reviewer ${taskEntity.reviewerId}"
        }
        require(taskEntity.status == ReviewStatus.PENDING) {
            "Only pending tasks can be claimed"
        }

        taskEntity.reviewerId = reviewerId
        taskEntity.setUpdateTime(OffsetDateTime.now())

        val updated = reviewTaskRepository.save(taskEntity)
        logger.info("Task {} claimed by reviewer {}", taskId, reviewerId)

        return updated.lock()
    }

    override fun submitFeedback(
        taskId: String,
        reviewerId: Long,
        feedback: ReviewFeedback
    ): ReviewTaskDetails {
        val taskEntity = findReviewJobTask(taskId)

        require(taskEntity.reviewerId == reviewerId) {
            "Task is not assigned to reviewer $reviewerId"
        }
        require(taskEntity.status == ReviewStatus.PENDING) {
            "Task is already reviewed"
        }

        taskEntity.feedback = feedback
        taskEntity.status = when (feedback.verdict) {
            ReviewVerdict.APPROVED -> ReviewStatus.APPROVED
            ReviewVerdict.REJECTED -> ReviewStatus.REJECTED
            ReviewVerdict.NEEDS_REVISION -> ReviewStatus.REJECTED
            ReviewVerdict.PENDING -> ReviewStatus.PENDING
        }
        taskEntity.setUpdateTime(OffsetDateTime.now())

        val updated = reviewTaskRepository.save(taskEntity)
        logger.info(
            "Feedback submitted for task {} by reviewer {} with verdict {}",
            taskId, reviewerId, feedback.verdict
        )

        return updated.lock()
    }

    override fun getTasksForReviewJob(reviewJobId: String): List<ReviewTaskDetails> {
        return reviewTaskRepository.findTasksByJobId(reviewJobId)
            .map { it.lock() }
    }

    override fun canPerformAction(
        taskId: String,
        reviewerId: Long,
        action: ReviewTaskAction
    ): Boolean {
        val taskEntity = findReviewJobTask(taskId)

        return when (action) {
            ReviewTaskAction.SUBMIT -> {
                taskEntity.reviewerId == reviewerId &&
                        taskEntity.status == ReviewStatus.PENDING
            }
            ReviewTaskAction.REASSIGN -> {
                taskEntity.reviewerId == reviewerId &&
                        taskEntity.status == ReviewStatus.PENDING
            }
            ReviewTaskAction.RETURN_FOR_REASSIGNMENT -> {
                taskEntity.reviewerId == reviewerId &&
                        taskEntity.status == ReviewStatus.PENDING
            }
            ReviewTaskAction.CLAIM -> {
                taskEntity.reviewerId == null &&
                        taskEntity.status == ReviewStatus.PENDING
            }
            ReviewTaskAction.CANCEL -> {
                taskEntity.reviewerId == reviewerId &&
                        taskEntity.status == ReviewStatus.PENDING
            }
        }
    }

    private fun findReviewJobTask(taskId: String): ReviewTaskEntity {
        return reviewTaskRepository.findById(taskId).orElseThrow {
            IllegalArgumentException("Review task not found: $taskId")
        }
    }
}

