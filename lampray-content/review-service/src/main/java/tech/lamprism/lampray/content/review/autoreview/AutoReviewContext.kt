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
package tech.lamprism.lampray.content.review.autoreview

import tech.lamprism.lampray.content.ContentDetails
import tech.lamprism.lampray.content.review.ReviewFeedback
import tech.lamprism.lampray.content.review.ReviewFeedbackEntry
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.ReviewSeverity
import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.ReviewVerdict
import tech.lamprism.lampray.content.review.autoreview.reviewer.AutoReviewer
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Context for auto review process that collects structured feedback from multiple reviewers.
 *
 * @author RollW
 */
class AutoReviewContext(
    val reviewJob: ReviewJobDetails,
    val reviewTask: ReviewTaskDetails,
    val contentDetails: ContentDetails
) {
    private val feedbackEntries: MutableList<ReviewFeedbackEntry> = CopyOnWriteArrayList()

    /**
     * Add a structured feedback item. Thread-safe.
     */
    fun addFeedbackEntry(item: ReviewFeedbackEntry) {
        feedbackEntries.add(item)
    }

    fun getFeedbackEntries(): List<ReviewFeedbackEntry> {
        return Collections.unmodifiableList(feedbackEntries)
    }

    /**
     * Whether any outcome has been recorded by the specified reviewer.
     */
    fun hasFeedbackFrom(reviewer: AutoReviewer): Boolean {
        return false
    }

    fun isApproved(): Boolean {
        return false
    }

    /**
     * Build final ReviewFeedback based on all collected entries.
     */
    fun buildFeedback(): ReviewFeedback? {
        if (feedbackEntries.isEmpty()) {
            return null // No issues found
        }

        val verdict = if (isApproved()) {
            ReviewVerdict.APPROVED
        } else {
            val hasCritical = feedbackEntries.any { it.severity == ReviewSeverity.CRITICAL }
            if (hasCritical) ReviewVerdict.REJECTED else ReviewVerdict.NEEDS_REVISION
        }

        return ReviewFeedback(
            verdict = verdict,
            entries = feedbackEntries.toList(),
        )
    }
}
