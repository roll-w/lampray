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
package tech.lamprism.lampray.content.review

import tech.lamprism.lampray.content.ContentTrait
import tech.rollw.common.web.system.Operator

/**
 * @author RollW
 */
interface ReviewJobProvider {
    fun getReviewJob(reviewJobId: Long): ReviewJobDetails

    val reviewJobs: List<ReviewJobDetails>

    fun getReviewJobsByOperator(operator: Operator): List<ReviewJobDetails>

    fun getReviewJobsByReviewer(reviewer: Operator): List<ReviewJobDetails>

    fun getReviewJobs(
        reviewer: Operator,
        status: ReviewStatus
    ): List<ReviewJobDetails>

    fun getReviewJobs(
        reviewer: Operator,
        statues: ReviewStatues = ReviewStatues.ALL
    ): List<ReviewJobDetails>

    fun getReviewJobs(contentTrait: ContentTrait): List<ReviewJobDetails>

    fun getReviewJobs(reviewStatus: ReviewStatus): List<ReviewJobDetails>

    fun getReviewJobs(reviewStatues: ReviewStatues): List<ReviewJobDetails>
}
