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

import type {Editor, JSONContent} from "@tiptap/core"
import type {InjectionKey, Ref} from "vue"
import {inject, provide, ref, watch} from "vue"
import {
    applyLink,
    getLinkHref,
    getSelectedText,
    parseHttpUrl
} from "@/components/structuraltext/composables/useEditorActions"

export interface TableInsertOptions {
    rows: number
    cols: number
    withHeaderRow: boolean
}

export const DEFAULT_TABLE_INSERT_OPTIONS: TableInsertOptions = {
    rows: 3,
    cols: 3,
    withHeaderRow: true,
}

type PendingInsertionKind = "link" | "image" | "table"

interface PendingInsertionTarget {
    kind: PendingInsertionKind
    blockPos: number
}

interface ResolvedInsertionTarget {
    insertPos: number
    targetPos: number
}

export interface StructuralTextInsertController {
    isLinkModalOpen: Ref<boolean>
    isImageModalOpen: Ref<boolean>
    isTableInsertModalOpen: Ref<boolean>
    linkInitialUrl: Ref<string>
    linkInitialText: Ref<string>
    linkIsEditing: Ref<boolean>
    imageInitialUrl: Ref<string>
    imageInitialAlt: Ref<string>
    tableInitialRows: Ref<number>
    tableInitialCols: Ref<number>
    tableInitialWithHeaderRow: Ref<boolean>
    openLinkModal: (options?: {
        url?: string
        text?: string
        isEditing?: boolean
    }) => void
    openLinkModalFromSelection: () => void
    openLinkModalBelow: (blockPos: number) => boolean
    confirmLink: (payload: { url: string; text?: string }) => boolean
    removeLink: () => boolean
    openImageModal: (options?: {
        url?: string
        alt?: string
    }) => void
    openImageModalBelow: (blockPos: number) => boolean
    confirmImage: (payload: { url: string; alt?: string }) => boolean
    openTableInsertModal: (options?: Partial<TableInsertOptions>) => void
    openTableInsertModalBelow: (blockPos: number, options?: Partial<TableInsertOptions>) => boolean
    confirmTableInsert: (options: TableInsertOptions) => boolean
}

const structuralTextInsertControllerKey = Symbol("structuralTextInsertController") as InjectionKey<StructuralTextInsertController>

export function normalizeTableInsertSize(value: number | string | undefined, fallback: number) {
    const parsedValue = typeof value === "string" ? Number.parseInt(value, 10) : value
    const normalizedValue = Number.isFinite(parsedValue) ? parsedValue : fallback
    return Math.min(10, Math.max(1, normalizedValue))
}

export function normalizeTableInsertOptions(options?: Partial<TableInsertOptions>): TableInsertOptions {
    return {
        rows: normalizeTableInsertSize(options?.rows, DEFAULT_TABLE_INSERT_OPTIONS.rows),
        cols: normalizeTableInsertSize(options?.cols, DEFAULT_TABLE_INSERT_OPTIONS.cols),
        withHeaderRow: options?.withHeaderRow ?? DEFAULT_TABLE_INSERT_OPTIONS.withHeaderRow,
    }
}

export function createStructuralTextInsertController(editor: Ref<Editor | null>): StructuralTextInsertController {
    const isLinkModalOpen = ref(false)
    const isImageModalOpen = ref(false)
    const isTableInsertModalOpen = ref(false)

    const linkInitialUrl = ref("")
    const linkInitialText = ref("")
    const linkIsEditing = ref(false)

    const imageInitialUrl = ref("")
    const imageInitialAlt = ref("")

    const tableInitialRows = ref(DEFAULT_TABLE_INSERT_OPTIONS.rows)
    const tableInitialCols = ref(DEFAULT_TABLE_INSERT_OPTIONS.cols)
    const tableInitialWithHeaderRow = ref(DEFAULT_TABLE_INSERT_OPTIONS.withHeaderRow)
    const pendingInsertionTarget = ref<PendingInsertionTarget | null>(null)

    const clearPendingInsertionTarget = () => {
        pendingInsertionTarget.value = null
    }

    const getInsertionCandidatePositions = (currentEditor: Editor, blockPos: number, targetNodeSize: number) => {
        const resolvedPos = currentEditor.state.doc.resolve(blockPos)
        const candidatePositions = [blockPos + targetNodeSize]

        for (let depth = resolvedPos.depth; depth > 0; depth--) {
            candidatePositions.push(resolvedPos.after(depth))
        }

        return [...new Set(candidatePositions)]
    }

    const resolvePendingInsertTarget = (kind: PendingInsertionKind, content: JSONContent) => {
        const currentEditor = editor.value
        const target = pendingInsertionTarget.value
        if (!currentEditor || !target || target.kind !== kind) {
            return null
        }

        const targetNode = currentEditor.state.doc.nodeAt(target.blockPos)
        if (!targetNode || !targetNode.isBlock) {
            return null
        }

        const candidatePositions = getInsertionCandidatePositions(currentEditor, target.blockPos, targetNode.nodeSize)
        for (const candidatePos of candidatePositions) {
            if (!currentEditor.can().insertContentAt(candidatePos, content)) {
                continue
            }

            return {
                insertPos: candidatePos,
                targetPos: target.blockPos,
            } satisfies ResolvedInsertionTarget
        }

        return null
    }

    const preparePendingInsertionBelowBlock = (blockPos: number, kind: PendingInsertionKind) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const targetNode = currentEditor.state.doc.nodeAt(blockPos)
        if (!targetNode || !targetNode.isBlock) {
            return false
        }

        if (targetNode.type.name === "listItem" || targetNode.type.name === "taskItem") {
            return false
        }

        pendingInsertionTarget.value = {
            kind,
            blockPos,
        }

        return true
    }

    const buildTableNodeContent = (currentEditor: Editor, options: TableInsertOptions) => {
        const tableNode = currentEditor.schema.nodes.table
        const tableRowNode = currentEditor.schema.nodes.tableRow
        const tableCellNode = currentEditor.schema.nodes.tableCell
        const tableHeaderNode = currentEditor.schema.nodes.tableHeader
        const paragraphNode = currentEditor.schema.nodes.paragraph

        if (!tableNode || !tableRowNode || !tableCellNode || !tableHeaderNode || !paragraphNode) {
            return null
        }

        const emptyParagraph = {
            type: paragraphNode.name,
        }

        return {
            type: tableNode.name,
            content: Array.from({length: options.rows}, (_rowValue, rowIndex) => {
                const cellType = options.withHeaderRow && rowIndex === 0 ? tableHeaderNode.name : tableCellNode.name

                return {
                    type: tableRowNode.name,
                    content: Array.from({length: options.cols}, () => ({
                        type: cellType,
                        content: [emptyParagraph],
                    })),
                }
            }),
        }
    }

    const openLinkModal = (options?: {
        url?: string
        text?: string
        isEditing?: boolean
    }) => {
        linkInitialUrl.value = options?.url || ""
        linkInitialText.value = options?.text || ""
        linkIsEditing.value = options?.isEditing || false
        isLinkModalOpen.value = true
    }

    const openLinkModalFromSelection = () => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return
        }

        openLinkModal({
            url: getLinkHref(currentEditor),
            text: getSelectedText(currentEditor),
            isEditing: currentEditor.isActive("link"),
        })
    }

    const openLinkModalBelow = (blockPos: number) => {
        if (!preparePendingInsertionBelowBlock(blockPos, "link")) {
            return false
        }

        openLinkModal()
        return true
    }

    const confirmLink = ({url, text}: { url: string; text?: string }) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const parsedUrl = parseHttpUrl(url)
        if (!parsedUrl) {
            return false
        }

        const linkText = text?.trim() || parsedUrl.toString()
        const linkContent = {
            type: "paragraph",
            content: [
                {
                    type: "text",
                    text: linkText,
                    marks: [
                        {
                            type: "link",
                            attrs: {
                                href: parsedUrl.toString(),
                            },
                        }
                    ],
                }
            ],
        } satisfies JSONContent

        const resolvedInsertTarget = resolvePendingInsertTarget("link", linkContent)
        if (resolvedInsertTarget) {
            const hasInserted = currentEditor.chain().focus().insertContentAt(resolvedInsertTarget.insertPos, linkContent).run()

            if (hasInserted) {
                clearPendingInsertionTarget()
                isLinkModalOpen.value = false
            }

            return hasInserted
        }

        const hasInserted = applyLink(currentEditor, url, text)
        if (hasInserted) {
            clearPendingInsertionTarget()
            isLinkModalOpen.value = false
        }

        return hasInserted
    }

    const removeLink = () => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const hasRemoved = currentEditor.chain().focus().unsetLink().run()
        if (hasRemoved) {
            clearPendingInsertionTarget()
            isLinkModalOpen.value = false
        }

        return hasRemoved
    }

    const openImageModal = (options?: {
        url?: string
        alt?: string
    }) => {
        imageInitialUrl.value = options?.url || ""
        imageInitialAlt.value = options?.alt || ""
        isImageModalOpen.value = true
    }

    const openImageModalBelow = (blockPos: number) => {
        if (!preparePendingInsertionBelowBlock(blockPos, "image")) {
            return false
        }

        openImageModal()
        return true
    }

    const confirmImage = ({url, alt}: { url: string; alt?: string }) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const parsedUrl = parseHttpUrl(url)
        if (!parsedUrl) {
            return false
        }

        const imageContent = {
            type: "image",
            attrs: {
                src: parsedUrl.toString(),
                alt: alt || undefined,
            },
        } satisfies JSONContent

        const resolvedInsertTarget = resolvePendingInsertTarget("image", imageContent)
        if (resolvedInsertTarget) {
            const hasInsertedBelow = currentEditor.chain().focus().insertContentAt(resolvedInsertTarget.insertPos, imageContent).run()

            if (hasInsertedBelow) {
                clearPendingInsertionTarget()
                isImageModalOpen.value = false
            }

            return hasInsertedBelow
        }

        const hasInserted = currentEditor.chain().focus().setImage({
            src: parsedUrl.toString(),
            alt: alt || undefined,
        }).run()

        if (hasInserted) {
            clearPendingInsertionTarget()
            isImageModalOpen.value = false
        }

        return hasInserted
    }

    const openTableInsertModal = (options?: Partial<TableInsertOptions>) => {
        const normalizedOptions = normalizeTableInsertOptions(options)
        tableInitialRows.value = normalizedOptions.rows
        tableInitialCols.value = normalizedOptions.cols
        tableInitialWithHeaderRow.value = normalizedOptions.withHeaderRow
        isTableInsertModalOpen.value = true
    }

    const openTableInsertModalBelow = (blockPos: number, options?: Partial<TableInsertOptions>) => {
        if (!preparePendingInsertionBelowBlock(blockPos, "table")) {
            return false
        }

        openTableInsertModal(options)
        return true
    }

    const confirmTableInsert = (options: TableInsertOptions) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const normalizedOptions = normalizeTableInsertOptions(options)
        const tableContent = buildTableNodeContent(currentEditor, normalizedOptions)
        if (!tableContent) {
            return false
        }

        const resolvedInsertTarget = resolvePendingInsertTarget("table", tableContent)
        if (resolvedInsertTarget) {
            const hasInsertedBelow = currentEditor.chain().focus().insertContentAt(resolvedInsertTarget.insertPos, tableContent).run()

            if (hasInsertedBelow) {
                clearPendingInsertionTarget()
                isTableInsertModalOpen.value = false
            }

            return hasInsertedBelow
        }

        const hasInserted = currentEditor.chain().focus().insertTable(normalizedOptions).run()

        if (hasInserted) {
            clearPendingInsertionTarget()
            isTableInsertModalOpen.value = false
        }

        return hasInserted
    }

    watch(isLinkModalOpen, isOpen => {
        if (!isOpen) {
            clearPendingInsertionTarget()
        }
    })

    watch(isImageModalOpen, isOpen => {
        if (!isOpen) {
            clearPendingInsertionTarget()
        }
    })

    watch(isTableInsertModalOpen, isOpen => {
        if (!isOpen) {
            clearPendingInsertionTarget()
        }
    })

    return {
        isLinkModalOpen,
        isImageModalOpen,
        isTableInsertModalOpen,
        linkInitialUrl,
        linkInitialText,
        linkIsEditing,
        imageInitialUrl,
        imageInitialAlt,
        tableInitialRows,
        tableInitialCols,
        tableInitialWithHeaderRow,
        openLinkModal,
        openLinkModalFromSelection,
        openLinkModalBelow,
        confirmLink,
        removeLink,
        openImageModal,
        openImageModalBelow,
        confirmImage,
        openTableInsertModal,
        openTableInsertModalBelow,
        confirmTableInsert,
    }
}

export function provideStructuralTextInsertController(controller: StructuralTextInsertController) {
    provide(structuralTextInsertControllerKey, controller)
    return controller
}

export function useStructuralTextInsertController() {
    const controller = inject(structuralTextInsertControllerKey, null)
    if (!controller) {
        throw new Error("StructuralText insert controller is not available.")
    }

    return controller
}
