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

import type {Editor} from "@tiptap/core";
import type {
    CodeBlockElement,
    HeadingElement,
    ImageElement,
    LinkElement,
    ListElement,
    ListItemElement,
    MentionElement,
    StructuralText,
    TextElement
} from "./types.ts";
import {ListType, StructuralTextType} from "./types.ts";

/**
 * TipTap node types that can be converted to marks (inline formatting).
 */
const MARK_TYPES = new Set([
    StructuralTextType.BOLD,
    StructuralTextType.ITALIC,
    StructuralTextType.UNDERLINE,
    StructuralTextType.STRIKETHROUGH,
    StructuralTextType.HIGHLIGHT,
    StructuralTextType.INLINE_CODE,
    StructuralTextType.LINK
]);

/**
 * Converts TipTap editor content to StructuralText format.
 */
export function convertToStructuralText(editor: Editor): StructuralText {
    const json = editor.getJSON()
    return convertNodeToStructuralText(json)
}

/**
 * Converts a TipTap JSON node to StructuralText format.
 */
function convertNodeToStructuralText(node: any): StructuralText {
    const type = mapTipTapTypeToStructuralType(node.type)

    // Handle text nodes with marks
    if (node.type === 'text') {
        const textNode: TextElement = {
            type: StructuralTextType.TEXT,
            content: node.text || '',
            children: []
        }

        if (node.marks && Array.isArray(node.marks) && node.marks.length > 0) {
            return wrapWithMarks(textNode, node.marks)
        }

        return textNode
    }

    // Build the base node
    const result: StructuralText = {
        type: type,
        content: '',
        children: []
    }

    if (node.attrs) {
        handleNodeAttributes(result, node)
    }

    // Handle list types
    if (node.type === 'bulletList') {
        (result as ListElement).listType = ListType.UNORDERED
    } else if (node.type === 'orderedList') {
        (result as ListElement).listType = ListType.ORDERED
    } else if (node.type === 'taskList') {
        (result as ListElement).listType = ListType.TASK
    }

    // Handle task item checked state
    if (node.type === 'taskItem' && node.attrs) {
        (result as ListItemElement).checked = node.attrs.checked || false
    }

    if (node.content && Array.isArray(node.content)) {
        result.children = node.content
            .map((child: any) => convertNodeToStructuralText(child))
    }

    optimizeNodeStructure(result)
    return result
}

/**
 * Optimizes the node structure by:
 * 1. Removing unnecessary paragraph wrappers in container nodes (only when there's a single paragraph)
 * 2. Simplifying pure text nodes to use content instead of children
 *
 * Note: Multiple paragraphs are preserved as they represent line breaks.
 *
 * @author RollW
 */
function optimizeNodeStructure(node: StructuralText): void {
    if (hasSpecialContent(node.type) || node.type === StructuralTextType.DOCUMENT) {
        return
    }

    if (shouldFlattenParagraph(node.type) && node.children.length === 1) {
        flattenSingleParagraph(node)
    }

    if (canSimplifyToContent(node)) {
        simplifyToContent(node)
    }
}

function shouldFlattenParagraph(type: StructuralTextType): boolean {
    return type === StructuralTextType.HEADING ||
        type === StructuralTextType.LIST_ITEM ||
        type === StructuralTextType.BLOCKQUOTE ||
        type === StructuralTextType.TABLE_CELL
}

function flattenSingleParagraph(node: StructuralText): void {
    if (node.children.length !== 1) {
        return
    }

    const firstChild = node.children[0]
    if (!firstChild || firstChild.type !== StructuralTextType.PARAGRAPH) {
        return
    }

    const paragraph = firstChild
    node.children = paragraph.children

    if (paragraph.children.length === 0 && paragraph.content) {
        node.content = paragraph.content
    }
}

function canSimplifyToContent(node: StructuralText): boolean {
    if (node.children.length !== 1) {
        return false
    }

    const child = node.children[0]
    if (!child) {
        return false
    }

    // Only simplify if child is a plain TEXT node
    return child.type === StructuralTextType.TEXT &&
           child.children.length === 0 &&
           child.content.length > 0
}

function simplifyToContent(node: StructuralText): void {
    if (node.children.length !== 1) {
        return
    }

    const firstChild = node.children[0]
    if (!firstChild || firstChild.type !== StructuralTextType.TEXT) {
        return
    }

    node.content = firstChild.content
    node.children = []
}

function hasSpecialContent(type: StructuralTextType): boolean {
    return type === StructuralTextType.IMAGE ||
        type === StructuralTextType.LINK ||
        type === StructuralTextType.HORIZONTAL_DIVIDER ||
        type === StructuralTextType.DOCUMENT
}

function isEmptyNode(node: StructuralText): boolean {
    return node.children.length === 0 && !node.content.trim()
}

function handleNodeAttributes(result: StructuralText, node: any): void {
    const attrs = node.attrs

    if (attrs.level !== undefined) {
        (result as HeadingElement).level = attrs.level
    }

    if (attrs.language !== undefined) {
        (result as CodeBlockElement).language = attrs.language
    }

    if (attrs.href !== undefined) {
        (result as LinkElement).href = attrs.href
        if (attrs.title !== undefined) {
            (result as LinkElement).title = attrs.title
        }
    }

    if (attrs.src !== undefined) {
        (result as ImageElement).src = attrs.src
        if (attrs.alt !== undefined) {
            (result as ImageElement).alt = attrs.alt
        }
        if (attrs.title !== undefined && !attrs.href) {
            (result as ImageElement).title = attrs.title
        }
    }

    if (attrs.id !== undefined || attrs.userId !== undefined) {
        (result as MentionElement).userId = attrs.id || attrs.userId
    }
}

/**
 * Wraps a StructuralText node with mark elements (inline formatting).
 * This ensures marks are converted to proper StructuralText nodes.
 *
 * @author RollW
 */
function wrapWithMarks(node: StructuralText, marks: any[]): StructuralText {
    let result = node

    for (const mark of marks) {
        const markType = mapTipTapTypeToStructuralType(mark.type)
        result = createMarkWrapper(markType, result, mark.attrs)
    }

    return result
}

/**
 * Creates a mark wrapper node based on the mark type.
 */
function createMarkWrapper(
    markType: StructuralTextType,
    child: StructuralText,
    attrs?: any
): StructuralText {
    const baseWrapper: StructuralText = {
        type: markType,
        content: '',
        children: [child]
    }

    // Handle link marks
    if (markType === StructuralTextType.LINK && attrs) {
        const linkWrapper = baseWrapper as LinkElement
        linkWrapper.href = attrs.href || ''
        if (attrs.title) {
            linkWrapper.title = attrs.title
        }
        return linkWrapper
    }

    return baseWrapper
}

function mapTipTapTypeToStructuralType(type: string): StructuralTextType {
    const mapping: Record<string, StructuralTextType> = {
        doc: StructuralTextType.DOCUMENT,
        paragraph: StructuralTextType.PARAGRAPH,
        heading: StructuralTextType.HEADING,
        bulletList: StructuralTextType.LIST_BLOCK,
        orderedList: StructuralTextType.LIST_BLOCK,
        listItem: StructuralTextType.LIST_ITEM,
        taskList: StructuralTextType.LIST_BLOCK,
        taskItem: StructuralTextType.LIST_ITEM,
        blockquote: StructuralTextType.BLOCKQUOTE,
        codeBlock: StructuralTextType.CODE_BLOCK,
        code: StructuralTextType.INLINE_CODE,
        bold: StructuralTextType.BOLD,
        italic: StructuralTextType.ITALIC,
        strike: StructuralTextType.STRIKETHROUGH,
        underline: StructuralTextType.UNDERLINE,
        highlight: StructuralTextType.HIGHLIGHT,
        text: StructuralTextType.TEXT,
        link: StructuralTextType.LINK,
        image: StructuralTextType.IMAGE,
        table: StructuralTextType.TABLE,
        tableRow: StructuralTextType.TABLE_ROW,
        tableCell: StructuralTextType.TABLE_CELL,
        tableHeader: StructuralTextType.TABLE_CELL,
        horizontalRule: StructuralTextType.HORIZONTAL_DIVIDER,
        math: StructuralTextType.MATH,
        mention: StructuralTextType.MENTION
    }

    return mapping[type] || StructuralTextType.TEXT
}

/**
 * Converts StructuralText format to TipTap JSON.
 */
export function convertFromStructuralText(structuralText: StructuralText): any {
    const tipTapType = mapStructuralTypeToTipTapType(structuralText.type, structuralText)

    // Check if this should be converted to a mark
    if (MARK_TYPES.has(structuralText.type) && shouldConvertToMark(structuralText)) {
        return convertToMarkFormat(structuralText)
    }

    const result: any = {
        type: tipTapType
    }

    // Add text content for text nodes
    if (structuralText.type === StructuralTextType.TEXT && structuralText.content) {
        result.text = structuralText.content
    }

    // Add attributes
    const attrs = buildNodeAttributes(structuralText)
    if (Object.keys(attrs).length > 0) {
        result.attrs = attrs
    }

    if (structuralText.children && structuralText.children.length > 0) {
        result.content = structuralText.children.map(child => convertFromStructuralText(child))
    } else if (structuralText.content) {
        // Only when there are NO children, expand content to appropriate structure
        if (needsParagraphWrapper(structuralText.type)) {
            result.content = [{
                type: 'paragraph',
                content: [{
                    type: 'text',
                    text: structuralText.content
                }]
            }]
        } else if (needsTextNode(structuralText.type)) {
            result.content = [{
                type: 'text',
                text: structuralText.content
            }]
        }
    }

    return result
}

/**
 * Determines if a node type needs paragraph wrapper when converting back to TipTap.
 * TipTap's heading and listItem typically contain paragraph nodes.
 */
function needsParagraphWrapper(type: StructuralTextType): boolean {
    return type === StructuralTextType.HEADING ||
        type === StructuralTextType.LIST_ITEM
}

/**
 * Determines if a node type needs direct text children (without paragraph wrapper).
 */
function needsTextNode(type: StructuralTextType): boolean {
    return type === StructuralTextType.PARAGRAPH ||
        type === StructuralTextType.BLOCKQUOTE ||
        type === StructuralTextType.TABLE_CELL ||
        type === StructuralTextType.CODE_BLOCK ||
        MARK_TYPES.has(type)
}

/**
 * Determines if a StructuralText node should be converted to TipTap mark format.
 */
function shouldConvertToMark(node: StructuralText): boolean {
    // If it has exactly one child, it should be unwrapped into a mark
    return node.children.length === 1
}

/**
 * Converts a StructuralText node to TipTap mark format.
 * This unwraps mark nodes and applies them as marks to the child node.
 */
function convertToMarkFormat(node: StructuralText): any {
    const marks: any[] = []
    let currentNode = node

    // Collect all marks from nested mark nodes
    while (MARK_TYPES.has(currentNode.type) && currentNode.children.length === 1) {
        const mark: any = {
            type: mapStructuralTypeToTipTapType(currentNode.type, currentNode)
        }

        // Add mark attributes if needed
        if (currentNode.type === StructuralTextType.LINK) {
            const linkNode = currentNode as LinkElement
            mark.attrs = {
                href: linkNode.href
            }
            if (linkNode.title) {
                mark.attrs.title = linkNode.title
            }
        }

        marks.push(mark)
        currentNode = currentNode.children[0]!
    }

    // Convert the innermost node
    const result = convertFromStructuralText(currentNode)

    // Apply marks to the result
    if (marks.length > 0) {
        if (result.marks) {
            result.marks = [...result.marks, ...marks]
        } else {
            result.marks = marks
        }
    }

    return result
}

/**
 * Builds TipTap node attributes from StructuralText.
 */
function buildNodeAttributes(structuralText: StructuralText): any {
    const attrs: any = {}

    switch (structuralText.type) {
        case StructuralTextType.HEADING:
            const heading = structuralText as HeadingElement
            if (heading.level !== undefined) {
                attrs.level = heading.level
            }
            break

        case StructuralTextType.CODE_BLOCK:
            const codeBlock = structuralText as CodeBlockElement
            if (codeBlock.language !== undefined) {
                attrs.language = codeBlock.language
            }
            break

        case StructuralTextType.LINK:
            const link = structuralText as LinkElement
            attrs.href = link.href
            if (link.title) {
                attrs.title = link.title
            }
            break

        case StructuralTextType.IMAGE:
            const image = structuralText as ImageElement
            attrs.src = image.src
            if (image.alt) {
                attrs.alt = image.alt
            }
            if (image.title) {
                attrs.title = image.title
            }
            break

        case StructuralTextType.MENTION:
            const mention = structuralText as MentionElement
            attrs.id = mention.userId
            break
    }

    return attrs
}

function mapStructuralTypeToTipTapType(type: StructuralTextType, node?: StructuralText): string {
    // Special handling for LIST type - check listType
    if (type === StructuralTextType.LIST_BLOCK && node) {
        const listNode = node as ListElement
        if (listNode.listType === ListType.TASK) {
            return 'taskList'
        } else if (listNode.listType === ListType.ORDERED) {
            return 'orderedList'
        }
        return 'bulletList'
    }

    // Special handling for LIST_ITEM - check if it has checked property (task item)
    if (type === StructuralTextType.LIST_ITEM && node) {
        const itemNode = node as ListItemElement
        if (itemNode.checked !== undefined) {
            return 'taskItem'
        }
    }

    const mapping: Record<StructuralTextType, string> = {
        [StructuralTextType.DOCUMENT]: 'doc',
        [StructuralTextType.PARAGRAPH]: 'paragraph',
        [StructuralTextType.HEADING]: 'heading',
        [StructuralTextType.LIST_BLOCK]: 'bulletList',
        [StructuralTextType.LIST_ITEM]: 'listItem',
        [StructuralTextType.BLOCKQUOTE]: 'blockquote',
        [StructuralTextType.CODE_BLOCK]: 'codeBlock',
        [StructuralTextType.INLINE_CODE]: 'code',
        [StructuralTextType.BOLD]: 'bold',
        [StructuralTextType.ITALIC]: 'italic',
        [StructuralTextType.STRIKETHROUGH]: 'strike',
        [StructuralTextType.UNDERLINE]: 'underline',
        [StructuralTextType.HIGHLIGHT]: 'highlight',
        [StructuralTextType.TEXT]: 'text',
        [StructuralTextType.LINK]: 'link',
        [StructuralTextType.IMAGE]: 'image',
        [StructuralTextType.TABLE]: 'table',
        [StructuralTextType.TABLE_ROW]: 'tableRow',
        [StructuralTextType.TABLE_CELL]: 'tableCell',
        [StructuralTextType.HORIZONTAL_DIVIDER]: 'horizontalRule',
        [StructuralTextType.MATH]: 'math',
        [StructuralTextType.MENTION]: 'mention'
    }

    return mapping[type] || 'text'
}


