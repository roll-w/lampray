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
import tech.lamprism.lampray.content.review.feedback.ReviewFeedback
import tech.lamprism.lampray.content.review.feedback.ReviewFeedbackEntry
import tech.lamprism.lampray.content.review.ReviewJobDetails
import tech.lamprism.lampray.content.review.feedback.ReviewSeverity
import tech.lamprism.lampray.content.review.ReviewTaskDetails
import tech.lamprism.lampray.content.review.feedback.ReviewVerdict
import tech.lamprism.lampray.content.review.autoreview.reviewer.AutoReviewer
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Context for auto review process that collects structured feedback from multiple reviewers.
 * This class is thread-safe and can be used by multiple auto-reviewers concurrently.
 *
 * @author RollW
 */
class AutoReviewContext(
    val reviewJob: ReviewJobDetails,
    val reviewTask: ReviewTaskDetails,
    val contentDetails: ContentDetails
) {
    private val feedbackEntries: MutableList<ReviewFeedbackEntry> = CopyOnWriteArrayList()
    private val reviewerCompletionStatus: MutableMap<String, Boolean> = ConcurrentHashMap()

    /**
     * Add a structured feedback item. Thread-safe.
     * The feedback entry should include the reviewer source information.
     */
    fun addFeedbackEntry(item: ReviewFeedbackEntry) {
        feedbackEntries.add(item)
    }

    /**
     * Add multiple feedback items at once.
     */
    fun addFeedbackEntries(items: List<ReviewFeedbackEntry>) {
        feedbackEntries.addAll(items)
    }

    /**
     * Mark a reviewer as completed.
     */
    fun markReviewerCompleted(reviewer: AutoReviewer) {
        reviewerCompletionStatus[reviewer.reviewerInfo.name] = true
    }

    /**
     * Get all collected feedback entries (immutable view).
     */
    fun getFeedbackEntries(): List<ReviewFeedbackEntry> {
        return Collections.unmodifiableList(feedbackEntries)
    }

    /**
     * Whether any feedback has been recorded by the specified reviewer.
     */
    fun hasFeedbackFrom(reviewer: AutoReviewer): Boolean {
        val reviewerName = reviewer.reviewerInfo.name
        return feedbackEntries.any {
            it.reviewerSource.isAutomatic &&
            it.reviewerSource.reviewerName == reviewerName
        }
    }

    /**
     * Whether the review has completed and all checks passed.
     */
    fun isApproved(): Boolean {
        return feedbackEntries.none {
            it.severity == ReviewSeverity.CRITICAL ||
            it.severity == ReviewSeverity.MAJOR
        }
    }

    /**
     * Get feedback entries from a specific reviewer.
     */
    fun getFeedbackFrom(reviewer: AutoReviewer): List<ReviewFeedbackEntry> {
        val reviewerName = reviewer.reviewerInfo.name
        return feedbackEntries.filter {
            it.reviewerSource.isAutomatic &&
            it.reviewerSource.reviewerName == reviewerName
        }
    }

    /**
     * Build final ReviewFeedback based on all collected entries.
     * Returns null if no issues were found (implying approval).
     */
    fun buildFeedback(): ReviewFeedback? {
        if (feedbackEntries.isEmpty()) {
            return ReviewFeedback.approved("Auto-review completed with no issues detected")
        }

        val verdict = determineVerdict()
        val summary = buildSummary()

        return ReviewFeedback(
            verdict = verdict,
            entries = feedbackEntries.toList(),
            summary = summary
        )
    }

    private fun determineVerdict(): ReviewVerdict {
        if (isApproved()) {
            return ReviewVerdict.APPROVED
        }

        val hasCritical = feedbackEntries.any { it.severity == ReviewSeverity.CRITICAL }
        return if (hasCritical) {
            ReviewVerdict.REJECTED
        } else {
            ReviewVerdict.NEEDS_REVISION
        }
    }

    private fun buildSummary(): String {
        val issuesBySeverity = feedbackEntries.groupBy { it.severity }
        val counts = ReviewSeverity.entries.associateWith { severity ->
            issuesBySeverity[severity]?.size ?: 0
        }.filter { it.value > 0 }

        return buildString {
            append("Auto-review found ")
            append(feedbackEntries.size)
            append(" issue(s): ")
            append(counts.entries.joinToString(", ") { (severity, count) ->
                "$count ${severity.name.lowercase()}"
            })
        }
    }
}
