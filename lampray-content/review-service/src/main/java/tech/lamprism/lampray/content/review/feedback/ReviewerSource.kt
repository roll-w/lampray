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

/**
 * Identifies the source of a review feedback.
 *
 * @property isAutomatic whether this feedback is from an automated reviewer
 * @property reviewerName name/identifier of the reviewer
 */
data class ReviewerSource(
    val isAutomatic: Boolean,
    val reviewerName: String
) {
    companion object {
        /**
         * Creates a source for automated reviewer.
         */
        @JvmStatic
        fun automatic(reviewerName: String): ReviewerSource =
            ReviewerSource(true, reviewerName)

        /**
         * Creates a source for manual human reviewer.
         */
        @JvmStatic
        fun manual(reviewerName: String = "manual"): ReviewerSource =
            ReviewerSource(false, reviewerName)
    }
}