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
import {getBackgroundColorClass} from "@/components/structuraltext/utils/color.ts"

/**
 * Enhanced Table extension with custom attributes.
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
            widths: {
                default: null,
                parseHTML: element => {
                    const widths = element.getAttribute("data-widths")
                    return widths ? JSON.parse(widths) : null
                },
                renderHTML: attributes => {
                    if (!attributes.widths) {
                        return {}
                    }
                    return {
                        "data-widths": JSON.stringify(attributes.widths)
                    }
                }
            }
        }
    }
})

/**
 * Enhanced TableRow extension with row height and cell widths support.
 */
export const TableRow = TiptapTableRow.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            height: {
                default: null,
                parseHTML: element => {
                    const height = element.getAttribute("data-row-height")
                    return height ? parseFloat(height) : null
                },
                renderHTML: attributes => {
                    if (!attributes.height) {
                        return {}
                    }
                    return {
                        "data-row-height": attributes.height,
                        style: `height: ${attributes.height}px`
                    }
                }
            },
            widths: {
                default: null,
                parseHTML: element => {
                    const widths = element.getAttribute("data-widths")
                    return widths ? JSON.parse(widths) : null
                },
                renderHTML: attributes => {
                    if (!attributes.widths) {
                        return {}
                    }
                    return {
                        "data-widths": JSON.stringify(attributes.widths)
                    }
                }
            }
        }
    }
})

/**
 * Enhanced TableCell extension with alignment, background color, and span support.
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
                    const colwidth = element.getAttribute("colwidth")
                    return colwidth ? parseFloat(colwidth) : null
                },
                renderHTML: attributes => {
                    if (!attributes.width) {
                        return {}
                    }
                    return {
                        "data-width": attributes.width,
                        "colwidth": attributes.width,
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


