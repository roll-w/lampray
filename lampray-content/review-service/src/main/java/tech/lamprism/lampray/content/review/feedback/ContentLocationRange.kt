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
 * Represents a range in content where an issue was detected.
 *
 * @property startOffset start position in content (character offset)
 * @property endOffset end position in content (character offset)
 * @property context contextual information about the location (e.g., "paragraph 3", "title")
 */
data class ContentLocationRange(
    val startOffset: Int,
    val endOffset: Int,
    val context: String? = null
) {
    init {
        require(startOffset >= 0) { "Start offset must be non-negative" }
        require(endOffset >= startOffset) { "End offset must be >= start offset" }
    }

    fun length(): Int = endOffset - startOffset

    companion object {
        /**
         * Creates a location range for the entire content.
         */
        @JvmStatic
        fun wholeContent(): ContentLocationRange = ContentLocationRange(0, Int.MAX_VALUE, "entire content")

        /**
         * Creates a location range with context description.
         */
        @JvmStatic
        fun at(startOffset: Int, endOffset: Int, context: String): ContentLocationRange =
            ContentLocationRange(startOffset, endOffset, context)
    }
}