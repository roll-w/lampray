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

enum class StructuralTextType(val type: String) {
    DOCUMENT("document"),
    PARAGRAPH("paragraph"),
    HEADING("heading"),
    LIST("list"),
    LIST_ITEM("list_item"),
    BLOCKQUOTE("blockquote"),
    CODE_BLOCK("code_block"),
    INLINE_CODE("inline_code"),
    BOLD("bold"),
    ITALIC("italic"),
    STRIKETHROUGH("strikethrough"),
    UNDERLINE("underline"),
    HIGHLIGHT("highlight"),
    TEXT("text"),
    LINK("link"),
    IMAGE("image"),
    TABLE("table"),
    TABLE_ROW("table_row"),
    TABLE_CELL("table_cell"),
    HORIZONTAL_DIVIDER("horizontal_rule"),
    MATH("math"),
    MENTION("mention");

    companion object {
        fun fromType(type: String): StructuralTextType? {
            return entries.firstOrNull { it.type == type }
        }
    }
}