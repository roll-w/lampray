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

package tech.lamprism.lampray.content.review.util

import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.element.Blockquote
import tech.lamprism.lampray.content.structuraltext.element.Bold
import tech.lamprism.lampray.content.structuraltext.element.Document
import tech.lamprism.lampray.content.structuraltext.element.Heading
import tech.lamprism.lampray.content.structuraltext.element.Highlight
import tech.lamprism.lampray.content.structuraltext.element.Image
import tech.lamprism.lampray.content.structuraltext.element.InlineCode
import tech.lamprism.lampray.content.structuraltext.element.Italic
import tech.lamprism.lampray.content.structuraltext.element.Link
import tech.lamprism.lampray.content.structuraltext.element.ListBlock
import tech.lamprism.lampray.content.structuraltext.element.ListItem
import tech.lamprism.lampray.content.structuraltext.element.Mention
import tech.lamprism.lampray.content.structuraltext.element.Paragraph
import tech.lamprism.lampray.content.structuraltext.element.StrikeThrough
import tech.lamprism.lampray.content.structuraltext.element.Table
import tech.lamprism.lampray.content.structuraltext.element.TableCell
import tech.lamprism.lampray.content.structuraltext.element.TableRow
import tech.lamprism.lampray.content.structuraltext.element.Text
import tech.lamprism.lampray.content.structuraltext.element.Underline

/**
 * Result of flattening structural text: contiguous text segments (block-level separated)
 * and collected link hrefs.
 */
data class FlattenResult(
    val texts: List<String>,
    val links: List<String>
)

/**
 * Flatten structural text into contiguous inline text sequences separated by block elements.
 * Inline nodes are concatenated so that words split across formatting nodes (bold/italic)
 * remain detectable.
 *
 * @author RollW
 */
object StructuralTextUtils {
    fun flatten(root: StructuralText): FlattenResult {
        val texts = mutableListOf<String>()
        val links = mutableListOf<String>()

        fun collectInline(node: StructuralText, sb: StringBuilder) {
            when (node) {
                is Text -> sb.append(node.content)
                is InlineCode -> sb.append(node.content)
                is Mention -> {
                    if (node.content.isNotEmpty()) sb.append(node.content)
                }
                is Bold, is Italic, is Underline, is StrikeThrough, is Highlight -> {
                    // these are container inline nodes; recurse children
                    node.children.forEach { collectInline(it, sb) }
                }
                is Link -> {
                    // link: collect visible children text and record href
                    links.add(node.href)
                    if (node.children.isEmpty()) {
                        // sometimes link might have empty children but title
                        node.title?.let { sb.append(it) }
                    } else {
                        node.children.forEach { collectInline(it, sb) }
                    }
                }
                is Image -> {
                    // ignore image content in text
                }
                else -> {
                    // fallback: recurse children
                    node.children.forEach { collectInline(it, sb) }
                }
            }
        }

        fun visit(node: StructuralText) {
            when (node) {
                is Document -> node.children.forEach { visit(it) }
                is Paragraph, is Heading, is ListItem, is TableCell -> {
                    val sb = StringBuilder()
                    if (node is Paragraph && node.children.isEmpty()) {
                        sb.append(node.content)
                    } else if (node.children.isEmpty()) {
                        // heading or other block with content
                        sb.append(node.content)
                    } else {
                        node.children.forEach { collectInline(it, sb) }
                    }
                    texts.add(sb.toString())
                }
                is ListBlock -> node.children.forEach { visit(it) }
                is Blockquote -> {
                    val sb = StringBuilder()
                    if (node.children.isEmpty()) sb.append(node.content) else node.children.forEach { collectInline(it, sb) }
                    texts.add(sb.toString())
                }
                is Table -> {
                    node.children.filterIsInstance<TableRow>().forEach { row ->
                        row.children.filterIsInstance<TableCell>().forEach { cell ->
                            val sb = StringBuilder()
                            if (cell.children.isEmpty()) sb.append(cell.content) else cell.children.forEach { collectInline(it, sb) }
                            texts.add(sb.toString())
                        }
                    }
                }
                else -> {
                    // fallback: process inline by collecting and add as single text
                    val sb = StringBuilder()
                    collectInline(node, sb)
                    if (sb.isNotEmpty()) texts.add(sb.toString())
                }
            }
        }

        visit(root)
        return FlattenResult(texts.filter { it.isNotEmpty() }, links)
    }
}
