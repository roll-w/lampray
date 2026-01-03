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

package tech.lamprism.lampray.content.review.autoreview.service

import org.slf4j.debug
import org.slf4j.error
import org.slf4j.info
import org.slf4j.logger
import org.slf4j.warn
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.ReviewTaskCoordinator
import tech.lamprism.lampray.content.review.ReviewerAllocator
import tech.lamprism.lampray.content.review.autoreview.AutoReviewContext
import tech.lamprism.lampray.content.review.autoreview.AutoReviewOrchestrator
import tech.lamprism.lampray.content.review.autoreview.reviewer.AutoReviewer
import java.util.concurrent.Executor

private val logger = logger<AutoReviewOrchestratorImpl>()

/**
 * Implementation of AutoReviewOrchestrator that coordinates automated content review.
 * Creates auto-review task, executes all registered reviewers, and submits consolidated feedback.
 *
 * @author RollW
 */
@Component
class AutoReviewOrchestratorImpl(
    private val reviewTaskCoordinator: ReviewTaskCoordinator,
    override val autoReviewers: List<AutoReviewer>,
    @Qualifier("mainScheduledExecutorService")
    private val executor: Executor
) : AutoReviewOrchestrator {

    override fun executeAutoReview(reviewJob: ReviewJobDetails, contentDetails: ContentDetails) {
        if (autoReviewers.isEmpty()) {
            logger.warn { "No auto-reviewers registered, skipping auto-review for job ${reviewJob.jobId}" }
            return
        }

        try {
            // Create a task for the auto-reviewer (validation is done inside)
            val autoReviewTask = reviewTaskCoordinator.createTask(
                reviewJob.jobId,
                ReviewerAllocator.AUTO_REVIEWER
            )

            val autoReviewContext = AutoReviewContext(reviewJob, autoReviewTask, contentDetails)
            executor.execute {
                executeAutoReviewProcess(autoReviewContext)
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Failed to create auto-review task for job ${reviewJob.jobId}: ${e.message}" }
        }
    }

    private fun executeAutoReviewProcess(autoReviewContext: AutoReviewContext) {
        try {
            logger.info {
                "Starting auto-review process for job ${autoReviewContext.reviewJob.jobId} with ${autoReviewers.size} reviewers"
            }

            // Execute all auto-reviewers
            autoReviewers.forEach { reviewer ->
                try {
                    reviewer.review(autoReviewContext.reviewJob, autoReviewContext)
                    logger.debug {
                        "Auto-reviewer '${reviewer.reviewerInfo.name}' completed for job ${autoReviewContext.reviewJob.jobId}"
                    }
                } catch (e: Exception) {
                    logger.error(e) {
                        "Auto-reviewer '${reviewer.reviewerInfo.name}' failed for job ${autoReviewContext.reviewJob.jobId}: ${e.message}"
                    }
                }
            }

            // Build and submit feedback
            val feedback = autoReviewContext.buildFeedback()
            if (feedback != null) {
                try {
                    reviewTaskCoordinator.submitFeedback(
                        autoReviewContext.reviewTask.taskId,
                        ReviewerAllocator.AUTO_REVIEWER,
                        feedback
                    )
                    logger.info {
                        "Auto-review completed for job ${autoReviewContext.reviewJob.jobId} with verdict: ${feedback.verdict}"
                    }
                } catch (e: Exception) {
                    logger.error(e) {
                        "Failed to submit auto-review feedback for job ${autoReviewContext.reviewJob.jobId}: ${e.message}"
                    }
                }
            } else {
                logger.warn {
                    "Auto-review completed for job ${autoReviewContext.reviewJob.jobId} but no feedback was generated"
                }
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Auto-review process failed for job ${autoReviewContext.reviewJob.jobId}: ${e.message}"
            }
        }
    }
}

