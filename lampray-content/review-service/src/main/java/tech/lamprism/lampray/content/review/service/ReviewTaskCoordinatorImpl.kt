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
package tech.lamprism.lampray.content.review.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import tech.lamprism.lampray.common.data.ResourceIdGenerator
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.ReviewTaskAction
import tech.lamprism.lampray.content.review.ReviewTaskCoordinator
import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.ReviewTaskResourceKind
import tech.lamprism.lampray.content.review.feedback.ReviewVerdict
import tech.lamprism.lampray.content.review.common.ReviewException
import tech.lamprism.lampray.content.review.event.OnReviewStateChangeEvent
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository
import tech.lamprism.lampray.content.review.persistence.ReviewTaskEntity
import tech.lamprism.lampray.content.review.persistence.ReviewTaskRepository
import tech.rollw.common.web.CommonErrorCode
import java.time.OffsetDateTime

/**
 * Implementation of review task coordinator that manages task lifecycle,
 * assignments, feedback collection, and job status transitions.
 *
 * This coordinator automatically updates the parent job status based on task results:
 * - Job is APPROVED when ALL active tasks are approved
 * - Job is REJECTED if ANY active task is rejected
 * - Job remains PENDING if there are still pending tasks
 * - Once job is in terminal state, new feedbacks are rejected (create new job for appeals)
 *
 * @author RollW
 */
@Component
class ReviewTaskCoordinatorImpl(
    private val reviewTaskRepository: ReviewTaskRepository,
    private val reviewJobRepository: ReviewJobRepository,
    private val resourceIdGenerator: ResourceIdGenerator,
    private val eventPublisher: ApplicationEventPublisher
) : ReviewTaskCoordinator {

    private val logger: Logger = LoggerFactory.getLogger(ReviewTaskCoordinatorImpl::class.java)

    override fun reassignTask(
        taskId: String,
        currentReviewerId: Long,
        newReviewerId: Long,
        reason: String?
    ): ReviewTaskDetails {
        val currentTask = findReviewTask(taskId)

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
        val taskEntity = findReviewTask(taskId)

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
        val taskEntity = findReviewTask(taskId)

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
        val taskEntity = findReviewTask(taskId)
        val jobId = taskEntity.reviewJobId

        // Check if job is still in valid state
        val job = reviewJobRepository.findById(jobId).orElseThrow {
            ReviewException(CommonErrorCode.ERROR_NOT_FOUND, "Review job not found: $jobId")
        }

        if (job.status.isFinished()) {
            throw ReviewException(
                CommonErrorCode.ERROR_NOT_FOUND,
                "Cannot submit feedback: job $jobId is in terminal state ${job.status}. " +
                "Create a new review job for appeals."
            )
        }

        require(taskEntity.reviewerId == reviewerId) {
            "Task is not assigned to reviewer $reviewerId"
        }
        require(taskEntity.status == ReviewStatus.PENDING) {
            "Task is already reviewed"
        }

        // Update task with feedback
        taskEntity.feedback = feedback
        taskEntity.status = feedback.verdict.toReviewStatus()
        taskEntity.setUpdateTime(OffsetDateTime.now())

        val updated = reviewTaskRepository.save(taskEntity)
        logger.info(
            "Feedback submitted for task {} by reviewer {} with verdict {}",
            taskId, reviewerId, feedback.verdict
        )

        // Update the main job status based on all tasks
        updateJobStatusAfterTaskChange(job)

        return updated.lock()
    }

    /**
     * Updates the job status based on all its tasks and publishes state change event if changed.
     */
    private fun updateJobStatusAfterTaskChange(job: ReviewJobEntity) {
        // If job is already in terminal state, don't change it
        if (job.status.isFinished()) {
            logger.debug(
                "Job {} is already in terminal state {}, skipping status update",
                job.resourceId, job.status
            )
            return
        }

        val previousStatus = job.status
        val tasks = reviewTaskRepository.findTasksByJobId(job.resourceId)
        val newStatus = determineJobStatus(tasks.map { it.lock() })

        if (newStatus != previousStatus) {
            job.status = newStatus
            job.updateTime = OffsetDateTime.now()
            reviewJobRepository.save(job)

            logger.info(
                "Job {} status updated from {} to {} based on {} tasks",
                job.resourceId, previousStatus, newStatus, tasks.size
            )

            // Publish state change event
            try {
                val event = OnReviewStateChangeEvent(
                    job.lock(),
                    previousStatus,
                    newStatus
                )
                eventPublisher.publishEvent(event)
                logger.debug("Published OnReviewStateChangeEvent for job {}", job.resourceId)
            } catch (e: Exception) {
                logger.error(
                    "Failed to publish state change event for job {}: {}",
                    job.resourceId, e.message, e
                )
            }
        }
    }

    /**
     * Determines the job status based on all task results.
     *
     * Rules:
     * - APPROVED: All active tasks are approved
     * - REJECTED: Any active task is rejected
     * - PENDING: Has pending tasks
     * - CANCELED: All tasks are canceled
     */
    private fun determineJobStatus(tasks: List<ReviewTaskDetails>): ReviewStatus {
        if (tasks.isEmpty()) {
            return ReviewStatus.PENDING
        }

        val activeTasks = tasks.filter { it.status != ReviewStatus.CANCELED }
        if (activeTasks.isEmpty()) {
            // All tasks canceled - job should be canceled
            return ReviewStatus.CANCELED
        }

        // Check if any task is rejected
        val hasRejected = activeTasks.any { task ->
            task.status == ReviewStatus.REJECTED ||
            task.feedback?.verdict == ReviewVerdict.REJECTED
        }

        if (hasRejected) {
            return ReviewStatus.REJECTED
        }

        // Check if any task still pending
        val hasPending = activeTasks.any { it.status == ReviewStatus.PENDING }
        if (hasPending) {
            return ReviewStatus.PENDING
        }

        // All tasks approved
        val allApproved = activeTasks.all { task ->
            task.status == ReviewStatus.APPROVED ||
            task.feedback?.verdict == ReviewVerdict.APPROVED
        }

        return if (allApproved) {
            ReviewStatus.APPROVED
        } else {
            // Some tasks may need revision
            ReviewStatus.PENDING
        }
    }

    /**
     * Validates if a new task can be created for this job.
     */
    private fun validateCanCreateTask(jobId: String) {
        val job = reviewJobRepository.findById(jobId).orElse(null)
        if (job != null && job.status.isFinished) {
            throw ReviewException(
                CommonErrorCode.ERROR_NOT_FOUND,
                "Cannot create new task for job $jobId: job is in terminal state ${job.status}. " +
                "Create a new review job for appeals or re-review."
            )
        }
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
        val taskEntity = findReviewTask(taskId)

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
                taskEntity.status == ReviewStatus.PENDING
            }

            ReviewTaskAction.CANCEL -> {
                taskEntity.reviewerId == reviewerId &&
                        taskEntity.status == ReviewStatus.PENDING
            }
        }
    }

    override fun createTasksForReviewers(
        reviewJobId: String,
        reviewerIds: List<Long>
    ): List<ReviewTaskDetails> {
        if (reviewerIds.isEmpty()) {
            return emptyList()
        }

        val tasks = reviewerIds.map { reviewerId ->
            createTaskEntity(reviewJobId, reviewerId)
        }

        val savedTasks = reviewTaskRepository.saveAll(tasks)
        logger.info(
            "Created {} review tasks for job {} assigned to reviewers: {}",
            savedTasks.size, reviewJobId, reviewerIds.joinToString()
        )

        return savedTasks.map { it.lock() }
    }

    override fun createTask(
        reviewJobId: String,
        reviewerId: Long
    ): ReviewTaskDetails {
        // Validate job can accept new tasks
        validateCanCreateTask(reviewJobId)

        val taskEntity = createTaskEntity(reviewJobId, reviewerId)
        val savedTask = reviewTaskRepository.save(taskEntity)

        logger.info(
            "Created review task {} for job {} assigned to reviewer {}",
            savedTask.resourceId, reviewJobId, reviewerId
        )

        return savedTask.lock()
    }

    private fun createTaskEntity(reviewJobId: String, reviewerId: Long): ReviewTaskEntity {
        val now = OffsetDateTime.now()
        return ReviewTaskEntity.builder()
            .setResourceId(resourceIdGenerator.nextId(ReviewTaskResourceKind))
            .setReviewJobId(reviewJobId)
            .setTaskStatus(ReviewStatus.PENDING)
            .setReviewerId(reviewerId)
            .setCreateTime(now)
            .setUpdateTime(now)
            .build()
    }

    private fun findReviewTask(taskId: String): ReviewTaskEntity {
        return reviewTaskRepository.findById(taskId).orElseThrow {
            ReviewException(CommonErrorCode.ERROR_NOT_FOUND, "Review task not found: $taskId")
        }
    }
}

