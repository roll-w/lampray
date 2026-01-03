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

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.review.ReviewFeedback
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.ReviewerAllocator
import tech.lamprism.lampray.content.review.autoreview.AutoReviewContext
import tech.lamprism.lampray.content.review.autoreview.AutoReviewService
import tech.lamprism.lampray.content.review.autoreview.reviewer.AutoReviewer
import tech.lamprism.lampray.content.review.service.ReviewStatusService
import java.util.concurrent.Executor

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

    override fun joinAutoReviewQueue(reviewJob: ReviewJobDetails, contentDetails: ContentDetails) {
        val autoReviewContext = AutoReviewContext(reviewJob, TODO(), contentDetails)
        val autoReviewers = autoReviewers
        if (autoReviewers.isEmpty()) {
            pass(autoReviewContext)
            return
        }
        startAutoReviewTask(autoReviewContext)
    }

    private fun startAutoReviewTask(autoReviewContext: AutoReviewContext) {
        autoReviewers.forEach {
            it.review(autoReviewContext.reviewJob, autoReviewContext)
        }
    }

    private fun pass(autoReviewContext: AutoReviewContext) {
        val reviewJob = autoReviewContext.reviewJob
        reviewStatusService.makeReview(
            reviewJob.jobId,
            autoReviewContext.reviewTask.taskId,
            ReviewerAllocator.AUTO_REVIEWER,
            ReviewFeedback.approved()
        )
    }

}