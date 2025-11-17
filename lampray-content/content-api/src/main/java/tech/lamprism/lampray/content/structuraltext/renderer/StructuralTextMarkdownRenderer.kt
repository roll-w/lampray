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
 * @author RollW
 */
class StructuralTextMarkdownRenderer : StructuralTextRenderer {
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
                    builder.append("#".repeat(node.level)).append(" ")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("\n\n")
                }

                is ListBlock -> {
                    node.children.filterIsInstance<ListItem>().forEachIndexed { index, item ->
                        if (node.ordered) {
                            builder.append("${index + 1}. ")
                        } else {
                            builder.append("- ")
                        }
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
                    builder.append("> ")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("\n\n")
                }

                is CodeBlock -> {
                    builder.append("```")
                    node.language?.let { builder.append(it) }
                    builder.append("\n")
                    builder.append(node.content).append("\n")
                    builder.append("```\n\n")
                }

                is InlineCode -> {
                    builder.append("`").append(node.content).append("`")
                }

                is Bold -> {
                    builder.append("**")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("**")
                }

                is Italic -> {
                    builder.append("*")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("*")
                }

                is StrikeThrough -> {
                    builder.append("~~")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("~~")
                }

                is Underline -> {
                    builder.append("<u>")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("</u>")
                }

                is Highlight -> {
                    builder.append("==")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("==")
                }

                is Link -> {
                    builder.append("[")
                    if (node.children.isEmpty()) {
                        builder.append(node.content)
                    } else {
                        node.children.forEach { it.accept(this) }
                    }
                    builder.append("](${node.href}")
                    node.title?.let { builder.append(" \"$it\"") }
                    builder.append(")")
                }

                is Image -> {
                    builder.append("![${node.alt ?: ""}](${node.src}")
                    node.title?.let { builder.append(" \"$it\"") }
                    builder.append(")")
                }

                is Table -> {
                    val rows = node.children.filterIsInstance<TableRow>()
                    if (rows.isNotEmpty()) {
                        val headerCells = rows.first().children.filterIsInstance<TableCell>()
                        if (headerCells.isNotEmpty()) {
                            builder.append("|")
                            headerCells.forEach { cell ->
                                builder.append(" ").append(cell.content).append(" |")
                            }
                            builder.append("\n|")
                            headerCells.forEach { _ ->
                                builder.append(" --- |")
                            }
                            builder.append("\n")
                        }
                        rows.drop(1).forEach { row ->
                            val cells = row.children.filterIsInstance<TableCell>()
                            if (cells.isNotEmpty()) {
                                builder.append("|")
                                cells.forEach { cell ->
                                    builder.append(" ").append(cell.content).append(" |")
                                }
                                builder.append("\n")
                            }
                        }
                        builder.append("\n")
                    }
                }

                is HorizontalDivider -> {
                    builder.append("---\n\n")
                }

                is Text -> {
                    builder.append(node.content)
                }

                is Math -> {
                    if (node.display) {
                        builder.append("$$\n").append(node.content).append("\n$$")
                    } else {
                        builder.append("$").append(node.content).append("$")
                    }
                }

                is Mention -> {
                    builder.append(" @").append(node.content).append(" ")
                }

                else -> {
                    builder.append(node.content)
                    node.children.forEach { it.accept(this) }
                }
            }
        }
    }
}