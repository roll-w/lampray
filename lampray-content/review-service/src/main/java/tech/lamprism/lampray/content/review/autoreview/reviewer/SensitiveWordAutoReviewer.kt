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
package tech.lamprism.lampray.content.review.autoreview.reviewer

import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.review.ReviewJobSummary
import tech.lamprism.lampray.content.review.autoreview.AutoReviewContext
import tech.lamprism.lampray.content.review.feedback.ReviewCategory
import tech.lamprism.lampray.content.review.feedback.ReviewFeedbackEntry
import tech.lamprism.lampray.content.review.feedback.ReviewSeverity
import tech.lamprism.lampray.content.structuraltext.StructuralText

/**
 * Enhanced auto reviewer that detects sensitive words by traversing structural text nodes.
 * Supports cross-node sensitive word detection (e.g., "sen" in node1 + "sitive" in node2 = "sensitive").
 *
 * @author RollW
 */
@Component
class SensitiveWordAutoReviewer : AutoReviewer {

    companion object {
        // Common sensitive word patterns - should be configurable in production
        private val SENSITIVE_PATTERNS = listOf(
            // Political sensitive words
            "test", "test2"
        )
        // TODO: load from configuration

        // Maximum window size for cross-node detection
        private const val MAX_CROSS_NODE_WINDOW = 50
    }

    override val reviewerInfo: AutoReviewer.Info = AutoReviewer.Info(
        name = "Sensitive Word Detector",
        description = "Detects sensitive words and inappropriate content with cross-node detection",
        className = SensitiveWordAutoReviewer::class.java.name
    )

    override fun review(reviewJob: ReviewJobSummary, autoReviewContext: AutoReviewContext) {
        val content = autoReviewContext.contentDetails
        val title = content.title ?: ""

        // Check title - simplified for now
        val foundInTitle = detectSensitiveWords(title)
        if (foundInTitle.isNotEmpty()) {
            // Just add to context for now - TODO: enhance with location tracking
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry(
                    category = ReviewCategory.SENSITIVE_CONTENT,
                    severity = ReviewSeverity.CRITICAL,
                    message = "Sensitive words detected in title: ${foundInTitle.joinToString(", ")}",
                    suggestion = "Please remove or replace sensitive words"
                )
            )
        }

        // Check content
        val structuralText = content.content
        if (structuralText != null) {
            val foundInContent = detectSensitiveWordsInStructure(structuralText)
            if (foundInContent.isNotEmpty()) {
                autoReviewContext.addFeedbackEntry(
                    ReviewFeedbackEntry(
                        category = ReviewCategory.SENSITIVE_CONTENT,
                        severity = ReviewSeverity.CRITICAL,
                        message = "Sensitive words detected in content: ${foundInContent.joinToString(", ")}",
                        suggestion = "Please remove or replace sensitive words"
                    )
                )
            }
        }

        // Mark as completed
        autoReviewContext.markReviewerCompleted(this)
    }

    private fun detectSensitiveWordsInStructure(node: StructuralText): List<String> {
        // Collect all text segments in order
        val textSegments = mutableListOf<String>()
        collectTextSegments(node, textSegments)

        // Build a sliding window buffer for cross-node detection
        val found = mutableSetOf<String>()
        val buffer = StringBuilder()

        for (segment in textSegments) {
            buffer.append(segment)

            // Keep buffer size limited
            if (buffer.length > MAX_CROSS_NODE_WINDOW * 2) {
                buffer.delete(0, buffer.length - MAX_CROSS_NODE_WINDOW)
            }

            // Check for sensitive words in current buffer
            found.addAll(detectSensitiveWords(buffer.toString()))
        }

        return found.toList()
    }

    private fun collectTextSegments(node: StructuralText, segments: MutableList<String>) {
        if (node.content.isNotEmpty()) {
            segments.add(node.content)
        }

        for (child in node.children) {
            collectTextSegments(child, segments)
        }
    }

    private fun detectSensitiveWords(text: String): List<String> {
        val lowerText = text.lowercase()
        return SENSITIVE_PATTERNS.filter { pattern ->
            lowerText.contains(pattern.lowercase())
        }
    }
}

