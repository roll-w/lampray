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

import org.slf4j.Logger
import org.slf4j.info
import org.slf4j.logger
import org.springframework.stereotype.Service
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentStatus
import tech.lamprism.lampray.content.publish.ContentPublishListener
import tech.lamprism.lampray.content.review.ReviewJobInfo
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.common.NotReviewedException

/**
 * @author RollW
 */
@Service
class OnContentPublishReviewListener(
    private val reviewService: ReviewService
) : ContentPublishListener {
    override fun onPublish(contentDetails: ContentDetails): ContentStatus {
        try {
            val reviewInfo = tryAssignReviewer(contentDetails)
            return when (reviewInfo.status) {
                ReviewStatus.PENDING -> ContentStatus.REVIEWING
                ReviewStatus.APPROVED -> ContentStatus.PUBLISHED
                ReviewStatus.REJECTED -> ContentStatus.FORBIDDEN
                ReviewStatus.CANCELED -> ContentStatus.FORBIDDEN
            }
        } catch (e: NotReviewedException) {
            logger.info {
                "Already assigned reviewer for content: ${contentDetails.contentId}@${contentDetails.contentType}, " +
                        "reviewer: ${e.reviewInfo.reviewer}"
            }
            return ContentStatus.REVIEWING
        }
    }

    private fun tryAssignReviewer(contentDetails: ContentDetails): ReviewJobInfo {
        val reviewInfo = reviewService.assignReviewer(contentDetails)
        logger.info {
            "Assign reviewer for content: ${contentDetails.contentId}@${contentDetails.contentType}, " +
                    "reviewer: ${reviewInfo.reviewer}"
        }
        return reviewInfo
    }

    companion object {
        private val logger: Logger = logger<OnContentPublishReviewListener>()
    }
}