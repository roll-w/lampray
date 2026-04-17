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
import type {Node as ProseMirrorNode} from "@tiptap/pm/model";
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

export interface TableCellTarget {
    tableNode: ProseMirrorNode
    tableStart: number
    cellPos: number
    rowIndex: number
    columnIndex: number
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

function getTableContextAtPos(editor: Editor, pos: number) {
    const $pos = editor.state.doc.resolve(pos)

    for (let depth = $pos.depth; depth > 0; depth--) {
        const node = $pos.node(depth)
        if (node.type.name !== "table") {
            continue
        }

        return {
            tableNode: node,
            tableStart: $pos.start(depth),
        }
    }

    return null
}

function getCellPositionAtPos(editor: Editor, pos: number) {
    const exactNode = editor.state.doc.nodeAt(pos)
    if (exactNode && (exactNode.type.name === "tableCell" || exactNode.type.name === "tableHeader")) {
        return pos
    }

    const $pos = editor.state.doc.resolve(pos)

    for (let depth = $pos.depth; depth > 0; depth--) {
        const node = $pos.node(depth)
        if (node.type.name !== "tableCell" && node.type.name !== "tableHeader") {
            continue
        }

        return $pos.before(depth)
    }

    return null
}

function createCellTarget(tableNode: ProseMirrorNode, tableStart: number, rowIndex: number, columnIndex: number) {
    const tableMap = TableMap.get(tableNode)
    if (rowIndex < 0 || rowIndex >= tableMap.height || columnIndex < 0 || columnIndex >= tableMap.width) {
        return null
    }

    const mapIndex = rowIndex * tableMap.width + columnIndex
    const relativeCellPos = tableMap.map[mapIndex]
    if (relativeCellPos == null) {
        return null
    }

    return {
        tableNode,
        tableStart,
        cellPos: tableStart + relativeCellPos,
        rowIndex,
        columnIndex,
    }
}

function getCurrentTableTarget(editor: Editor) {
    const tableContext = getTableContextAtPos(editor, editor.state.selection.from)
    if (!tableContext) {
        return null
    }

    const currentCellPos = getCellPositionAtPos(editor, editor.state.selection.from)
    if (currentCellPos != null) {
        const tableMap = TableMap.get(tableContext.tableNode)
        const cellRect = tableMap.findCell(currentCellPos - tableContext.tableStart)

        return createCellTarget(tableContext.tableNode, tableContext.tableStart, cellRect.top, cellRect.left)
    }

    return createCellTarget(tableContext.tableNode, tableContext.tableStart, 0, 0)
}

function focusCellTarget(editor: Editor, target: TableCellTarget | null) {
    if (!target) {
        return false
    }

    const cellSelection = CellSelection.create(editor.state.doc, target.cellPos)
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

    const getCellTargetAtPos = (pos: number) => {
        const tableContext = getTableContextAtPos(editor, pos)
        if (!tableContext) {
            return null
        }

        const cellPos = getCellPositionAtPos(editor, pos)
        if (cellPos == null) {
            return null
        }

        const tableMap = TableMap.get(tableContext.tableNode)
        const cellRect = tableMap.findCell(cellPos - tableContext.tableStart)

        return createCellTarget(tableContext.tableNode, tableContext.tableStart, cellRect.top, cellRect.left)
    }

    const getCurrentCellTarget = () => {
        return getCurrentTableTarget(editor)
    }

    const resolveTarget = (target?: TableCellTarget | null) => {
        return target ?? getCurrentCellTarget()
    }

    const runOnTarget = (target: TableCellTarget | null | undefined, command: () => boolean) => {
        const resolvedTarget = resolveTarget(target)
        if (!focusCellTarget(editor, resolvedTarget)) {
            return false
        }

        return command()
    }

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

    const insertRowBeforeAtTarget = (target?: TableCellTarget | null) => {
        return runOnTarget(target, () => editor.chain().focus().addRowBefore().run())
    }

    const insertRowAfterAtTarget = (target?: TableCellTarget | null) => {
        return runOnTarget(target, () => editor.chain().focus().addRowAfter().run())
    }

    const deleteRowAtTarget = (target?: TableCellTarget | null) => {
        return runOnTarget(target, () => editor.chain().focus().deleteRow().run())
    }

    const insertColumnBeforeAtTarget = (target?: TableCellTarget | null) => {
        return runOnTarget(target, () => editor.chain().focus().addColumnBefore().run())
    }

    const insertColumnAfterAtTarget = (target?: TableCellTarget | null) => {
        return runOnTarget(target, () => editor.chain().focus().addColumnAfter().run())
    }

    const deleteColumnAtTarget = (target?: TableCellTarget | null) => {
        return runOnTarget(target, () => editor.chain().focus().deleteColumn().run())
    }

    const insertRowBefore = () => {
        return editor.chain().focus().addRowBefore().run()
    }

    const insertRowAfter = () => {
        return editor.chain().focus().addRowAfter().run()
    }

    const deleteRow = () => {
        return editor.chain().focus().deleteRow().run()
    }

    const insertColumnBefore = () => {
        return editor.chain().focus().addColumnBefore().run()
    }

    const insertColumnAfter = () => {
        return editor.chain().focus().addColumnAfter().run()
    }

    const deleteColumn = () => {
        return editor.chain().focus().deleteColumn().run()
    }

    const getRowHandleActions = (target: TableCellTarget | null) => {
        return [
            {
                key: "insert-row-before",
                label: t("editor.table.insertRowBefore"),
                icon: "i-lucide-arrow-up-to-line",
                disabled: !target,
                onSelect: () => insertRowBeforeAtTarget(target),
            },
            {
                key: "insert-row-after",
                label: t("editor.table.insertRowAfter"),
                icon: "i-lucide-arrow-down-to-line",
                disabled: !target,
                onSelect: () => insertRowAfterAtTarget(target),
            },
            {
                key: "delete-row",
                label: t("editor.table.deleteRow"),
                icon: "i-lucide-trash-2",
                color: "error" as const,
                disabled: !target,
                onSelect: () => deleteRowAtTarget(target),
            },
        ] satisfies TableAction[]
    }

    const getColumnHandleActions = (target: TableCellTarget | null) => {
        return [
            {
                key: "insert-column-before",
                label: t("editor.table.insertColumnBefore"),
                icon: "i-lucide-arrow-left-to-line",
                disabled: !target,
                onSelect: () => insertColumnBeforeAtTarget(target),
            },
            {
                key: "insert-column-after",
                label: t("editor.table.insertColumnAfter"),
                icon: "i-lucide-arrow-right-to-line",
                disabled: !target,
                onSelect: () => insertColumnAfterAtTarget(target),
            },
            {
                key: "delete-column",
                label: t("editor.table.deleteColumn"),
                icon: "i-lucide-trash-2",
                color: "error" as const,
                disabled: !target,
                onSelect: () => deleteColumnAtTarget(target),
            },
        ] satisfies TableAction[]
    }

    const columnActions = computed<TableActionGroup>(() => ({
        key: "columns",
        actions: [
            {
                key: "insert-column-before",
                label: t("editor.table.insertColumnBefore"),
                icon: "i-lucide-arrow-left-to-line",
                onSelect: insertColumnBefore,
            },
            {
                key: "insert-column-after",
                label: t("editor.table.insertColumnAfter"),
                icon: "i-lucide-arrow-right-to-line",
                onSelect: insertColumnAfter,
            },
            {
                key: "delete-column",
                label: t("editor.table.deleteColumn"),
                icon: "i-lucide-trash-2",
                color: "error",
                onSelect: deleteColumn,
            },
        ],
    }))

    const rowActions = computed<TableActionGroup>(() => ({
        key: "rows",
        actions: [
            {
                key: "insert-row-before",
                label: t("editor.table.insertRowBefore"),
                icon: "i-lucide-arrow-up-to-line",
                onSelect: insertRowBefore,
            },
            {
                key: "insert-row-after",
                label: t("editor.table.insertRowAfter"),
                icon: "i-lucide-arrow-down-to-line",
                onSelect: insertRowAfter,
            },
            {
                key: "delete-row",
                label: t("editor.table.deleteRow"),
                icon: "i-lucide-trash-2",
                color: "error",
                onSelect: deleteRow,
            },
        ],
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
                icon: "i-lucide-rows-2",
                onSelect: () => editor.chain().focus().toggleHeaderRow().run(),
            },
            {
                key: "toggle-header-column",
                label: t("editor.table.toggleHeaderColumn"),
                icon: "i-lucide-columns-2",
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

        return actions.filter((action): action is TableAction => !!action)
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
        isInTable,
        isInCell,
        getCellTargetAtPos,
        getCurrentCellTarget,
        getRowHandleActions,
        getColumnHandleActions,
        primaryToolbarActions,
        dropdownMenuItems,
        contextMenuItems,
    }
}
