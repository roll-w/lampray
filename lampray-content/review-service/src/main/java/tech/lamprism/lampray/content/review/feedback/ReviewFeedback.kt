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
package tech.lamprism.lampray.content.review.feedback

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Structured review feedback that provides detailed review opinions and results.
 *
 * @author RollW
 */
data class ReviewFeedback(
    val verdict: ReviewVerdict,
    val entries: List<ReviewFeedbackEntry> = emptyList(),
    val summary: String? = null,
) {
    /**
     * Checks if the feedback indicates approval.
     */
    @JsonIgnore
    fun isApproved(): Boolean = verdict == ReviewVerdict.APPROVED

    /**
     * Checks if the feedback has any critical issues.
     */
    @JsonIgnore
    fun hasCriticalIssues(): Boolean = entries.any { it.severity == ReviewSeverity.CRITICAL }

    companion object {
        /**
         * Creates an approved feedback with optional summary.
         */
        @JvmStatic
        fun approved(summary: String? = null): ReviewFeedback =
            ReviewFeedback(
                verdict = ReviewVerdict.APPROVED,
                summary = summary
            )

        /**
         * Creates a rejected feedback with issues.
         */
        @JvmStatic
        fun rejected(
            entries: List<ReviewFeedbackEntry>,
            summary: String? = null
        ): ReviewFeedback =
            ReviewFeedback(
                verdict = ReviewVerdict.REJECTED,
                entries = entries,
                summary = summary
            )

        /**
         * Creates feedback that requires revision.
         */
        @JvmStatic
        fun needsRevision(
            entries: List<ReviewFeedbackEntry>,
            summary: String? = null
        ): ReviewFeedback =
            ReviewFeedback(
                verdict = ReviewVerdict.NEEDS_REVISION,
                entries = entries,
                summary = summary
            )
    }
}

