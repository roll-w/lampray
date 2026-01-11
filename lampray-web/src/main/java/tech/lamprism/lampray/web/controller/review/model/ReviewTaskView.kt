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

package tech.lamprism.lampray.web.controller.review.model

import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.ReviewTaskStatus
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback
import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class ReviewTaskView(
    val taskId: String,
    val reviewJobId: String,
    val status: ReviewTaskStatus,
    val reviewerId: Long,
    val feedback: ReviewFeedback?,
    val createTime: OffsetDateTime,
    val updateTime: OffsetDateTime
) {
    companion object {
        @JvmStatic
        fun from(taskDetails: ReviewTaskDetails): ReviewTaskView {
            return ReviewTaskView(
                taskId = taskDetails.taskId,
                reviewJobId = taskDetails.reviewJobId,
                status = taskDetails.status,
                reviewerId = taskDetails.reviewerId,
                feedback = taskDetails.feedback,
                createTime = taskDetails.createTime,
                updateTime = taskDetails.updateTime
            )
        }
    }
}

