/*
 * Copyright (C) 2023 RollW
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
import tech.lamprism.lampray.content.review.ReviewJobContent
import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class ReviewJobContentView(
    val contentId: Long,
    val contentType: ContentType,
    val title: String?,
    val content: String,
    val userId: Long,
    val createTime: OffsetDateTime,
    val updateTime: OffsetDateTime,
) {
    companion object {
        @JvmStatic
        fun of(reviewJobContent: ReviewJobContent): ReviewJobContentView {
            return ReviewJobContentView(
                contentId = reviewJobContent.contentDetails.contentId,
                contentType = reviewJobContent.contentDetails.contentType,
                title = reviewJobContent.contentDetails.title,
                content = reviewJobContent.contentDetails.content,
                userId = reviewJobContent.contentDetails.userId,
                createTime = reviewJobContent.contentDetails.createTime,
                updateTime = reviewJobContent.contentDetails.updateTime,
            )
        }
    }
}