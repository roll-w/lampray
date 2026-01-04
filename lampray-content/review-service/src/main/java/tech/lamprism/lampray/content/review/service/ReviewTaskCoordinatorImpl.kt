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
import org.slf4j.debug
import org.slf4j.error
import org.slf4j.info
import org.slf4j.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import tech.lamprism.lampray.common.data.ResourceIdGenerator
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.ReviewTaskAction
import tech.lamprism.lampray.content.review.ReviewTaskCoordinator
import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.ReviewTaskResourceKind
import tech.lamprism.lampray.content.review.common.ReviewException
import tech.lamprism.lampray.content.review.event.OnReviewStateChangeEvent
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback
import tech.lamprism.lampray.content.review.feedback.ReviewVerdict
import tech.lamprism.lampray.content.review.persistence.ReviewJobEntity
import tech.lamprism.lampray.content.review.persistence.ReviewJobRepository
import tech.lamprism.lampray.content.review.persistence.ReviewTaskEntity
import tech.lamprism.lampray.content.review.persistence.ReviewTaskRepository
import tech.rollw.common.web.CommonErrorCode
import java.time.OffsetDateTime

private val logger: Logger = logger<ReviewTaskCoordinatorImpl>()

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
        val now = OffsetDateTime.now()

        currentTask.apply {
            status = ReviewStatus.CANCELED
            updateTime = now
            if (reason != null) {
                // Store reason in feedback for audit trail
                feedback = ReviewFeedback(
                    verdict = ReviewVerdict.PENDING,
                    summary = "Task reassigned: $reason"
                )
            }
        }

        reviewTaskRepository.save(currentTask)

        // Create new task for the new reviewer
        val newTask = ReviewTaskEntity.builder()
            .setResourceId(resourceIdGenerator.nextId(ReviewTaskResourceKind))
            .setReviewJobId(currentTask.reviewJobId)
            .setTaskStatus(ReviewStatus.PENDING)
            .setReviewerId(newReviewerId)
            .setCreateTime(now)
            .setUpdateTime(now)
            .build()

        val savedTask = reviewTaskRepository.save(newTask)
        logger.info {
            "Task $taskId reassigned from reviewer $currentReviewerId to $newReviewerId " +
                    "as new task ${savedTask.resourceId} (reason: ${reason ?: "none"})"
        }

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

        // TODO: maybe create a new task instead
        taskEntity.apply {
            this.reviewerId = reviewerId
            updateTime = OffsetDateTime.now()
        }

        val updated = reviewTaskRepository.save(taskEntity)
        logger.info {
            "Task $taskId claimed by reviewer $reviewerId"
        }
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

        if (job.status.isFinished) {
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
        logger.info {
            "Feedback submitted for task ${taskEntity.resourceId} by reviewer $reviewerId with verdict ${feedback.verdict}"
        }

        // Update the main job status based on all tasks
        updateJobStatusAfterTaskChange(job)

        return updated.lock()
    }

    /**
     * Updates the job status based on all its tasks and publishes state change event if changed.
     */
    private fun updateJobStatusAfterTaskChange(job: ReviewJobEntity) {
        // If job is already in terminal state, don't change it
        if (job.status.isFinished) {
            logger.debug {
                "Job ${job.resourceId} is already in terminal state ${job.status}, skipping status update"
            }
            return
        }

        val previousStatus = job.status
        val tasks = reviewTaskRepository.findByJobId(job.resourceId)
        val newStatus = determineJobStatus(tasks)

        if (newStatus == previousStatus) {
            return
        }
        job.status = newStatus
        job.updateTime = OffsetDateTime.now()
        reviewJobRepository.save(job)

        logger.info {
            "Job ${job.resourceId} status changed: $previousStatus -> $newStatus based on ${tasks.size} tasks"
        }

        try {
            val event = OnReviewStateChangeEvent(
                job.lock(),
                previousStatus,
                newStatus
            )
            eventPublisher.publishEvent(event)
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to publish state change event for job ${job.resourceId}: ${e.message}"
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
            // All tasks canceled, job should be canceled
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
        if (job == null || !job.status.isFinished) {
            return
        }
        throw ReviewException(
            CommonErrorCode.ERROR_NOT_FOUND,
            "Cannot create new task for job $jobId: job is in terminal state ${job.status}. " +
                    "Create a new review job for appeals or re-review."
        )
    }

    override fun getTasksForReviewJob(reviewJobId: String): List<ReviewTaskDetails> {
        return reviewTaskRepository.findByJobId(reviewJobId)
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
        logger.info {
            "Created ${savedTasks.count()} review tasks for job $reviewJobId assigned to reviewers: ${reviewerIds.joinToString()}"
        }

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

        logger.info {
            "Created review task ${savedTask.resourceId} for job $reviewJobId assigned to reviewer $reviewerId"
        }

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

