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
import tech.lamprism.lampray.content.review.feedback.ContentLocationRange
import tech.lamprism.lampray.content.review.feedback.ReviewCategory
import tech.lamprism.lampray.content.review.feedback.ReviewFeedbackEntry
import tech.lamprism.lampray.content.review.ReviewJobSummary
import tech.lamprism.lampray.content.review.feedback.ReviewSeverity
import tech.lamprism.lampray.content.review.autoreview.AutoReviewContext
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextType

/**
 * Auto reviewer that checks content formatting and structure quality by traversing nodes.
 *
 * @author RollW
 */
@Component
class ContentQualityAutoReviewer : AutoReviewer {

    companion object {
        private const val MIN_PARAGRAPH_LENGTH = 20
        private const val MAX_LINE_LENGTH = 1000
        private const val MIN_TITLE_LENGTH = 3
    }

    override val reviewerInfo: AutoReviewer.Info = AutoReviewer.Info(
        name = "Content Quality Checker",
        description = "Checks content formatting and structural quality",
        className = ContentQualityAutoReviewer::class.java.name
    )

    override fun review(reviewJob: ReviewJobSummary, autoReviewContext: AutoReviewContext) {
        val content = autoReviewContext.contentDetails
        val title = content.title ?: ""
        val structuralText = content.content

        // Check title
        if (title.isBlank()) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.CONTENT_QUALITY,
                    severity = ReviewSeverity.CRITICAL,
                    message = "Title is empty",
                    locationRange = ContentLocationRange.at(0, 0, "title"),
                    suggestion = "A title is required for content"
                )
            )
        } else if (title.length < MIN_TITLE_LENGTH) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.CONTENT_QUALITY,
                    severity = ReviewSeverity.MAJOR,
                    message = "Title is too short (${title.length} characters)",
                    locationRange = ContentLocationRange.at(0, title.length, "title"),
                    suggestion = "Minimum $MIN_TITLE_LENGTH characters required"
                )
            )
        }

        if (structuralText != null) {
            val qualityAnalysis = analyzeQuality(structuralText)

            // Check for excessively long lines
            if (qualityAnalysis.longLinesCount > 0) {
                autoReviewContext.addFeedbackEntry(
                    ReviewFeedbackEntry.fromAutoReviewer(
                        reviewerName = reviewerInfo.name,
                        category = ReviewCategory.FORMAT,
                        severity = ReviewSeverity.MINOR,
                        message = "${qualityAnalysis.longLinesCount} line(s) exceed maximum length",
                        locationRange = ContentLocationRange.wholeContent(),
                        suggestion = "Lines should not exceed $MAX_LINE_LENGTH characters"
                    )
                )
            }

            // Check for empty content
            if (qualityAnalysis.isEmpty) {
                autoReviewContext.addFeedbackEntry(
                    ReviewFeedbackEntry.fromAutoReviewer(
                        reviewerName = reviewerInfo.name,
                        category = ReviewCategory.CONTENT_QUALITY,
                        severity = ReviewSeverity.CRITICAL,
                        message = "Content is empty",
                        locationRange = ContentLocationRange.at(0, 0, "content"),
                        suggestion = "Content body is required"
                    )
                )
            }

            // Check for very short paragraphs (potential low quality)
            val shortParagraphRatio = if (qualityAnalysis.totalParagraphs > 0) {
                qualityAnalysis.shortParagraphs.toDouble() / qualityAnalysis.totalParagraphs
            } else {
                0.0
            }

            if (qualityAnalysis.totalParagraphs > 2 && shortParagraphRatio > 0.5) {
                autoReviewContext.addFeedbackEntry(
                    ReviewFeedbackEntry.fromAutoReviewer(
                        reviewerName = reviewerInfo.name,
                        category = ReviewCategory.CONTENT_QUALITY,
                        severity = ReviewSeverity.MINOR,
                        message = "Too many very short paragraphs detected (${qualityAnalysis.shortParagraphs}/${qualityAnalysis.totalParagraphs})",
                        locationRange = ContentLocationRange.wholeContent(),
                        suggestion = "Consider expanding paragraphs to at least $MIN_PARAGRAPH_LENGTH characters for better quality"
                    )
                )
            }
        } else {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.CONTENT_QUALITY,
                    severity = ReviewSeverity.CRITICAL,
                    message = "Content is empty",
                    locationRange = ContentLocationRange.at(0, 0, "content"),
                    suggestion = "Content body is required"
                )
            )
        }

        autoReviewContext.markReviewerCompleted(this)
    }

    private fun analyzeQuality(node: StructuralText): QualityAnalysis {
        val analysis = QualityAnalysis()
        analyzeNode(node, analysis)
        return analysis
    }

    private fun analyzeNode(node: StructuralText, analysis: QualityAnalysis) {
        // Check if this is a paragraph-level node
        if (isParagraphNode(node)) {
            val paragraphLength = calculateNodeTextLength(node)
            analysis.totalParagraphs++

            if (paragraphLength in 1 until MIN_PARAGRAPH_LENGTH) {
                analysis.shortParagraphs++
            }
        }

        // Check line length in text content
        if (node.content.isNotEmpty()) {
            analysis.hasContent = true
            val lines = node.content.lines()
            analysis.longLinesCount += lines.count { it.length > MAX_LINE_LENGTH }
        }

        // Recursively analyze children
        for (child in node.children) {
            analyzeNode(child, analysis)
        }
    }

    private fun isParagraphNode(node: StructuralText): Boolean {
        return node.type == StructuralTextType.PARAGRAPH ||
               node.type == StructuralTextType.HEADING
    }

    private fun calculateNodeTextLength(node: StructuralText): Int {
        var length = node.content.length
        for (child in node.children) {
            length += calculateNodeTextLength(child)
        }
        return length
    }

    private class QualityAnalysis {
        var longLinesCount = 0
        var hasContent = false
        var totalParagraphs = 0
        var shortParagraphs = 0

        val isEmpty: Boolean
            get() = !hasContent
    }
}

