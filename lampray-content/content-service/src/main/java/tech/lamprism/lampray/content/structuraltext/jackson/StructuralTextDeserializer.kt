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

package tech.lamprism.lampray.content.structuraltext.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component
import tech.lamprism.lampray.content.structuraltext.StructuralText
import tech.lamprism.lampray.content.structuraltext.StructuralTextType
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
@Component
class StructuralTextDeserializer : JsonDeserializer<StructuralText>() {
    private val typeToClass: Map<StructuralTextType, Class<out StructuralText>> = mapOf(
        StructuralTextType.DOCUMENT to Document::class.java,
        StructuralTextType.PARAGRAPH to Paragraph::class.java,
        StructuralTextType.HEADING to Heading::class.java,
        StructuralTextType.TEXT to Text::class.java,
        StructuralTextType.BOLD to Bold::class.java,
        StructuralTextType.ITALIC to Italic::class.java,
        StructuralTextType.UNDERLINE to Underline::class.java,
        StructuralTextType.STRIKETHROUGH to StrikeThrough::class.java,
        StructuralTextType.INLINE_CODE to InlineCode::class.java,
        StructuralTextType.CODE_BLOCK to CodeBlock::class.java,
        StructuralTextType.MATH to Math::class.java,
        StructuralTextType.LINK to Link::class.java,
        StructuralTextType.IMAGE to Image::class.java,
        StructuralTextType.MENTION to Mention::class.java,
        StructuralTextType.LIST to ListBlock::class.java,
        StructuralTextType.LIST_ITEM to ListItem::class.java,
        StructuralTextType.BLOCKQUOTE to Blockquote::class.java,
        StructuralTextType.HORIZONTAL_DIVIDER to HorizontalDivider::class.java,
        StructuralTextType.TABLE to Table::class.java,
        StructuralTextType.TABLE_ROW to TableRow::class.java,
        StructuralTextType.TABLE_CELL to TableCell::class.java,
        StructuralTextType.HIGHLIGHT to Highlight::class.java
    )

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): StructuralText {
        val mapper = p.codec as ObjectMapper
        val node = mapper.readTree<JsonNode>(p) as ObjectNode

        val typeNode = node["type"]
        if (typeNode == null || !typeNode.isTextual) {
            throw JsonMappingException.from(p, "Missing or invalid 'type' field")
        }

        val typeName = typeNode.asText()
        val structuralTextType = StructuralTextType.fromType(typeName)
            ?: throw JsonMappingException.from(p, "Unknown StructuralText type: $typeName")
        val targetClass = typeToClass[structuralTextType]
            ?: throw JsonMappingException.from(p, "Unknown StructuralText type: $typeName")
        return mapper.treeToValue(node, targetClass)
    }
}