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
 * Row in a table containing cells.
 *
 * @author RollW
 */
data class TableRow @JvmOverloads constructor(
    override val children: List<StructuralText> = emptyList(),
    /** Row height in pixels (px) */
    val rowHeight: Double? = null,
) : StructuralText {
    init {
        children.forEachIndexed { index, child ->
            if (child.type != StructuralTextType.TABLE_CELL) {
                throw StructuralTextValidationException(
                    "Disallowed child type for parent=TABLE_ROW: index=$index, child=${child.type}"
                )
            }
        }
    }

    override val type: StructuralTextType
        get() = StructuralTextType.TABLE_ROW

    /**
     * Table rows do not have direct text content.
     */
    override val content: String
        get() = ""

    override fun accept(visitor: StructuralTextVisitor) {
        visitor.visit(this)
    }
}