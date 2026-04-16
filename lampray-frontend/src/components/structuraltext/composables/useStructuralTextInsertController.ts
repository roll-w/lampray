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

import type {Editor} from "@tiptap/core"
import type {InjectionKey, Ref} from "vue"
import {inject, provide, ref} from "vue"
import {applyLink, getLinkHref, getSelectedText, parseHttpUrl} from "@/components/structuraltext/composables/useEditorActions"
import type {TableInsertOptions} from "@/components/structuraltext/composables/useTableActions"

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
    confirmLink: (payload: { url: string; text?: string }) => boolean
    removeLink: () => boolean
    openImageModal: (options?: {
        url?: string
        alt?: string
    }) => void
    confirmImage: (payload: { url: string; alt?: string }) => boolean
    openTableInsertModal: (options?: Partial<TableInsertOptions>) => void
    confirmTableInsert: (options: TableInsertOptions) => boolean
}

const structuralTextInsertControllerKey = Symbol("structuralTextInsertController") as InjectionKey<StructuralTextInsertController>

function clampTableSize(value: number, fallback: number) {
    const normalizedValue = Number.isFinite(value) ? value : fallback
    return Math.min(10, Math.max(1, normalizedValue))
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

    const tableInitialRows = ref(3)
    const tableInitialCols = ref(3)
    const tableInitialWithHeaderRow = ref(true)

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

    const confirmLink = ({url, text}: { url: string; text?: string }) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const hasInserted = applyLink(currentEditor, url, text)
        if (hasInserted) {
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

    const confirmImage = ({url, alt}: { url: string; alt?: string }) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const parsedUrl = parseHttpUrl(url)
        if (!parsedUrl) {
            return false
        }

        const hasInserted = currentEditor.chain().focus().setImage({
            src: parsedUrl.toString(),
            alt: alt || undefined,
        }).run()

        if (hasInserted) {
            isImageModalOpen.value = false
        }

        return hasInserted
    }

    const openTableInsertModal = (options?: Partial<TableInsertOptions>) => {
        tableInitialRows.value = clampTableSize(options?.rows ?? 3, 3)
        tableInitialCols.value = clampTableSize(options?.cols ?? 3, 3)
        tableInitialWithHeaderRow.value = options?.withHeaderRow ?? true
        isTableInsertModalOpen.value = true
    }

    const confirmTableInsert = (options: TableInsertOptions) => {
        const currentEditor = editor.value
        if (!currentEditor) {
            return false
        }

        const hasInserted = currentEditor.chain().focus().insertTable({
            rows: clampTableSize(options.rows, 3),
            cols: clampTableSize(options.cols, 3),
            withHeaderRow: options.withHeaderRow,
        }).run()

        if (hasInserted) {
            isTableInsertModalOpen.value = false
        }

        return hasInserted
    }

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
        confirmLink,
        removeLink,
        openImageModal,
        confirmImage,
        openTableInsertModal,
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
