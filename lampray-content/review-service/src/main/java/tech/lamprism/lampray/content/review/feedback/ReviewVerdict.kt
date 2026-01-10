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



package tech.lamprism.lampray.content.review.feedback

import tech.lamprism.lampray.content.review.ReviewTaskStatus

/**
 * Review verdict indicating the overall result.
 *
 * @author RollW
 */
enum class ReviewVerdict {
    /**
     * Review is pending or in progress.
     */
    PENDING,

    /**
     * Content needs revision before it can be approved.
     */
    NEEDS_REVISION,

    /**
     * Content is rejected and cannot be published.
     */
    REJECTED,

    /**
     * Content is approved and can be published.
     */
    APPROVED;

    fun toReviewTaskStatus() = when (this) {
        PENDING -> ReviewTaskStatus.PENDING
        NEEDS_REVISION -> ReviewTaskStatus.REJECTED
        REJECTED -> ReviewTaskStatus.REJECTED
        APPROVED -> ReviewTaskStatus.APPROVED
    }
}