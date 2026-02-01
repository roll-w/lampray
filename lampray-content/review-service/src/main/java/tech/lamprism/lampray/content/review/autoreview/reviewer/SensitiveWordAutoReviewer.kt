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
            "Loaded ${sensitivePatterns.size} sensitive word patterns, max cross-node window: $maxCrossNodeWindow"
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
        val details = autoReviewContext.contentDetails
        val title = details.title ?: ""
        val bodyNode: StructuralText = details.content ?: StructuralText.EMPTY

        val detections = mutableListOf<SensitiveWordDetection>()

        // detect in title
        if (title.isNotBlank()) {
            detections += detectSensitiveWordsWithLocation(title, "$.title")
        }

        // detect in structural content
        detections += detectSensitiveWordsInStructure(bodyNode)

        if (detections.isNotEmpty()) {
            val entries = detections.map { d ->
                ReviewFeedbackEntry.fromAutoReviewer(
                    reviewerName = reviewerInfo.name,
                    category = ReviewCategory.SENSITIVE_CONTENT,
                    severity = ReviewSeverity.CRITICAL,
                    // TODO: i18n message
                    message = d.maskedWord,
                    locationRange = d.location,
                )
            }
            autoReviewContext.addFeedbackEntries(entries)
        }

        // Mark as completed
        autoReviewContext.markReviewerCompleted(this)
    }

    private fun detectSensitiveWordsInStructure(node: StructuralText): List<SensitiveWordDetection> {
        val segments = mutableListOf<String>()
        val jsonPaths = mutableListOf<String>()
        collectTextSegmentsWithPath(node, "$", segments, jsonPaths)

        if (segments.isEmpty() || sensitivePatterns.isEmpty()) return emptyList()

        // build concatenated text and mapping
        val sb = StringBuilder()
        val charToNode = mutableListOf<Pair<Int, Int>>() // pair(nodeIndex, posInNode)

        segments.forEachIndexed { idx, text ->
            for (i in text.indices) {
                sb.append(text[i])
                charToNode.add(Pair(idx, i))
            }
        }

        val combined = sb.toString()
        if (combined.isEmpty()) {
            return emptyList()
        }

        val lowerCombined = combined.lowercase()
        val results = mutableListOf<SensitiveWordDetection>()

        sensitivePatterns.forEach { patternRaw ->
            val pattern = patternRaw.trim()
            if (pattern.isEmpty()) return@forEach
            val lowerPattern = pattern.lowercase()

            var idx = lowerCombined.indexOf(lowerPattern)
            while (idx >= 0) {
                val start = idx
                val end = idx + lowerPattern.length // exclusive

                val startMap = charToNode[start]
                val endMap = charToNode[end - 1]

                val path = jsonPaths[startMap.first]
                val endPath = jsonPaths[endMap.first]
                val startInNode = startMap.second
                val endInNode = endMap.second + 1

                val location = ContentLocationRange(
                    startInNode,
                    endInNode,
                    path,
                    endPath
                )

                val masked = maskWord(combined.substring(start, end))
                val context = extractContext(combined, start, end, masked, 20)

                results.add(SensitiveWordDetection(location, masked, context))

                idx = lowerCombined.indexOf(lowerPattern, idx + 1)
            }
        }

        return results
    }

    private fun collectTextSegmentsWithPath(
        node: StructuralText,
        currentJsonPath: String,
        segments: MutableList<String>,
        jsonPaths: MutableList<String>
    ) {
        // If node has content and it's non-empty, record it
        if (node.content.isNotBlank()) {
            segments.add(node.content)
            jsonPaths.add("$currentJsonPath.content")
        }
        if (node.hasChildren()) {
            node.children.forEachIndexed { idx, child ->
                val childJsonPath = "$currentJsonPath.children[$idx]"
                collectTextSegmentsWithPath(child, childJsonPath, segments, jsonPaths)
            }
        }
    }

    private fun detectSensitiveWordsWithLocation(text: String, jsonPath: String? = null): List<SensitiveWordDetection> {
        if (text.isBlank() || sensitivePatterns.isEmpty()) {
            return emptyList()
        }
        val lowerText = text.lowercase()
        val results = mutableListOf<SensitiveWordDetection>()

        sensitivePatterns.forEach { patternRaw ->
            val pattern = patternRaw.trim()
            if (pattern.isEmpty()) return@forEach
            val lowerPattern = pattern.lowercase()

            var idx = lowerText.indexOf(lowerPattern)
            while (idx >= 0) {
                val start = idx
                val end = idx + lowerPattern.length
                val location = ContentLocationRange(
                    start,
                    end,
                    jsonPath,
                    jsonPath
                )
                val masked = maskWord(text.substring(start, end))
                val context = extractContext(text, start, end, masked, 20)
                results.add(SensitiveWordDetection(location, masked, context))
                idx = lowerText.indexOf(lowerPattern, idx + 1)
            }
        }

        return results
    }

    /**
     * Mask a sensitive word for display purposes.
     * Shows first character, masks middle characters, shows last character if word is long enough.
     */
    private fun maskWord(word: String): String {
        if (word.length <= 1) return "*"
        if (word.length == 2) return "${word[0]}*"
        val middle = "*".repeat(word.length - 2)
        return "${word.first()}$middle${word.last()}"
    }

    /**
     * Extract context around a match position with the sensitive word masked.
     */
    private fun extractContext(text: String, startPos: Int, endPos: Int, maskedWord: String, contextLength: Int): String {
        val from = maxOf(0, startPos - contextLength)
        val to = minOf(text.length, endPos + contextLength)
        val before = text.substring(from, startPos)
        val after = text.substring(endPos, to)
        return before + maskedWord + after
    }

    data class SensitiveWordDetection(
        val location: ContentLocationRange,
        val maskedWord: String,
        val context: String
    )
}
