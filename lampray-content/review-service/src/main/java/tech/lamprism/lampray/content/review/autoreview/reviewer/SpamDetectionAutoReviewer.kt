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
 * Auto reviewer that detects spam patterns by traversing structural text.
 *
 * @author RollW
 */
@Component
class SpamDetectionAutoReviewer : AutoReviewer {

    companion object {
        private const val MAX_URL_COUNT = 5
        private const val MAX_REPEATED_CHAR_SEQUENCE = 10
        private const val MIN_UNIQUE_CHAR_RATIO = 0.3
        private val URL_REGEX = """https?://\S+""".toRegex()
    }

    override val reviewerInfo: AutoReviewer.Info = AutoReviewer.Info(
        name = "Spam Detector",
        description = "Detects spam patterns and low-quality content",
        className = SpamDetectionAutoReviewer::class.java.name
    )

    override fun review(reviewJob: ReviewJobSummary, autoReviewContext: AutoReviewContext) {
        val content = autoReviewContext.contentDetails
        val structuralText = content.content ?: run {
            autoReviewContext.markReviewerCompleted(this)
            return
        }

        val analysisResult = analyzeStructure(structuralText)

        // Check excessive URLs
        if (analysisResult.urlCount > MAX_URL_COUNT) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.POLICY_VIOLATION,
                    severity = ReviewSeverity.MAJOR,
                    message = "Excessive URLs detected: found ${analysisResult.urlCount} URLs, maximum $MAX_URL_COUNT allowed",
                    locationRange = ContentLocationRange.wholeContent(),
                    suggestion = "Remove excessive URLs to comply with spam policy"
                )
            )
        }

        // Check for repeated character sequences
        if (analysisResult.hasExcessiveRepeatedChars) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.CONTENT_QUALITY,
                    severity = ReviewSeverity.MINOR,
                    message = "Excessive repeated character sequences detected",
                    locationRange = ContentLocationRange.wholeContent(),
                    suggestion = "Avoid using more than $MAX_REPEATED_CHAR_SEQUENCE consecutive identical characters"
                )
            )
        }

        // Check for low diversity (copy-paste spam)
        if (analysisResult.totalChars > 100 && analysisResult.uniqueCharRatio < MIN_UNIQUE_CHAR_RATIO) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.CONTENT_QUALITY,
                    severity = ReviewSeverity.MAJOR,
                    message = "Content has low character diversity (${String.format("%.1f%%", analysisResult.uniqueCharRatio * 100)})",
                    locationRange = ContentLocationRange.wholeContent(),
                    suggestion = "This may indicate spam or low-quality content. Add more varied content."
                )
            )
        }

        // Check all caps
        if (analysisResult.isAllCaps && analysisResult.totalChars > 20) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.FORMAT,
                    severity = ReviewSeverity.MINOR,
                    message = "Content is excessively in all caps",
                    locationRange = ContentLocationRange.wholeContent(),
                    suggestion = "Use normal capitalization for better readability"
                )
            )
        }

        autoReviewContext.markReviewerCompleted(this)
    }

    private fun analyzeStructure(node: StructuralText): AnalysisResult {
        val result = AnalysisResult()
        analyzeNode(node, result)
        return result
    }

    private fun analyzeNode(node: StructuralText, result: AnalysisResult) {
        // Analyze content if present
        if (node.content.isNotEmpty()) {
            result.totalChars += node.content.length
            result.allChars.append(node.content)

            // Count URLs if this is a link node or contains URLs in text
            if (node.type == StructuralTextType.LINK) {
                result.urlCount++
            } else {
                result.urlCount += URL_REGEX.findAll(node.content).count()
            }

            // Check for repeated characters
            if (!result.hasExcessiveRepeatedChars && hasExcessiveRepeatedChars(node.content)) {
                result.hasExcessiveRepeatedChars = true
            }
        }

        // Recursively analyze children
        for (child in node.children) {
            analyzeNode(child, result)
        }
    }

    private fun hasExcessiveRepeatedChars(text: String): Boolean {
        var maxSequence = 1
        var currentSequence = 1
        var lastChar = ' '

        for (char in text) {
            if (char == lastChar && !char.isWhitespace()) {
                currentSequence++
                if (currentSequence > maxSequence) {
                    maxSequence = currentSequence
                }
            } else {
                currentSequence = 1
            }
            lastChar = char
        }

        return maxSequence > MAX_REPEATED_CHAR_SEQUENCE
    }

    private class AnalysisResult {
        var urlCount = 0
        var totalChars = 0
        val allChars = StringBuilder()
        var hasExcessiveRepeatedChars = false

        val uniqueCharRatio: Double
            get() {
                if (totalChars == 0) return 1.0
                val nonWhitespace = allChars.filterNot { it.isWhitespace() }
                if (nonWhitespace.isEmpty()) return 1.0
                return nonWhitespace.toSet().size.toDouble() / nonWhitespace.length
            }

        val isAllCaps: Boolean
            get() {
                val letters = allChars.filter { it.isLetter() }
                if (letters.isEmpty()) return false
                return letters.all { it.isUpperCase() }
            }
    }
}

