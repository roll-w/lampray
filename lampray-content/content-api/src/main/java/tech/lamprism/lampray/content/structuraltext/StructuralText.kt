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

package tech.lamprism.lampray.content.structuraltext

import tech.lamprism.lampray.content.structuraltext.element.Document

/**
 * Represents a node in a structured text document, which can be either a container node
 * (like a paragraph or heading) or a leaf node (like plain text or an inline element).
 * Each node has a type, optional text content, and may have child nodes.
 *
 * @author RollW
 */
interface StructuralText {
    val type: StructuralTextType

    /**
     * The text content of the node (meaningful only for leaf nodes or certain inline nodes).
     *
     * Semantic notes:
     * - For `TEXT`, `INLINE_CODE`, `CODE_BLOCK`, `MATH` etc.: represents the raw text
     * - For container nodes like `DOCUMENT`, `PARAGRAPH` etc.: typically empty or null
     * - For `LINK`, `IMAGE` etc.: not used to store primary data (primary data is in attributes),
     *   but may contain alt text or title if applicable
     * - For `BOLD`, `ITALIC`, `UNDERLINE`, `STRIKETHROUGH`: usually empty, as formatting is implied by type
     * - For `MENTION`: may contain the display name or username
     */
    val content: String

    val children: List<StructuralText>

    fun isLeaf(): Boolean = children.isEmpty()

    fun isEmpty(): Boolean = children.isEmpty() && content.isEmpty()

    fun hasChildren(): Boolean = children.isNotEmpty()

    fun accept(visitor: StructuralTextVisitor)

    companion object {
        @JvmStatic
        val EMPTY: StructuralText = Document()
    }
}