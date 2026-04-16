/*
 * Copyright (C) 2023-2026 RollW
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
import type {ContextMenuItem, DropdownMenuItem} from "@nuxt/ui";
import {computed} from "vue";
import {CellSelection, TableMap} from "@tiptap/pm/tables";
import type {AttributeColor} from "@/components/structuraltext/types";
import {BASIC_COLORS} from "@/components/structuraltext/utils/color.ts";

type Translate = (key: string) => string
type TableActionColor = "neutral" | "primary" | "error"

export interface TableInsertOptions {
    rows: number
    cols: number
    withHeaderRow: boolean
}

export interface TableAction {
    key: string
    label: string
    icon: string
    color?: TableActionColor
    disabled?: boolean
    onSelect: () => void
}

export interface TableActionGroup {
    key: string
    actions: TableAction[]
}

const MIN_TABLE_SIZE = 1
const MAX_TABLE_SIZE = 10

function clampTableSize(value: number) {
    return Math.min(MAX_TABLE_SIZE, Math.max(MIN_TABLE_SIZE, value))
}

function mapActionToContextMenuItem(action: TableAction): ContextMenuItem {
    return {
        label: action.label,
        icon: action.icon,
        disabled: action.disabled,
        color: action.color,
        onSelect: () => action.onSelect(),
    }
}

function mapActionToDropdownMenuItem(action: TableAction): DropdownMenuItem {
    return {
        label: action.label,
        icon: action.icon,
        disabled: action.disabled,
        color: action.color,
        onSelect: () => action.onSelect(),
    }
}

type TableEdge = "top" | "bottom" | "left" | "right"

function getTableContext(editor: Editor) {
    const {$from} = editor.state.selection

    for (let depth = $from.depth; depth > 0; depth--) {
        const node = $from.node(depth)
        if (node.type.name !== "table") {
            continue
        }

        return {
            tableNode: node,
            tableStart: $from.start(depth),
        }
    }

    return null
}

function focusTableEdge(editor: Editor, edge: TableEdge) {
    const tableContext = getTableContext(editor)
    if (!tableContext) {
        return false
    }

    const tableMap = TableMap.get(tableContext.tableNode)

    const targetRow = edge === "bottom" ? tableMap.height - 1 : 0
    const targetColumn = edge === "right" ? tableMap.width - 1 : 0
    const mapIndex = targetRow * tableMap.width + targetColumn
    const targetCellStart = tableMap.map[mapIndex]
    if (!targetCellStart) {
        return false
    }

    const cellPosition = tableContext.tableStart + targetCellStart
    const cellSelection = CellSelection.create(editor.state.doc, cellPosition)

    editor.view.dispatch(editor.state.tr.setSelection(cellSelection))
    editor.commands.focus()
    return true
}

export function useTableActions(editor: Editor, t: Translate) {
    const isInTable = computed(() => editor.isActive("table"))
    const isInCell = computed(() => editor.isActive("tableCell") || editor.isActive("tableHeader"))
    const canMergeCells = computed(() => editor.can().mergeCells())
    const canSplitCell = computed(() => editor.can().splitCell())

    const colors = BASIC_COLORS

    const insertTable = ({rows, cols, withHeaderRow}: TableInsertOptions) => {
        return editor.chain().focus().insertTable({
            rows: clampTableSize(rows),
            cols: clampTableSize(cols),
            withHeaderRow,
        }).run()
    }

    const selectColor = (color: AttributeColor | null) => {
        editor.chain().focus().updateAttributes("tableCell", {backgroundColor: color}).run()
        editor.chain().focus().updateAttributes("tableHeader", {backgroundColor: color}).run()
    }

    const insertRowAtTop = () => {
        if (!focusTableEdge(editor, "top")) {
            return false
        }

        return editor.chain().focus().addRowBefore().run()
    }

    const insertRowAtBottom = () => {
        if (!focusTableEdge(editor, "bottom")) {
            return false
        }

        return editor.chain().focus().addRowAfter().run()
    }

    const insertColumnAtLeft = () => {
        if (!focusTableEdge(editor, "left")) {
            return false
        }

        return editor.chain().focus().addColumnBefore().run()
    }

    const insertColumnAtRight = () => {
        if (!focusTableEdge(editor, "right")) {
            return false
        }

        return editor.chain().focus().addColumnAfter().run()
    }

    const columnActions = computed<TableActionGroup>(() => ({
        key: "columns",
        actions: [
            {
                key: "insert-column-before",
                label: t("editor.table.insertColumnBefore"),
                icon: "i-lucide-columns-2",
                onSelect: () => editor.chain().focus().addColumnBefore().run(),
            },
            {
                key: "insert-column-after",
                label: t("editor.table.insertColumnAfter"),
                icon: "i-lucide-columns-2",
                onSelect: () => editor.chain().focus().addColumnAfter().run(),
            },
            {
                key: "delete-column",
                label: t("editor.table.deleteColumn"),
                icon: "i-lucide-trash-2",
                color: "error",
                onSelect: () => editor.chain().focus().deleteColumn().run(),
            }
        ]
    }))

    const rowActions = computed<TableActionGroup>(() => ({
        key: "rows",
        actions: [
            {
                key: "insert-row-before",
                label: t("editor.table.insertRowBefore"),
                icon: "i-lucide-rows-2",
                onSelect: () => editor.chain().focus().addRowBefore().run(),
            },
            {
                key: "insert-row-after",
                label: t("editor.table.insertRowAfter"),
                icon: "i-lucide-rows-2",
                onSelect: () => editor.chain().focus().addRowAfter().run(),
            },
            {
                key: "delete-row",
                label: t("editor.table.deleteRow"),
                icon: "i-lucide-trash-2",
                color: "error",
                onSelect: () => editor.chain().focus().deleteRow().run(),
            }
        ]
    }))

    const cellActions = computed<TableActionGroup>(() => ({
        key: "cells",
        actions: isInCell.value ? [
            {
                key: "merge-cells",
                label: t("editor.table.mergeCells"),
                icon: "i-lucide-merge",
                disabled: !canMergeCells.value,
                onSelect: () => editor.chain().focus().mergeCells().run(),
            },
            {
                key: "split-cell",
                label: t("editor.table.splitCell"),
                icon: "i-lucide-ungroup",
                disabled: !canSplitCell.value,
                onSelect: () => editor.chain().focus().splitCell().run(),
            }
        ] : []
    }))

    const headerActions = computed<TableActionGroup>(() => ({
        key: "headers",
        actions: [
            {
                key: "toggle-header-row",
                label: t("editor.table.toggleHeaderRow"),
                icon: "i-lucide-rows",
                onSelect: () => editor.chain().focus().toggleHeaderRow().run(),
            },
            {
                key: "toggle-header-column",
                label: t("editor.table.toggleHeaderColumn"),
                icon: "i-lucide-columns",
                onSelect: () => editor.chain().focus().toggleHeaderColumn().run(),
            },
            {
                key: "toggle-header-cell",
                label: t("editor.table.toggleHeaderCell"),
                icon: "i-lucide-square-stack",
                disabled: !isInCell.value,
                onSelect: () => editor.chain().focus().toggleHeaderCell().run(),
            }
        ]
    }))

    const destructiveActions = computed<TableActionGroup>(() => ({
        key: "danger",
        actions: [
            {
                key: "delete-table",
                label: t("editor.table.deleteTable"),
                icon: "i-lucide-trash-2",
                color: "error",
                onSelect: () => editor.chain().focus().deleteTable().run(),
            }
        ]
    }))

    const primaryToolbarActions = computed<TableAction[]>(() => {
        if (!isInTable.value) {
            return []
        }

        const actions = [
            rowActions.value.actions[1],
            columnActions.value.actions[1],
            headerActions.value.actions[0],
        ]

        if (cellActions.value.actions.length > 0) {
            actions.push(...cellActions.value.actions)
        }

        return actions
    })

    const overflowActionGroups = computed<TableActionGroup[]>(() => {
        if (!isInTable.value) {
            return []
        }

        return [
            columnActions.value,
            rowActions.value,
            ...(cellActions.value.actions.length > 0 ? [cellActions.value] : []),
            headerActions.value,
            destructiveActions.value,
        ]
    })

    const dropdownMenuItems = computed<DropdownMenuItem[][]>(() => {
        return overflowActionGroups.value
            .filter(group => group.actions.length > 0)
            .map(group => group.actions.map(mapActionToDropdownMenuItem))
    })

    const contextMenuItems = computed<ContextMenuItem[][]>(() => {
        if (!isInTable.value) {
            return []
        }

        const items: ContextMenuItem[][] = [
            columnActions.value.actions.map(mapActionToContextMenuItem),
            rowActions.value.actions.map(mapActionToContextMenuItem),
        ]

        if (cellActions.value.actions.length > 0) {
            items.push(cellActions.value.actions.map(mapActionToContextMenuItem))
            items.push([
                {
                    label: t("editor.table.backgroundColor"),
                    icon: "i-lucide-palette",
                    children: [
                        {
                            type: "label",
                            slot: "color-grid" as const,
                            onSelect: (event: Event) => {
                                event.stopPropagation()
                            }
                        }
                    ]
                }
            ])
        }

        items.push(headerActions.value.actions.map(mapActionToContextMenuItem))
        items.push(destructiveActions.value.actions.map(mapActionToContextMenuItem))

        return items
    })

    return {
        colors,
        insertTable,
        selectColor,
        insertRowAtTop,
        insertRowAtBottom,
        insertColumnAtLeft,
        insertColumnAtRight,
        isInTable,
        isInCell,
        primaryToolbarActions,
        dropdownMenuItems,
        contextMenuItems,
    }
}
