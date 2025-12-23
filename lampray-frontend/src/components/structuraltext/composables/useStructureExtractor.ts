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

import type {HeadingElement, StructuralText} from "../types"
import {StructuralTextType} from "../types"

/**
 * Represents a heading node in the document outline.
 */
export interface OutlineNode {
    /** Unique identifier for the heading */
    id: string
    /** Heading level (1-6) */
    level: number
    /** Text content of the heading */
    text: string
    /** Child heading nodes */
    children: OutlineNode[]
}

/**
 * Extracts text content from a StructuralText node recursively.
 */
function extractTextContent(node: StructuralText): string {
    if (node.type === StructuralTextType.TEXT) {
        return node.content
    }

    if (!node.children || node.children.length === 0) {
        return node.content
    }

    return node.children
        .map(child => extractTextContent(child))
        .join("")
}

/**
 * Generates a URL-safe ID from heading text.
 */
function generateHeadingId(text: string, index: number, level: number): string {
    const sanitized = text
        .toLowerCase()
        .trim()
        .replace(/\s+/g, "-")
        .replace(/[^\w\u4e00-\u9fa5-]/g, "")

    return sanitized ? `heading-${level}-${sanitized}` : `heading-${index}`
}

/**
 * Extracts document outline from StructuralText.
 *
 * @param document The StructuralText document to extract outline from
 * @returns Array of top-level outline nodes
 */
export function extractDocumentOutline(document: StructuralText): OutlineNode[] {
    const headings: Array<HeadingElement & { id: string }> = []
    let headingIndex = 0

    // Traverse the document to find all headings
    function traverse(node: StructuralText) {
        if (node.type === StructuralTextType.HEADING) {
            const heading = node as HeadingElement
            const text = extractTextContent(heading)
            const id = generateHeadingId(text, headingIndex++, heading.level)

            headings.push({
                ...heading,
                id,
                content: text
            })
        }

        if (node.children && node.children.length > 0) {
            node.children.forEach(child => traverse(child))
        }
    }

    traverse(document)

    // Build hierarchical structure
    const outline: OutlineNode[] = []
    const stack: OutlineNode[] = []

    for (const heading of headings) {
        const node: OutlineNode = {
            id: heading.id,
            level: heading.level,
            text: heading.content,
            children: []
        }

        // Find the correct parent in the stack
        while (stack.length > 0 && stack[stack.length - 1]!.level >= node.level) {
            stack.pop()
        }

        if (stack.length === 0) {
            // Top-level heading
            outline.push(node)
        } else {
            // Add as child to the most recent lower-level heading
            const parent = stack[stack.length - 1]
            if (parent) {
                parent.children.push(node)
            }
        }

        stack.push(node)
    }

    return outline
}

/**
 * Flattens the outline tree into a linear list.
 */
export function flattenOutline(outline: OutlineNode[]): OutlineNode[] {
    const result: OutlineNode[] = []

    function traverse(nodes: OutlineNode[]) {
        for (const node of nodes) {
            result.push(node)
            if (node.children.length > 0) {
                traverse(node.children)
            }
        }
    }

    traverse(outline)
    return result
}

