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
import type {Selection} from "@tiptap/pm/state"
import type {Mappable} from "@tiptap/pm/transform"
import type {InjectionKey, Ref} from "vue"
import {inject, provide, ref} from "vue"

export type StructuralTextFloatingMenuId =
    | "context-menu"
    | "drag-handle-menu"
    | "table-row-menu"
    | "table-column-menu"

type SelectionBookmark = ReturnType<Selection["getBookmark"]>

export interface StructuralTextFloatingMenuState {
    activeMenu: Ref<StructuralTextFloatingMenuId | null>
    closeSignal: Ref<number>
    preservedTableSelection: Ref<SelectionBookmark | null>
    openMenu: (menuId: StructuralTextFloatingMenuId) => void
    closeMenu: (menuId: StructuralTextFloatingMenuId) => void
    closeAllMenus: () => void
    isAnotherMenuOpen: (menuId: StructuralTextFloatingMenuId | StructuralTextFloatingMenuId[]) => boolean
    preserveTableSelection: (selection: Selection | null | undefined) => void
    mapPreservedTableSelection: (mapping: Mappable) => void
    restorePreservedTableSelection: (editor: Editor) => boolean
    clearPreservedTableSelection: () => void
}

const structuralTextFloatingMenuStateKey = Symbol("structuralTextFloatingMenuState") as InjectionKey<StructuralTextFloatingMenuState>

export function createStructuralTextFloatingMenuState(): StructuralTextFloatingMenuState {
    const activeMenu = ref<StructuralTextFloatingMenuId | null>(null)
    const closeSignal = ref(0)
    const preservedTableSelection = ref<SelectionBookmark | null>(null)

    const clearPreservedTableSelection = () => {
        preservedTableSelection.value = null
    }

    const openMenu = (menuId: StructuralTextFloatingMenuId) => {
        activeMenu.value = menuId
    }

    const closeMenu = (menuId: StructuralTextFloatingMenuId) => {
        if (activeMenu.value === menuId) {
            activeMenu.value = null
        }

        if (menuId === "context-menu") {
            clearPreservedTableSelection()
        }
    }

    const closeAllMenus = () => {
        activeMenu.value = null
        closeSignal.value += 1
        clearPreservedTableSelection()
    }

    const isAnotherMenuOpen = (menuId: StructuralTextFloatingMenuId | StructuralTextFloatingMenuId[]) => {
        if (!activeMenu.value) {
            return false
        }

        const ownMenuIds = Array.isArray(menuId) ? menuId : [menuId]
        return !ownMenuIds.includes(activeMenu.value)
    }

    const preserveTableSelection = (selection: Selection | null | undefined) => {
        preservedTableSelection.value = selection?.getBookmark() ?? null
    }

    const mapPreservedTableSelection = (mapping: Mappable) => {
        const bookmark = preservedTableSelection.value
        if (!bookmark) {
            return
        }

        preservedTableSelection.value = bookmark.map(mapping)
    }

    const restorePreservedTableSelection = (editor: Editor) => {
        const bookmark = preservedTableSelection.value
        if (!bookmark) {
            return false
        }

        try {
            const selection = bookmark.resolve(editor.state.doc)
            editor.view.dispatch(editor.state.tr.setSelection(selection))
            editor.commands.focus()
            return true
        } catch (_error) {
            clearPreservedTableSelection()
            return false
        }
    }

    return {
        activeMenu,
        closeSignal,
        preservedTableSelection,
        openMenu,
        closeMenu,
        closeAllMenus,
        isAnotherMenuOpen,
        preserveTableSelection,
        mapPreservedTableSelection,
        restorePreservedTableSelection,
        clearPreservedTableSelection,
    }
}

export function provideStructuralTextFloatingMenuState(state: StructuralTextFloatingMenuState) {
    provide(structuralTextFloatingMenuStateKey, state)
    return state
}

export function useStructuralTextFloatingMenuState() {
    return inject(structuralTextFloatingMenuStateKey, null)
}
