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
data class ReviewJobDetailsView(
    val id: String,
    val status: ReviewStatus,
    val contentType: ContentType,
    val contentId: Long,
    val reviewMark: ReviewMark,
    val createTime: OffsetDateTime,
    val updateTime: OffsetDateTime,
    val tasks: List<ReviewTaskView> = emptyList()
) {

    companion object {
        @JvmStatic
        fun from(reviewJobDetails: ReviewJobDetails): ReviewJobDetailsView {
            return ReviewJobDetailsView(
                id = reviewJobDetails.jobId,
                status = reviewJobDetails.status,
                contentType = reviewJobDetails.associatedContent.contentType,
                contentId = reviewJobDetails.associatedContent.contentId,
                reviewMark = reviewJobDetails.reviewMark,
                createTime = reviewJobDetails.createTime,
                updateTime = reviewJobDetails.updateTime,
                tasks = reviewJobDetails.tasks.map { ReviewTaskView.from(it) }
            )
        }
    }
}
