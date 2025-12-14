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

package tech.lamprism.lampray.content.structuraltext.element

import com.fasterxml.jackson.annotation.JsonInclude
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextType
import tech.lamprism.lampray.content.structuraltext.StructuralTextVisitor
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextValidationException

/**
 * Heading element representing a heading with a specified level.
 *
 * @author RollW
 */
data class Heading @JvmOverloads constructor(
    val level: Int,
    override val content: String = "",
    override val children: List<StructuralText> = emptyList(),
    @field:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    val alignment: TextAlignment = TextAlignment.LEFT,
) : StructuralText {
    init {
        // Heading should only contain inline children
        val disallowed = setOf(
            StructuralTextType.DOCUMENT,
            StructuralTextType.PARAGRAPH,
            StructuralTextType.LIST,
            StructuralTextType.TABLE,
            StructuralTextType.TABLE_ROW,
            StructuralTextType.TABLE_CELL,
            StructuralTextType.BLOCKQUOTE,
            StructuralTextType.CODE_BLOCK
        )
        children.forEachIndexed { index, child ->
            if (child.type in disallowed) {
                throw StructuralTextValidationException(
                    "Disallowed child type for parent=${StructuralTextType.HEADING}: index=$index, child=${child.type}"
                )
            }
        }
    }

    override val type: StructuralTextType
        get() = StructuralTextType.HEADING

    override fun accept(visitor: StructuralTextVisitor) {
        visitor.visit(this)
    }
}