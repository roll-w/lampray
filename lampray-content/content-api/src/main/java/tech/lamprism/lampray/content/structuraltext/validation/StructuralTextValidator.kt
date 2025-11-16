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

package tech.lamprism.lampray.content.structuraltext.validation

import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextType

/**
 * @author RollW
 */
class StructuralTextValidator(
    private val minTotalTextLength: Long = 0,
    private val maxTotalTextLength: Long = 100_000,
    private val maxDepth: Int = 40,
    private val maxTextNodeLength: Long = Long.MAX_VALUE,
    private val requireDocumentRoot: Boolean = true
) {

    @Throws(
        StructuralTextEmptyException::class,
        StructuralTextRootNotDocumentException::class,
        StructuralTextTooDeepException::class,
        StructuralTextNodeTooLongException::class,
        StructuralTextTooShortException::class,
        StructuralTextTooLongException::class
    )
    fun validate(root: StructuralText?) {
        if (root == null) {
            throw StructuralTextEmptyException("Structural text is null")
        }

        if (root.isEmpty()) {
            throw StructuralTextEmptyException("Structural text is empty")
        }

        if (requireDocumentRoot && root.type != StructuralTextType.DOCUMENT) {
            throw StructuralTextRootNotDocumentException("Root must be DOCUMENT")
        }

        var totalTextLength = 0L
        val stack = ArrayDeque<Pair<StructuralText, Int>>()
        stack.add(root to 0)

        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()

            if (depth > maxDepth) {
                throw StructuralTextTooDeepException("Structural text is too deep: depth=$depth, max=$maxDepth")
            }

            val content = node.content
            if (content.isNotEmpty()) {
                totalTextLength += content.length
                if (content.length > maxTextNodeLength) {
                    throw StructuralTextNodeTooLongException("A single text node is too long: ${content.length} chars")
                }
            }

            node.children.forEach { child ->
                stack.add(child to depth + 1)
            }
        }

        if (totalTextLength < minTotalTextLength) {
            throw StructuralTextTooShortException("Content is too short: $totalTextLength chars, minimum is $minTotalTextLength")
        }

        if (totalTextLength > maxTotalTextLength) {
            throw StructuralTextTooLongException("Content is too long: $totalTextLength chars")
        }
    }
}
