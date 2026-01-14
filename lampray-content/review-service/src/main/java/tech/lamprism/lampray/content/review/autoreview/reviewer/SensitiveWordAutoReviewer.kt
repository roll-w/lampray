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

import jakarta.annotation.PostConstruct
import org.slf4j.info
import org.slf4j.logger
import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.review.ReviewJobSummary
import tech.lamprism.lampray.content.review.autoreview.AutoReviewContext
import tech.lamprism.lampray.content.review.autoreview.config.SensitiveWordConfigKeys
import tech.lamprism.lampray.content.review.feedback.ContentLocationRange
import tech.lamprism.lampray.content.review.feedback.ReviewCategory
import tech.lamprism.lampray.content.review.feedback.ReviewFeedbackEntry
import tech.lamprism.lampray.content.review.feedback.ReviewSeverity
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.setting.ConfigReader
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author RollW
 */
@Component
class SensitiveWordAutoReviewer(
    private val configReader: ConfigReader
) : AutoReviewer {

    companion object {
        private val logger = logger<SensitiveWordAutoReviewer>()
    }

    private var sensitivePatterns: List<String> = emptyList()
    private var maxCrossNodeWindow: Int = 50

    @PostConstruct
    fun init() {
        sensitivePatterns = loadSensitiveWords()
        maxCrossNodeWindow = configReader[SensitiveWordConfigKeys.MAX_WINDOW_SIZE, 50]
        logger.info {
            "Loaded ${sensitivePatterns.size} sensitive word patterns, max cross-node window: ${maxCrossNodeWindow}"
        }
    }

    private fun loadSensitiveWords(): List<String> {
        val filePath = configReader[SensitiveWordConfigKeys.SENSITIVE_WORD_FILE_PATH]
        val loadedWords = if (!filePath.isNullOrBlank()) {
            loadFromFile(filePath)
        } else {
            configReader[SensitiveWordConfigKeys.SENSITIVE_WORD_LIST] ?: emptySet()
        }
        return loadedWords
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun loadFromFile(path: String): Set<String> {
        return try {
            val resolvedPath = Paths.get(path)
            if (!Files.exists(resolvedPath)) {
                logger.warn("Sensitive words file not found: {}", path)
                emptySet()
            } else {
                Files.readAllLines(resolvedPath)
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                    .filter { it.startsWith("#").not() }
                    .toSet()
            }
        } catch (e: Exception) {
            logger.error("Failed to load sensitive words from file: {}", path, e)
            emptySet()
        }
    }

    override val reviewerInfo: AutoReviewer.Info = AutoReviewer.Info(
        name = "Sensitive Word Detector",
        description = "Detects sensitive words and inappropriate content with cross-node detection",
        className = SensitiveWordAutoReviewer::class.java.name
    )

    override fun review(reviewJob: ReviewJobSummary, autoReviewContext: AutoReviewContext) {
        val content = autoReviewContext.contentDetails
        val title = content.title ?: ""

        // Check title
        val foundInTitle = detectSensitiveWordsWithLocation(title, "title")
        for ((location, maskedWord, context) in foundInTitle) {
            autoReviewContext.addFeedbackEntry(
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.SENSITIVE_CONTENT,
                    severity = ReviewSeverity.CRITICAL,
                    message = "Sensitive word detected: $maskedWord",
                    locationRange = location,
                    suggestion = "Please remove or replace the sensitive content. Context: $context"
                )
            )
        }

        // Check content
        val structuralText = content.content
        if (structuralText != null) {
            val foundInContent = detectSensitiveWordsInStructure(structuralText)
            for ((location, maskedWord, context) in foundInContent) {
                autoReviewContext.addFeedbackEntry(
                    ReviewFeedbackEntry.fromAutoReviewer(
                        reviewerName = reviewerInfo.name,
                        category = ReviewCategory.SENSITIVE_CONTENT,
                        severity = ReviewSeverity.CRITICAL,
                        message = "Sensitive word detected: $maskedWord",
                        locationRange = location,
                        suggestion = "Please remove or replace the sensitive content. Context: $context"
                    )
                )
            }
        }

        // Mark as completed
        autoReviewContext.markReviewerCompleted(this)
    }

    private data class SensitiveWordDetection(
        val location: ContentLocationRange,
        val maskedWord: String,
        val context: String
    )

    private fun detectSensitiveWordsInStructure(node: StructuralText): List<SensitiveWordDetection> {
        val detections = mutableListOf<SensitiveWordDetection>()

        // Collect all text segments with their node paths
        val textSegments = mutableListOf<Pair<String, String>>() // (nodePath, text)
        collectTextSegmentsWithPath(node, "", textSegments)

        // Build a sliding window buffer for cross-node detection
        val buffer = StringBuilder()
        val segmentOffsets = mutableListOf<Int>() // Track where each segment starts in buffer

        for ((nodePath, segment) in textSegments) {
            val startOffset = buffer.length
            segmentOffsets.add(startOffset)
            buffer.append(segment)

            // Keep buffer size limited
            if (buffer.length > maxCrossNodeWindow * 2) {
                val removeLength = buffer.length - maxCrossNodeWindow
                buffer.delete(0, removeLength)
                // Adjust offsets
                segmentOffsets.replaceAll { it - removeLength }
                segmentOffsets.removeIf { it < 0 }
            }

            // Check for sensitive words in current buffer
            val bufferDetections = detectSensitiveWordsWithLocation(buffer.toString(), nodePath)
            detections.addAll(bufferDetections)
        }

        // Deduplicate detections based on position and content
        return detections.distinctBy { "${it.location.context}-${it.location.startOffset}-${it.location.endOffset}" }
    }

    private fun collectTextSegmentsWithPath(
        node: StructuralText,
        currentPath: String,
        segments: MutableList<Pair<String, String>>
    ) {
        val nodePath = if (currentPath.isEmpty()) {
            node.type.name
        } else {
            "$currentPath.${node.type}"
        }

        if (node.content.isNotEmpty()) {
            segments.add(nodePath to node.content)
        }

        node.children.forEachIndexed { index, child ->
            collectTextSegmentsWithPath(child, "$nodePath[$index]", segments)
        }
    }

    private fun detectSensitiveWordsWithLocation(
        text: String,
        nodePath: String
    ): List<SensitiveWordDetection> {
        if (sensitivePatterns.isEmpty()) {
            return emptyList()
        }

        val lowerText = text.lowercase()
        val detections = mutableListOf<SensitiveWordDetection>()

        for (pattern in sensitivePatterns) {
            val lowerPattern = pattern.lowercase()
            var startIndex = 0

            while (true) {
                val index = lowerText.indexOf(lowerPattern, startIndex)
                if (index == -1) break

                val endIndex = index + pattern.length
                val maskedWord = maskWord(pattern)
                val context = extractContext(text, index, endIndex, maskedWord)

                val location = ContentLocationRange.at(
                    startOffset = index,
                    endOffset = endIndex,
                    context = nodePath
                )

                detections.add(SensitiveWordDetection(location, maskedWord, context))

                startIndex = index + 1 // Continue searching for overlapping matches
            }
        }

        return detections
    }

    /**
     * Mask a sensitive word for display purposes.
     * Shows first character, masks middle characters, shows last character if word is long enough.
     */
    private fun maskWord(word: String): String {
        return when {
            word.length <= 1 -> "*"
            word.length == 2 -> "${word[0]}*"
            else -> "${word[0]}${"*".repeat(word.length - 2)}${word.last()}"
        }
    }

    /**
     * Extract context around a match position with the sensitive word masked.
     */
    private fun extractContext(
        text: String,
        startPos: Int,
        endPos: Int,
        maskedWord: String,
        contextLength: Int = 20
    ): String {
        val beforeStart = maxOf(0, startPos - contextLength)
        val afterEnd = minOf(text.length, endPos + contextLength)

        val before = text.substring(beforeStart, startPos)
        val after = text.substring(endPos, afterEnd)

        val prefix = if (beforeStart > 0) "..." else ""
        val suffix = if (afterEnd < text.length) "..." else ""

        return "$prefix$before$maskedWord$after$suffix"
    }
}

