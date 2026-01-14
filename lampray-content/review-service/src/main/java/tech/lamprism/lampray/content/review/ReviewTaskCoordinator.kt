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
package tech.lamprism.lampray.content.review

import tech.lamprism.lampray.content.review.feedback.ReviewFeedback

/**
 * Coordinator for managing review task assignments, transfers, and lifecycle operations.
 * Handles task distribution, reassignment, and feedback submission for review workflows.
 *
 * @author RollW
 */
interface ReviewTaskCoordinator {
    /**
     * Reassigns a review task to another reviewer by creating a new task
     * and canceling the current one. This approach maintains audit trail
     * and avoids performance issues with tracking assignment history.
     *
     * @param jobId the review job identifier for validation
     * @param taskId the current task identifier
     * @param currentReviewerId the current reviewer's ID
     * @param newReviewerId the new reviewer's ID to assign to
     * @param reason optional reason for reassignment
     * @return the newly created task details
     * @throws IllegalStateException if the task cannot be reassigned
     */
    fun reassignTask(
        jobId: String,
        taskId: String,
        currentReviewerId: Long,
        newReviewerId: Long,
        reason: String? = null
    ): ReviewTaskDetails

    /**
     * Returns a task for reassignment by canceling it, making it available
     * for another reviewer to claim or be reassigned.
     *
     * @param jobId the review job identifier for validation
     * @param taskId the task identifier
     * @param reviewerId the current reviewer's ID
     * @return the updated task details with canceled status
     * @throws IllegalStateException if the task cannot be returned
     */
    fun returnTask(
        jobId: String,
        taskId: String,
        reviewerId: Long
    ): ReviewTaskDetails

    /**
     * Claims an unassigned review task.
     *
     * @param jobId the review job identifier for validation
     * @param taskId the task identifier
     * @param reviewerId the reviewer's ID who is claiming the task
     * @return the updated task details
     * @throws IllegalStateException if the task cannot be claimed
     */
    fun claimTask(
        jobId: String,
        taskId: String,
        reviewerId: Long
    ): ReviewTaskDetails

    /**
     * Submits review feedback for a task.
     *
     * @param jobId the review job identifier for validation
     * @param taskId the task identifier
     * @param reviewerId the reviewer's ID
     * @param feedback the structured review feedback
     * @return the updated task details
     * @throws IllegalStateException if the feedback cannot be submitted
     */
    fun submitFeedback(
        jobId: String,
        taskId: String,
        reviewerId: Long,
        feedback: ReviewFeedback
    ): ReviewTaskDetails

    /**
     * Gets all tasks for a specific review job, including canceled and reassigned ones.
     * This provides the complete history of assignments.
     *
     * @param reviewJobId the review job identifier
     * @return list of all tasks for the review job
     */
    fun getTasksForReviewJob(reviewJobId: String): List<ReviewTaskDetails>

    /**
     * Checks if a reviewer can perform an action on a task.
     *
     * @param taskId the task identifier
     * @param reviewerId the reviewer's ID
     * @param action the action to check
     * @return true if the action is allowed, false otherwise
     */
    fun canPerformAction(
        taskId: String,
        reviewerId: Long,
        action: ReviewTaskAction
    ): Boolean

    /**
     * Creates review tasks for multiple reviewers for a single review job.
     * This is the primary method for distributing review work.
     *
     * @param reviewJobId the review job identifier
     * @param reviewerIds list of reviewer IDs to assign tasks to
     * @return list of created task details
     */
    fun createTasksForReviewers(
        reviewJobId: String,
        reviewerIds: List<Long>
    ): List<ReviewTaskDetails>

    /**
     * Creates a single review task for a specific reviewer.
     *
     * @param reviewJobId the review job identifier
     * @param reviewerId the reviewer ID, can be AUTO_REVIEWER constant for automated review
     * @return created task details
     */
    fun createTask(
        reviewJobId: String,
        reviewerId: Long
    ): ReviewTaskDetails
}

