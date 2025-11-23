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

/**
 * Types for structural text editor.
 *
 * @author RollW
 */
export enum StructuralTextType {
    DOCUMENT = 'DOCUMENT',
    PARAGRAPH = 'PARAGRAPH',
    HEADING = 'HEADING',
    LIST = 'LIST',
    LIST_ITEM = 'LIST_ITEM',
    BLOCKQUOTE = 'BLOCKQUOTE',
    CODE_BLOCK = 'CODE_BLOCK',
    INLINE_CODE = 'INLINE_CODE',
    BOLD = 'BOLD',
    ITALIC = 'ITALIC',
    STRIKETHROUGH = 'STRIKETHROUGH',
    UNDERLINE = 'UNDERLINE',
    HIGHLIGHT = 'HIGHLIGHT',
    TEXT = 'TEXT',
    LINK = 'LINK',
    IMAGE = 'IMAGE',
    TABLE = 'TABLE',
    TABLE_ROW = 'TABLE_ROW',
    TABLE_CELL = 'TABLE_CELL',
    HORIZONTAL_DIVIDER = 'HORIZONTAL_DIVIDER',
    MATH = 'MATH',
    MENTION = 'MENTION'
}

/**
 * Base interface for all structural text nodes.
 */
export interface StructuralText {
    type: StructuralTextType
    content: string
    children: StructuralText[]
}

/**
 * Document element - the root of a structural text document.
 */
export interface DocumentElement extends StructuralText {
    type: StructuralTextType.DOCUMENT
    content: ''
}

/**
 * Paragraph element - a block of text.
 */
export interface ParagraphElement extends StructuralText {
    type: StructuralTextType.PARAGRAPH
}

/**
 * Heading element - a heading with level 1-6.
 */
export interface HeadingElement extends StructuralText {
    type: StructuralTextType.HEADING
    level: number
}

/**
 * List element - ordered or unordered list.
 */
export interface ListElement extends StructuralText {
    type: StructuralTextType.LIST
    ordered: boolean
}

/**
 * List item element - an item in a list.
 */
export interface ListItemElement extends StructuralText {
    type: StructuralTextType.LIST_ITEM
}

/**
 * Blockquote element - a quoted block.
 */
export interface BlockquoteElement extends StructuralText {
    type: StructuralTextType.BLOCKQUOTE
}

/**
 * Code block element - a block of code with optional language.
 */
export interface CodeBlockElement extends StructuralText {
    type: StructuralTextType.CODE_BLOCK
    language?: string
}

/**
 * Inline code element - inline code text.
 */
export interface InlineCodeElement extends StructuralText {
    type: StructuralTextType.INLINE_CODE
}

/**
 * Bold element - bold inline text.
 */
export interface BoldElement extends StructuralText {
    type: StructuralTextType.BOLD
}

/**
 * Italic element - italic inline text.
 */
export interface ItalicElement extends StructuralText {
    type: StructuralTextType.ITALIC
}

/**
 * Strikethrough element - strikethrough inline text.
 */
export interface StrikethroughElement extends StructuralText {
    type: StructuralTextType.STRIKETHROUGH
}

/**
 * Underline element - underlined inline text.
 */
export interface UnderlineElement extends StructuralText {
    type: StructuralTextType.UNDERLINE
}

/**
 * Highlight element - highlighted inline text.
 */
export interface HighlightElement extends StructuralText {
    type: StructuralTextType.HIGHLIGHT
}

/**
 * Text element - plain text leaf node.
 */
export interface TextElement extends StructuralText {
    type: StructuralTextType.TEXT
    children: []
}

/**
 * Link element - a hyperlink with href and optional title.
 */
export interface LinkElement extends StructuralText {
    type: StructuralTextType.LINK
    href: string
    title?: string
    content: ''
}

/**
 * Image element - an image with src, alt, and optional title.
 */
export interface ImageElement extends StructuralText {
    type: StructuralTextType.IMAGE
    src: string
    alt?: string
    title?: string
}

/**
 * Table element - a table container.
 */
export interface TableElement extends StructuralText {
    type: StructuralTextType.TABLE
}

/**
 * Table row element - a row in a table.
 */
export interface TableRowElement extends StructuralText {
    type: StructuralTextType.TABLE_ROW
}

/**
 * Table cell element - a cell in a table row.
 */
export interface TableCellElement extends StructuralText {
    type: StructuralTextType.TABLE_CELL
}

/**
 * Horizontal divider element - a horizontal rule.
 */
export interface HorizontalDividerElement extends StructuralText {
    type: StructuralTextType.HORIZONTAL_DIVIDER
    content: ''
    children: []
}

/**
 * Math element - mathematical expression.
 */
export interface MathElement extends StructuralText {
    type: StructuralTextType.MATH
}

/**
 * Mention element - a user mention.
 */
export interface MentionElement extends StructuralText {
    type: StructuralTextType.MENTION
    userId: string
}

/**
 * Union type of all structural text elements.
 */
export type StructuralTextElement =
    | DocumentElement
    | ParagraphElement
    | HeadingElement
    | ListElement
    | ListItemElement
    | BlockquoteElement
    | CodeBlockElement
    | InlineCodeElement
    | BoldElement
    | ItalicElement
    | StrikethroughElement
    | UnderlineElement
    | HighlightElement
    | TextElement
    | LinkElement
    | ImageElement
    | TableElement
    | TableRowElement
    | TableCellElement
    | HorizontalDividerElement
    | MathElement
    | MentionElement

