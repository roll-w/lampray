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

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.review.AutoReviewContext
import tech.lamprism.lampray.content.review.AutoReviewContext.Verdict
import tech.lamprism.lampray.content.review.AutoReviewService
import tech.lamprism.lampray.content.review.AutoReviewer
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.ReviewerAllocator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author RollW
 */
@Service
class AutoReviewerServiceImpl(
    private val reviewStatusService: ReviewStatusService,
    override val autoReviewers: List<AutoReviewer>,
    @Qualifier("mainScheduledExecutorService")
    private val executor: Executor
) : AutoReviewService {

    companion object {
        private const val DEFAULT_TIMEOUT_MS: Long = 5_000L
    }

    override fun joinAutoReviewQueue(reviewJob: ReviewJobDetails, contentDetails: ContentDetails) {
        val autoReviewContext = AutoReviewContext(reviewJob, contentDetails)
        val autoReviewers = autoReviewers
        if (autoReviewers.isEmpty()) {
            pass(autoReviewContext)
            return
        }
        startAutoReviewTask(autoReviewContext)
    }

    private fun startAutoReviewTask(autoReviewContext: AutoReviewContext) {
        // Submit all reviewers concurrently and wait for each with per-reviewer timeout.
        val futures = autoReviewers.map { reviewer ->
            CompletableFuture.runAsync({
                val start = System.currentTimeMillis()
                try {
                    reviewer.review(autoReviewContext.reviewJob, autoReviewContext)
                    val duration = System.currentTimeMillis() - start
                    // If reviewer didn't explicitly record an outcome, mark as APPROVE by default
                    if (!autoReviewContext.hasOutcomeFrom(reviewer)) {
                        autoReviewContext.addOutcome(reviewer, Verdict.APPROVE, null, duration)
                    } else {
                        // update duration for existing outcome if desired (not implemented here)
                    }
                } catch (ex: Throwable) {
                    val duration = System.currentTimeMillis() - start
                    autoReviewContext.addOutcome(reviewer, Verdict.FAILED, ex.message ?: "error", duration)
                }
            }, executor)
        }

        // Wait for each future with timeout; if timeout occurs, cancel and record TIMEOUT
        futures.forEachIndexed { idx, cf ->
            val reviewer = autoReviewers[idx]
            try {
                cf.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                cf.cancel(true)
                autoReviewContext.addOutcome(reviewer, Verdict.TIMEOUT, "timeout", DEFAULT_TIMEOUT_MS)
            } catch (_: ExecutionException) {
                // Already handled inside runnable, ignore here
            } catch (ex: Throwable) {
                // record unexpected
                autoReviewContext.addOutcome(reviewer, Verdict.FAILED, ex.message ?: "error", 0)
            }
        }

        // After all reviewers finished/recorded outcomes, decide final result (collect all opinions)
        val reasons = autoReviewContext.getOutcomes()
            .filter { it.verdict == Verdict.REJECT || it.verdict == Verdict.FAILED || it.verdict == Verdict.TIMEOUT }
            .joinToString("; ") { outcome ->
                "${outcome.reviewer.reviewerInfo.name}: ${outcome.reason}"
            }

        if (autoReviewContext.isApproved()) {
            pass(autoReviewContext)
            return
        }

        reviewStatusService.makeReview(
            autoReviewContext.reviewJob.jobId,
            ReviewerAllocator.AUTO_REVIEWER,
            false,
            reasons.ifBlank { "" }
        )
    }

    private fun pass(autoReviewContext: AutoReviewContext) {
        val reviewJob = autoReviewContext.reviewJob
        reviewStatusService.makeReview(
            reviewJob.jobId,
            ReviewerAllocator.AUTO_REVIEWER,
            true,
            null
        )
    }

}
