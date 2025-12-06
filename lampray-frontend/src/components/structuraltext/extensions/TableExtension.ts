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

import {
    Table as TiptapTable,
    TableCell as TiptapTableCell,
    TableHeader as TiptapTableHeader,
    TableRow as TiptapTableRow
} from "@tiptap/extension-table"
import {mergeAttributes} from "@tiptap/core"
import type {AttributeColor, TextAlignment} from "@/components/structuraltext/types"

/**
 * Enhanced Table extension with custom attributes.
 *
 * @author RollW
 */
export const Table = TiptapTable.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            hasHeaderRow: {
                default: false,
                parseHTML: element => element.getAttribute("data-header-row") === "true",
                renderHTML: attributes => {
                    if (!attributes.hasHeaderRow) {
                        return {}
                    }
                    return {
                        "data-header-row": "true"
                    }
                }
            },
            hasHeaderColumn: {
                default: false,
                parseHTML: element => element.getAttribute("data-header-column") === "true",
                renderHTML: attributes => {
                    if (!attributes.hasHeaderColumn) {
                        return {}
                    }
                    return {
                        "data-header-column": "true"
                    }
                }
            },
            columnWidths: {
                default: null,
                parseHTML: element => {
                    const widths = element.getAttribute("data-column-widths")
                    return widths ? JSON.parse(widths) : null
                },
                renderHTML: attributes => {
                    if (!attributes.columnWidths) {
                        return {}
                    }
                    return {
                        "data-column-widths": JSON.stringify(attributes.columnWidths)
                    }
                }
            }
        }
    }
})

/**
 * Enhanced TableRow extension with row height support.
 *
 * @author RollW
 */
export const TableRow = TiptapTableRow.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            rowHeight: {
                default: null,
                parseHTML: element => {
                    const height = element.getAttribute("data-row-height")
                    return height ? parseFloat(height) : null
                },
                renderHTML: attributes => {
                    if (!attributes.rowHeight) {
                        return {}
                    }
                    return {
                        "data-row-height": attributes.rowHeight,
                        style: `height: ${attributes.rowHeight}px`
                    }
                }
            }
        }
    }
})

/**
 * Enhanced TableCell extension with alignment, background color, and span support.
 *
 * @author RollW
 */
export const TableCell = TiptapTableCell.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            colspan: {
                default: 1,
                parseHTML: element => {
                    const colspan = element.getAttribute("colspan")
                    return colspan ? parseInt(colspan, 10) : 1
                },
                renderHTML: attributes => {
                    if (attributes.colspan === 1) {
                        return {}
                    }
                    return {
                        colspan: attributes.colspan
                    }
                }
            },
            rowspan: {
                default: 1,
                parseHTML: element => {
                    const rowspan = element.getAttribute("rowspan")
                    return rowspan ? parseInt(rowspan, 10) : 1
                },
                renderHTML: attributes => {
                    if (attributes.rowspan === 1) {
                        return {}
                    }
                    return {
                        rowspan: attributes.rowspan
                    }
                }
            },
            backgroundColor: {
                default: null,
                parseHTML: element => element.getAttribute("data-bg-color"),
                renderHTML: attributes => {
                    if (!attributes.backgroundColor) {
                        return {}
                    }
                    const bgClass = getBackgroundColorClass(attributes.backgroundColor)
                    return {
                        "data-bg-color": attributes.backgroundColor,
                        class: bgClass
                    }
                }
            },
            width: {
                default: null,
                parseHTML: element => {
                    const width = element.getAttribute("data-width")
                    if (width) {
                        return parseFloat(width)
                    }
                    const widthLegacy = element.getAttribute("colwidth")
                    return widthLegacy ? parseFloat(widthLegacy) : null
                },
                renderHTML: attributes => {
                    if (!attributes.width) {
                        return {}
                    }
                    return {
                        "data-width": attributes.width,
                        style: `width: ${attributes.width}px`
                    }
                }
            },
            height: {
                default: null,
                parseHTML: element => {
                    const height = element.getAttribute("data-height")
                    return height ? parseFloat(height) : null
                },
                renderHTML: attributes => {
                    if (!attributes.height) {
                        return {}
                    }
                    return {
                        "data-height": attributes.height,
                        style: `height: ${attributes.height}px`
                    }
                }
            },
            isHeader: {
                default: false,
                parseHTML: element => element.getAttribute("data-is-header") === "true",
                renderHTML: attributes => {
                    if (!attributes.isHeader) {
                        return {}
                    }
                    return {
                        "data-is-header": "true"
                    }
                }
            }
        }
    },

    renderHTML({HTMLAttributes}) {
        const attrs = mergeAttributes(this.options.HTMLAttributes, HTMLAttributes)

        // Merge alignment and background color classes
        const classes = ["border", "border-gray-300", "dark:border-gray-600", "px-4", "py-2"]
        if (attrs.class) {
            classes.push(attrs.class)
        }
        attrs.class = classes.join(" ")
        return ["td", attrs, 0]
    },


})

/**
 * Enhanced TableHeader extension with alignment and background color support.
 *
 * @author RollW
 */
export const TableHeader = TiptapTableHeader.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            colspan: {
                default: 1,
                parseHTML: element => {
                    const colspan = element.getAttribute("colspan")
                    return colspan ? parseInt(colspan, 10) : 1
                },
                renderHTML: attributes => {
                    if (attributes.colspan === 1) {
                        return {}
                    }
                    return {
                        colspan: attributes.colspan
                    }
                }
            },
            rowspan: {
                default: 1,
                parseHTML: element => {
                    const rowspan = element.getAttribute("rowspan")
                    return rowspan ? parseInt(rowspan, 10) : 1
                },
                renderHTML: attributes => {
                    if (attributes.rowspan === 1) {
                        return {}
                    }
                    return {
                        rowspan: attributes.rowspan
                    }
                }
            },
            backgroundColor: {
                default: null,
                parseHTML: element => element.getAttribute("data-bg-color"),
                renderHTML: attributes => {
                    if (!attributes.backgroundColor) {
                        return {}
                    }
                    const bgClass = getBackgroundColorClass(attributes.backgroundColor)
                    return {
                        "data-bg-color": attributes.backgroundColor,
                        class: bgClass
                    }
                }
            },
            width: {
                default: null,
                parseHTML: element => {
                    const width = element.getAttribute("data-width")
                    if (width) {
                        return parseFloat(width)
                    }
                    const widthLegacy = element.getAttribute("colwidth")
                    return widthLegacy ? parseFloat(widthLegacy) : null
                },
                renderHTML: attributes => {
                    if (!attributes.width) {
                        return {}
                    }
                    return {
                        "data-width": attributes.width,
                        style: `width: ${attributes.width}px`
                    }
                }
            },
            height: {
                default: null,
                parseHTML: element => {
                    const height = element.getAttribute("data-height")
                    return height ? parseFloat(height) : null
                },
                renderHTML: attributes => {
                    if (!attributes.height) {
                        return {}
                    }
                    return {
                        "data-height": attributes.height,
                        style: `height: ${attributes.height}px`
                    }
                }
            }
        }
    },

    renderHTML({HTMLAttributes}) {
        const attrs = mergeAttributes(this.options.HTMLAttributes, HTMLAttributes)

        // Merge alignment and background color classes
        const classes = ["border", "border-gray-300", "dark:border-gray-600", "px-4", "py-2", "bg-gray-100", "dark:bg-gray-800", "font-bold"]
        if (attrs.class) {
            classes.push(attrs.class)
        }
        attrs.class = classes.join(" ")

        return ["th", attrs, 0]
    }
})

function getAlignmentClass(alignment: TextAlignment): string {
    const alignmentMap: Record<TextAlignment, string> = {
        LEFT: "text-left",
        CENTER: "text-center",
        RIGHT: "text-right",
        JUSTIFY: "text-justify"
    }
    return alignmentMap[alignment] || "text-left"
}

function getBackgroundColorClass(color: AttributeColor): string {
    const colorMap: Record<string, string> = {
        "yellow": "bg-yellow-200 dark:bg-yellow-800",
        "green": "bg-green-200 dark:bg-green-800",
        "blue": "bg-blue-200 dark:bg-blue-800",
        "pink": "bg-pink-200 dark:bg-pink-800",
        "orange": "bg-orange-200 dark:bg-orange-800",
        "purple": "bg-purple-200 dark:bg-purple-800",
        "red": "bg-red-200 dark:bg-red-800",
        "lime": "bg-lime-200 dark:bg-lime-800",
        "teal": "bg-teal-200 dark:bg-teal-800",
        "cyan": "bg-cyan-200 dark:bg-cyan-800",
        "light-yellow": "bg-yellow-100 dark:bg-yellow-900",
        "light-green": "bg-green-100 dark:bg-green-900",
        "light-blue": "bg-blue-100 dark:bg-blue-900",
        "light-pink": "bg-pink-100 dark:bg-pink-900",
        "light-orange": "bg-orange-100 dark:bg-orange-900",
        "light-purple": "bg-purple-100 dark:bg-purple-900",
        "light-red": "bg-red-100 dark:bg-red-900",
        "light-lime": "bg-lime-100 dark:bg-lime-900",
        "light-teal": "bg-teal-100 dark:bg-teal-900",
        "light-cyan": "bg-cyan-100 dark:bg-cyan-900",
        "dark-yellow": "bg-yellow-300 dark:bg-yellow-700",
        "dark-green": "bg-green-300 dark:bg-green-700",
        "dark-blue": "bg-blue-300 dark:bg-blue-700",
        "dark-pink": "bg-pink-300 dark:bg-pink-700",
        "dark-orange": "bg-orange-300 dark:bg-orange-700",
        "dark-purple": "bg-purple-300 dark:bg-purple-700",
        "dark-red": "bg-red-300 dark:bg-red-700",
        "dark-lime": "bg-lime-300 dark:bg-lime-700",
        "dark-teal": "bg-teal-300 dark:bg-teal-700",
        "dark-cyan": "bg-cyan-300 dark:bg-cyan-700"
    }
    return colorMap[color] || ""
}

