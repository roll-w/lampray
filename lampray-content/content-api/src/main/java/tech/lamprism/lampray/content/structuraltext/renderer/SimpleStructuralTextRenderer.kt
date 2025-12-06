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

package tech.lamprism.lampray.content.structuraltext.renderer

import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextRenderer
import tech.lamprism.lampray.content.structuraltext.StructuralTextVisitor
import tech.lamprism.lampray.content.structuraltext.element.Blockquote
import tech.lamprism.lampray.content.structuraltext.element.Bold
import tech.lamprism.lampray.content.structuraltext.element.CodeBlock
import tech.lamprism.lampray.content.structuraltext.element.Document
import tech.lamprism.lampray.content.structuraltext.element.Heading
import tech.lamprism.lampray.content.structuraltext.element.Highlight
import tech.lamprism.lampray.content.structuraltext.element.HorizontalDivider
import tech.lamprism.lampray.content.structuraltext.element.Image
import tech.lamprism.lampray.content.structuraltext.element.InlineCode
import tech.lamprism.lampray.content.structuraltext.element.Italic
import tech.lamprism.lampray.content.structuraltext.element.Link
import tech.lamprism.lampray.content.structuraltext.element.ListBlock
import tech.lamprism.lampray.content.structuraltext.element.ListItem
import tech.lamprism.lampray.content.structuraltext.element.Math
import tech.lamprism.lampray.content.structuraltext.element.Mention
import tech.lamprism.lampray.content.structuraltext.element.Paragraph
import tech.lamprism.lampray.content.structuraltext.element.StrikeThrough
import tech.lamprism.lampray.content.structuraltext.element.Table
import tech.lamprism.lampray.content.structuraltext.element.TableCell
import tech.lamprism.lampray.content.structuraltext.element.TableRow
import tech.lamprism.lampray.content.structuraltext.element.Text
import tech.lamprism.lampray.content.structuraltext.element.Underline

/**
 * Simply renders structural text to plain text.
 *
 * @author RollW
 */
class SimpleStructuralTextRenderer: StructuralTextRenderer {
    override fun render(text: StructuralText): String {
        val visitor = Visitor()
        text.accept(visitor)
        return visitor.result
    }

    private class Visitor : StructuralTextVisitor {
        private val builder = StringBuilder()

        val result: String
            get() = builder.toString()

        override fun visit(node: StructuralText) {
            when (node) {
                is Document -> {
                    node.children.forEach { it.accept(this) }
                }

                is Paragraph -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("\n\n")
                }

                is Heading -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("\n\n")
                }

                is ListBlock -> {
                    node.children.filterIsInstance<ListItem>().forEachIndexed { index, item ->
                        if (item.children.isEmpty()) {
                            builder.append(item.content)
                        } else {
                            item.children.forEach { it.accept(this) }
                        }
                        builder.append("\n")
                    }
                    builder.append("\n")
                }

                is Blockquote -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("\n")
                }

                is CodeBlock -> {
                    // include raw code content
                    builder.append(node.content).append("\n")
                }

                is InlineCode -> {
                    builder.append(node.content)
                }

                is Bold -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                }

                is Italic -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                }

                is StrikeThrough -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                }

                is Underline -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                }

                is Highlight -> {
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                }

                is Link -> {
                    if (node.children.isEmpty()) {
                        if (node.content.isNotEmpty()) builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                }

                is Image -> {
                    // skip image in plain text
                }

                is Table -> {
                    val rows = node.children.filterIsInstance<TableRow>()
                    rows.forEachIndexed { rowIdx, row ->
                        val cells = row.children.filterIsInstance<TableCell>()
                        if (cells.isNotEmpty()) {
                            cells.forEachIndexed { idx, cell ->
                                if (idx > 0) builder.append(' ')

                                // build prefix annotation
                                val prefixes = mutableListOf<String>()
                                if (cell.isHeader || (node.hasHeaderRow && rowIdx == 0) || (node.hasHeaderColumn && idx == 0)) {
                                    prefixes.add("H")
                                }
                                if (cell.colspan > 1 || cell.rowspan > 1) {
                                    prefixes.add("span=${cell.colspan}x${cell.rowspan}")
                                }
                                if (cell.backgroundColor != null) {
                                    prefixes.add("bg=${cell.backgroundColor.toJson()}")
                                }
                                if (cell.width != null || cell.height != null) {
                                    val w = cell.width?.let { "${it}px" } ?: "auto"
                                    val h = cell.height?.let { "${it}px" } ?: "auto"
                                    prefixes.add("size=${w}x${h}")
                                }
                                if (prefixes.isNotEmpty()) {
                                    builder.append('[').append(prefixes.joinToString(",")).append("] ")
                                }

                                // content or children
                                if (cell.children.isEmpty()) {
                                    builder.append(cell.content)
                                } else {
                                    cell.children.forEach { it.accept(this) }
                                }
                            }
                            builder.append("\n")
                        }
                    }
                    builder.append("\n")
                }

                is HorizontalDivider -> {
                    // skip horizontal divider in plain text
                }

                is Text -> {
                    builder.append(node.content)
                }

                is Math -> {
                    builder.append(node.content)
                }

                is Mention -> {
                    if (node.content.isNotEmpty()) builder.append("@").append(node.content)
                }

                else -> {
                    if (node.content.isNotEmpty()) builder.append(node.content)
                    node.children.forEach { it.accept(this) }
                }
            }
        }
    }
}