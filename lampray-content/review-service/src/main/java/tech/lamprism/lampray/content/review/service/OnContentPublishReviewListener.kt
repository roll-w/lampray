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
import org.slf4j.info
import org.slf4j.logger
import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.ContentStatus
import tech.lamprism.lampray.content.publish.ContentPublishListener
import tech.lamprism.lampray.content.review.ReviewJobCreator
import tech.lamprism.lampray.content.review.ReviewMark
import tech.lamprism.lampray.content.review.ReviewStatus
import tech.lamprism.lampray.content.review.common.NotReviewedException

/**
 * Listener that creates review jobs when content is published.
 *
 * @author RollW
 */
@Component
class OnContentPublishReviewListener(
    private val reviewJobCreator: ReviewJobCreator
) : ContentPublishListener {

    override fun onPublish(contentDetails: ContentDetails): ContentStatus {
        try {
            val reviewInfo = reviewJobCreator.createReviewJob(contentDetails, ReviewMark.NORMAL)
            return when (reviewInfo.status) {
                ReviewStatus.PENDING -> ContentStatus.REVIEWING
                ReviewStatus.APPROVED -> ContentStatus.PUBLISHED
                ReviewStatus.REJECTED -> ContentStatus.FORBIDDEN
                ReviewStatus.CANCELED -> ContentStatus.FORBIDDEN
            }
        } catch (e: NotReviewedException) {
            logger.info {
                "Review job already exists for content: ${contentDetails.contentId}@${contentDetails.contentType}, " +
                        "job: ${e.reviewInfo.jobId}"
            }
            return ContentStatus.REVIEWING
        }
    }

    companion object {
        private val logger: Logger = logger<OnContentPublishReviewListener>()
    }
}