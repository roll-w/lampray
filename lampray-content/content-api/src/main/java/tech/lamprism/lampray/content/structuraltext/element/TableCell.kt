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
 * Cell inside a table row.
 *
 * @author RollW
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class TableCell @JvmOverloads constructor(
    override val content: String = "",
    override val children: List<StructuralText> = emptyList(),
    /**
     * Whether this cell should be treated as a header cell (render as <th>).
     */
    val isHeader: Boolean = false,
    /**
     * Background color chosen from the fixed palette
     */
    val backgroundColor: AttributeColor? = null,
    /**
     * Number of columns this cell spans.
     */
    val colspan: Int = 1,
    /**
     * Number of rows this cell spans.
     */
    val rowspan: Int = 1
) : StructuralText {
    init {
        // Allow Paragraph, List, Image inside a cell; disallow Table, TableRow, Document directly
        val disallowed = setOf(
            StructuralTextType.TABLE,
            StructuralTextType.TABLE_ROW,
            StructuralTextType.DOCUMENT
        )
        children.forEachIndexed { index, child ->
            if (child.type in disallowed) {
                throw StructuralTextValidationException(
                    "Disallowed child type for parent=TABLE_CELL: index=$index, child=${child.type}"
                )
            }
        }
    }

    override val type: StructuralTextType
        get() = StructuralTextType.TABLE_CELL

    override fun accept(visitor: StructuralTextVisitor) {
        visitor.visit(this)
    }
}