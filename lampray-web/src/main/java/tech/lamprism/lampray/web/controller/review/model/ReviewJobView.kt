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

package tech.lamprism.lampray.web.controller.review.model

import tech.lamprism.lampray.content.ContentType
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.ReviewMark
import tech.lamprism.lampray.content.review.ReviewStatus
import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class ReviewJobView(
    val id: Long,
    val status: ReviewStatus,
    val result: String,
    val reviewer: Long,
    val operator: Long?,
    val contentType: ContentType,
    val contentId: Long,
    val reviewMark: ReviewMark,
    val assignedTime: OffsetDateTime,
    val reviewTime: OffsetDateTime
) {

    companion object {
        @JvmStatic
        fun from(reviewJobDetails: ReviewJobDetails): ReviewJobView {
            return ReviewJobView(
                id = reviewJobDetails.jobId,
                status = reviewJobDetails.status,
                result = reviewJobDetails.result,
                reviewer = reviewJobDetails.reviewer,
                operator = reviewJobDetails.operator,
                contentType = reviewJobDetails.associatedContent.contentType,
                contentId = reviewJobDetails.associatedContent.contentId,
                reviewMark = reviewJobDetails.reviewMark,
                assignedTime = reviewJobDetails.assignedTime,
                reviewTime = reviewJobDetails.reviewTime
            )
        }
    }
}
