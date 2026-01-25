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
 * @author RollW
 */
data class ContentLocationRange(
    val startInNode: Int,
    val endInNode: Int,
    /**
     * Could be a JSONPath-like expression to locate the node in the content structure.
     * Example: "$.children[0].children[2].content". These fields are optional and may be null when not available.
     */
    val startPath: String? = null,
    val endPath: String? = null
) {
    init {
        require(startInNode >= 0) { "startInNode must be non-negative" }
        require(endInNode >= 0) { "endInNode must be non-negative" }
        require(startPath?.isNotBlank() ?: true) { "path if provided must be non-blank" }
        require(endPath?.isNotBlank() ?: true) { "endPath if provided must be non-blank" }
    }

}
