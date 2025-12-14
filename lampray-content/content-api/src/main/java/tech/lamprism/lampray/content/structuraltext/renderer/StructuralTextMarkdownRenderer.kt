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
import tech.lamprism.lampray.content.structuraltext.element.ListType
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

        private fun renderNodeContent(node: StructuralText): String {
            val sub = Visitor()
            node.accept(sub)
            return sub.result
        }

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
                        when (node.listType) {
                            ListType.ORDERED -> builder.append("${index + 1}. ")
                            ListType.TASK -> {
                                val checkbox = if (item.checked == true) "[x]" else "[ ]"
                                builder.append("- $checkbox ")
                            }

                            ListType.UNORDERED -> builder.append("- ")
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
                    if (rows.isEmpty()) return

                    // detect advanced attributes that markdown cannot represent
                    var advanced = false
                    if (node.widths.isNotEmpty()) advanced = true
                    for (r in rows) {
                        if (r.height != null) advanced = true
                        if (r.widths != null) advanced = true
                        for (c in r.children.filterIsInstance<TableCell>()) {
                            if (c.backgroundColor != null) advanced = true
                            if (c.colspan > 1 || c.rowspan > 1) advanced = true
                            if (c.isHeader) advanced = true
                        }
                    }

                    if (!advanced) {
                        // simple markdown table
                        val first = rows.first()
                        val headerCells = first.children.filterIsInstance<TableCell>()
                        if (node.hasHeaderRow && headerCells.isNotEmpty()) {
                            builder.append("|")
                            headerCells.forEach { cell ->
                                builder.append(" ").append(cell.content).append(" |")
                            }
                            builder.append("\n|")
                            // alignment row
                            headerCells.forEach { cell ->
                                builder.append(" --- |")
                            }
                            builder.append("\n")
                            // remaining rows
                            rows.drop(1).forEach { row ->
                                val cells = row.children.filterIsInstance<TableCell>()
                                if (cells.isNotEmpty()) {
                                    builder.append("|")
                                    cells.forEach { cell -> builder.append(" ").append(cell.content).append(" |") }
                                    builder.append("\n")
                                }
                            }
                            builder.append("\n")
                        } else {
                            // no header row, render every row as normal rows
                            rows.forEach { row ->
                                val cells = row.children.filterIsInstance<TableCell>()
                                if (cells.isNotEmpty()) {
                                    builder.append("|")
                                    cells.forEach { cell -> builder.append(" ").append(cell.content).append(" |") }
                                    builder.append("\n")
                                }
                            }
                            builder.append("\n")
                        }
                    } else {
                        // render as HTML table to preserve attributes
                        builder.append("<table>")
                        // colgroup for column widths
                        if (node.widths.isNotEmpty()) {
                            builder.append("<colgroup>")
                            for (w in node.widths) {
                                if (w == null) builder.append("<col />")
                                else builder.append("<col style=\"width:${w}px\" />")
                            }
                            builder.append("</colgroup>")
                        }
                        // rows
                        for ((rowIdx, row) in rows.withIndex()) {
                            if (row.height != null) builder.append("<tr style=\"height:${row.height}px\">")
                            else builder.append("<tr>")
                            val cells = row.children.filterIsInstance<TableCell>()
                            for ((colIdx, cell) in cells.withIndex()) {
                                val tag =
                                    if (cell.isHeader || (node.hasHeaderRow && rowIdx == 0) || (node.hasHeaderColumn && colIdx == 0)) "th" else "td"
                                val attrs = mutableListOf<String>()
                                if (cell.colspan > 1) {
                                    attrs.add("colspan=\"${cell.colspan}\"")
                                }
                                if (cell.rowspan > 1) {
                                    attrs.add("rowspan=\"${cell.rowspan}\"")
                                }
                                val styles = mutableListOf<String>()
                                if (cell.backgroundColor != null) {
                                    styles.add("background:${cell.backgroundColor.toJson()}")
                                }
                                // Cell width from row's widths
                                if (row.widths != null && colIdx < row.widths.size) {
                                    val cellWidth = row.widths[colIdx]
                                    if (cellWidth != null) {
                                        styles.add("width:${cellWidth}px")
                                    }
                                }
                                if (styles.isNotEmpty()) {
                                    attrs.add("style=\"${styles.joinToString(";")}\"")
                                }
                                builder.append("<").append(tag)
                                if (attrs.isNotEmpty()) {
                                    builder.append(" ").append(attrs.joinToString(" "))
                                }
                                builder.append(">")
                                // cell content (render children or content)
                                if (cell.children.isEmpty()) {
                                    builder.append(escapeHtml(cell.content))
                                } else {
                                    builder.append(
                                        escapeHtml(renderNodeContent(TableCell(cell.content, cell.children)))
                                    )
                                }
                                builder.append("</").append(tag).append(">")
                            }
                            builder.append("</tr>")
                        }
                        builder.append("</table>\n\n")
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

        private fun escapeHtml(s: String): String {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        }
    }
}