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

import {Heading as TiptapHeading} from "@tiptap/extension-heading"
import type {Node as ProseMirrorNode} from "@tiptap/pm/model"

/**
 * Custom Heading extension that adds unique IDs to headings for outline navigation.
 */
export const HeadingWithId = TiptapHeading.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            id: {
                default: null,
                parseHTML: element => element.getAttribute("id"),
                renderHTML: attributes => {
                    if (!attributes.id) {
                        return {}
                    }
                    return {
                        id: attributes.id
                    }
                }
            }
        }
    },

    addNodeView() {
        return ({ node, HTMLAttributes }) => {
            const level = node.attrs.level
            const dom = document.createElement(`h${level}`)

            // Generate ID from text content
            const text = node.textContent
            const id = generateHeadingId(text, level)

            Object.entries(HTMLAttributes).forEach(([key, value]) => {
                if (key !== "id") {
                    dom.setAttribute(key, value as string)
                }
            })

            dom.setAttribute("id", id)

            const contentDOM = document.createElement("span")
            dom.appendChild(contentDOM)

            return {
                dom,
                contentDOM,
                update: (updatedNode: ProseMirrorNode) => {
                    if (updatedNode.type !== node.type) {
                        return false
                    }

                    // Update ID when content changes
                    const newText = updatedNode.textContent
                    const newId = generateHeadingId(newText, updatedNode.attrs.level)
                    dom.setAttribute("id", newId)

                    return true
                }
            }
        }
    }
})

/**
 * Generates a URL-safe ID from heading text.
 */
function generateHeadingId(text: string, level: number): string {
    const sanitized = text
        .toLowerCase()
        .trim()
        .replace(/\s+/g, "-")
        .replace(/[^\w\u4e00-\u9fa5-]/g, "")

    return sanitized ? `heading-${level}-${sanitized}` : `heading-level-${level}`
}

