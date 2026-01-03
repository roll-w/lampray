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

import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback

/**
 * Details of a review job task.
 *
 * @author RollW
 */
interface ReviewTaskDetails : TimeAttributed {
    /**
     * Unique identifier for this review task.
     */
    val taskId: String

    /**
     * ID of the parent review job.
     */
    val reviewJobId: String

    /**
     * Current status of the review task.
     */
    val status: ReviewStatus

    /**
     * ID of the assigned reviewer, null if unassigned.
     */
    val reviewerId: Long?

    /**
     * Structured feedback from the reviewer.
     */
    val feedback: ReviewFeedback?
}