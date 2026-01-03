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
package tech.lamprism.lampray.content.review.autoreview

import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.autoreview.reviewer.AutoReviewer

/**
 * Orchestrator for automated review processes.
 * Coordinates multiple auto-reviewers to analyze content and produce feedback.
 *
 * @author RollW
 */
interface AutoReviewOrchestrator {
    /**
     * Start automated review process for the given review job and content.
     * This will execute all registered auto-reviewers and collect their feedback.
     *
     * @param reviewJob the review job details
     * @param contentDetails the content to be reviewed
     */
    fun executeAutoReview(reviewJob: ReviewJobDetails, contentDetails: ContentDetails)

    /**
     * Get all registered auto-reviewers.
     */
    val autoReviewers: List<AutoReviewer>
}
