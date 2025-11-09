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

/**
 * Types of structural text nodes.
 *
 * @author RollW
 */
enum class StructuralTextType {
    DOCUMENT,
    PARAGRAPH,
    HEADING,
    LIST,
    LIST_ITEM,
    BLOCKQUOTE,
    CODE_BLOCK,
    INLINE_CODE,
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    UNDERLINE,
    HIGHLIGHT,
    TEXT,
    LINK,
    IMAGE,
    TABLE,
    TABLE_ROW,
    TABLE_CELL,
    HORIZONTAL_DIVIDER,
    MATH,
    MENTION;

    companion object {
        fun fromType(type: String): StructuralTextType? {
            return entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
        }
    }
}