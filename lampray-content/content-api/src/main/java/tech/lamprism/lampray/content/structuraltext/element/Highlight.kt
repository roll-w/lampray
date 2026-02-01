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

import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextType
import tech.lamprism.lampray.content.structuraltext.StructuralTextVisitor
import tech.lamprism.lampray.content.structuraltext.validation.StructuralTextValidationException

/**
 * @author RollW
 */
data class Highlight @JvmOverloads constructor(
    override val content: String = "",
    override val children: List<StructuralText> = emptyList(),
    val color: AttributeColor? = null
) : StructuralText {
    init {
        if (color != null) {
            require(color in HIGHLIGHTS) { "Invalid highlight color: $color" }
        }

        val disallowed = setOf(
            StructuralTextType.TABLE,
            StructuralTextType.TABLE_CELL,
            StructuralTextType.TABLE_ROW,
            StructuralTextType.DOCUMENT,
            StructuralTextType.PARAGRAPH,
            StructuralTextType.LIST,
            StructuralTextType.CODE_BLOCK,
            StructuralTextType.BLOCKQUOTE
        )
        children.forEachIndexed { index, child ->
            if (child.type in disallowed) {
                throw StructuralTextValidationException(
                    "Disallowed child type for parent=HIGHLIGHT: index=$index, child=${child.type}"
                )
            }
        }
    }

    override val type: StructuralTextType
        get() = StructuralTextType.HIGHLIGHT

    override fun accept(visitor: StructuralTextVisitor) {
        visitor.visit(this)
    }

    companion object {
        @JvmField
        val HIGHLIGHTS: Set<AttributeColor> = setOf(
            AttributeColor.YELLOW,
            AttributeColor.GREEN,
            AttributeColor.BLUE,
            AttributeColor.PINK,
            AttributeColor.ORANGE,
            AttributeColor.PURPLE,
            AttributeColor.RED,
            AttributeColor.LIME,
            AttributeColor.TEAL,
            AttributeColor.CYAN,
        )
    }
}