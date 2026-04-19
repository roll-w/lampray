<!--
  - Copyright (C) 2023-2026 RollW
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup lang="ts">
import type {Editor} from "@tiptap/core"
import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue"
import {useI18n} from "vue-i18n"
import {
    useStructuralTextFloatingMenuState
} from "@/components/structuraltext/composables/useStructuralTextFloatingMenuState"
import {type TableCellTargetReference, useTableActions} from "@/components/structuraltext/composables/useTableActions"
import {
    editorCompactFloatingSurfaceClass,
    editorMenuItemClass,
    editorRoundHandleButtonClass,
    editorSectionLabelClass,
} from "@/components/structuraltext/editorUi"

interface Props {
    editor: Editor
    editable: boolean
    surface: HTMLElement | null
}

interface HandlePosition {
    top: number
    left: number
}

const props = defineProps<Props>()
const {t} = useI18n()
const tableActions = useTableActions(props.editor, t)
const floatingMenuState = useStructuralTextFloatingMenuState()

const hoveredTargetRef = ref<TableCellTargetReference | null>(null)
const pinnedTargetRef = ref<TableCellTargetReference | null>(null)
const rowHandlePosition = ref<HandlePosition | null>(null)
const columnHandlePosition = ref<HandlePosition | null>(null)
const rowMenuOpen = ref(false)
const columnMenuOpen = ref(false)
const supportsHoverHandles = ref(false)
const editorStateVersion = ref(0)
const rowHandleRef = ref<HTMLElement | null>(null)
const columnHandleRef = ref<HTMLElement | null>(null)

let frameId = 0
let hoverCapabilityQuery: MediaQueryList | null = null

const isMenuOpen = computed(() => rowMenuOpen.value || columnMenuOpen.value)
const currentSelectionTarget = computed(() => {
    void editorStateVersion.value
    return tableActions.getCurrentCellTarget()
})
const activeTarget = computed(() => {
    void editorStateVersion.value

    if (isMenuOpen.value) {
        return tableActions.resolveTargetReference(pinnedTargetRef.value)
                ?? tableActions.resolveTargetReference(hoveredTargetRef.value)
                ?? currentSelectionTarget.value
    }

    return tableActions.resolveTargetReference(hoveredTargetRef.value) ?? currentSelectionTarget.value
})
const rowHandleActions = computed(() => tableActions.getRowHandleActions(activeTarget.value))
const columnHandleActions = computed(() => tableActions.getColumnHandleActions(activeTarget.value))
const affordanceMenuClass = `min-w-48 ${editorCompactFloatingSurfaceClass}`
const affordanceMenuItemClass = editorMenuItemClass
const handleButtonClass = editorRoundHandleButtonClass
const sectionLabelClass = editorSectionLabelClass
const hasConflictingMenuOpen = computed(() => floatingMenuState?.isAnotherMenuOpen(["table-row-menu", "table-column-menu"]) ?? false)

const isVisible = computed(() => {
    return props.editable &&
        supportsHoverHandles.value &&
            !hasConflictingMenuOpen.value &&
        !!props.surface &&
        !!activeTarget.value &&
        !!rowHandlePosition.value &&
        !!columnHandlePosition.value
})

function updateHoverCapability() {
    if (typeof window === "undefined") {
        supportsHoverHandles.value = false
        return
    }

    const hasHoverPointer = window.matchMedia("(hover: hover) and (pointer: fine)").matches
    supportsHoverHandles.value = hasHoverPointer && window.innerWidth >= 768
}

function getCellElement(target: EventTarget | null) {
    if (!(target instanceof Element)) {
        return null
    }

    const candidate = target.closest("td, th")
    return candidate instanceof HTMLTableCellElement ? candidate : null
}

function getTargetReferenceFromElement(cellElement: HTMLTableCellElement) {
    try {
        const domPos = props.editor.view.posAtDOM(cellElement, 0)
        const target = tableActions.getCellTargetAtPos(domPos)
        return tableActions.createTargetReference(target)
    } catch (_error) {
        return null
    }
}

function getCellElementAtPos(pos: number) {
    const domNode = props.editor.view.nodeDOM(pos)
    return domNode instanceof HTMLTableCellElement ? domNode : null
}

function clearHoverTarget() {
    hoveredTargetRef.value = null
    if (!isMenuOpen.value && currentSelectionTarget.value == null) {
        pinnedTargetRef.value = null
    }
}

function updatePositions() {
    if (!props.editable || !props.surface || !supportsHoverHandles.value) {
        rowHandlePosition.value = null
        columnHandlePosition.value = null
        return
    }

    const target = activeTarget.value
    if (!target) {
        rowHandlePosition.value = null
        columnHandlePosition.value = null
        return
    }

    const cellElement = getCellElementAtPos(target.cellPos)
    if (!cellElement) {
        rowHandlePosition.value = null
        columnHandlePosition.value = null
        return
    }

    const tableElement = cellElement.closest("table")
    if (!(tableElement instanceof HTMLTableElement)) {
        rowHandlePosition.value = null
        columnHandlePosition.value = null
        return
    }

    const surfaceRect = props.surface.getBoundingClientRect()
    const tableRect = tableElement.getBoundingClientRect()
    const cellRect = cellElement.getBoundingClientRect()
    const offset = 12

    rowHandlePosition.value = {
        top: cellRect.top - surfaceRect.top + cellRect.height / 2,
        left: tableRect.left - surfaceRect.left - offset,
    }

    columnHandlePosition.value = {
        top: tableRect.top - surfaceRect.top - offset,
        left: cellRect.left - surfaceRect.left + cellRect.width / 2,
    }
}

function scheduleUpdate() {
    if (frameId) {
        cancelAnimationFrame(frameId)
    }

    frameId = window.requestAnimationFrame(() => {
        updatePositions()
        frameId = 0
    })
}

function clearScheduledUpdate() {
    if (!frameId) {
        return
    }

    cancelAnimationFrame(frameId)
    frameId = 0
}

function handleSurfaceMouseMove(event: MouseEvent) {
    if (!props.editable || !supportsHoverHandles.value) {
        clearHoverTarget()
        scheduleUpdate()
        return
    }

    if (hasConflictingMenuOpen.value) {
        clearHoverTarget()
        scheduleUpdate()
        return
    }

    if (isMenuOpen.value) {
        scheduleUpdate()
        return
    }

    const eventTarget = event.target
    if (eventTarget instanceof Element && eventTarget.closest("[data-table-affordance-handle='true']")) {
        return
    }

    const cellElement = getCellElement(eventTarget)
    if (!cellElement) {
        clearHoverTarget()
        scheduleUpdate()
        return
    }

    if (cellElement.querySelector(".column-resize-handle") === eventTarget ||
        (eventTarget instanceof Element && eventTarget.closest(".column-resize-handle"))) {
        return
    }

    const targetReference = getTargetReferenceFromElement(cellElement)
    hoveredTargetRef.value = targetReference
    if (!isMenuOpen.value) {
        pinnedTargetRef.value = targetReference
    }
    scheduleUpdate()
}

function handleSurfaceMouseLeave() {
    clearHoverTarget()
    scheduleUpdate()
}

function handleEditorTransaction() {
    editorStateVersion.value += 1
    scheduleUpdate()
}

function handleSelectionChange() {
    editorStateVersion.value += 1

    if (!isMenuOpen.value && hoveredTargetRef.value == null) {
        pinnedTargetRef.value = tableActions.createTargetReference(currentSelectionTarget.value)
    }

    scheduleUpdate()
}

function getStableTargetReference() {
    return tableActions.createTargetReference(activeTarget.value ?? currentSelectionTarget.value)
}

function openRowMenu() {
    const targetReference = getStableTargetReference()
    if (!targetReference) {
        return
    }

    floatingMenuState?.openMenu("table-row-menu")
    pinnedTargetRef.value = targetReference
    rowMenuOpen.value = true
    columnMenuOpen.value = false
    scheduleUpdate()
}

function openColumnMenu() {
    const targetReference = getStableTargetReference()
    if (!targetReference) {
        return
    }

    floatingMenuState?.openMenu("table-column-menu")
    pinnedTargetRef.value = targetReference
    columnMenuOpen.value = true
    rowMenuOpen.value = false
    scheduleUpdate()
}

function handleMenuOpenChange(isOpen: boolean, menuId: "table-row-menu" | "table-column-menu") {
    if (isOpen) {
        floatingMenuState?.openMenu(menuId)
        pinnedTargetRef.value = getStableTargetReference()
    } else if (!isMenuOpen.value && hoveredTargetRef.value == null) {
        floatingMenuState?.closeMenu(menuId)
        pinnedTargetRef.value = null
    } else {
        floatingMenuState?.closeMenu(menuId)
    }

    scheduleUpdate()
}

function handleActionSelect(action: { onSelect: () => void }, closeMenu: () => void) {
    action.onSelect()
    closeMenu()
    pinnedTargetRef.value = tableActions.createTargetReference(tableActions.getCurrentCellTarget()) ?? pinnedTargetRef.value
    scheduleUpdate()
}

onMounted(() => {
    updateHoverCapability()
    hoverCapabilityQuery = window.matchMedia("(hover: hover) and (pointer: fine)")

    props.surface?.addEventListener("mousemove", handleSurfaceMouseMove)
    props.surface?.addEventListener("mouseleave", handleSurfaceMouseLeave)
    props.editor.on("selectionUpdate", handleSelectionChange)
    props.editor.on("transaction", handleEditorTransaction)
    props.editor.on("focus", scheduleUpdate)
    props.editor.on("blur", scheduleUpdate)

    window.addEventListener("resize", updateHoverCapability)
    window.addEventListener("resize", scheduleUpdate)
    window.addEventListener("scroll", scheduleUpdate, true)
    hoverCapabilityQuery.addEventListener("change", updateHoverCapability)

    scheduleUpdate()
})

onBeforeUnmount(() => {
    clearScheduledUpdate()
    props.surface?.removeEventListener("mousemove", handleSurfaceMouseMove)
    props.surface?.removeEventListener("mouseleave", handleSurfaceMouseLeave)
    props.editor.off("selectionUpdate", handleSelectionChange)
    props.editor.off("transaction", handleEditorTransaction)
    props.editor.off("focus", scheduleUpdate)
    props.editor.off("blur", scheduleUpdate)

    window.removeEventListener("resize", updateHoverCapability)
    window.removeEventListener("resize", scheduleUpdate)
    window.removeEventListener("scroll", scheduleUpdate, true)
    hoverCapabilityQuery?.removeEventListener("change", updateHoverCapability)
})

watch(() => props.surface, (newSurface, oldSurface) => {
    oldSurface?.removeEventListener("mousemove", handleSurfaceMouseMove)
    oldSurface?.removeEventListener("mouseleave", handleSurfaceMouseLeave)
    newSurface?.addEventListener("mousemove", handleSurfaceMouseMove)
    newSurface?.addEventListener("mouseleave", handleSurfaceMouseLeave)
    scheduleUpdate()
})

watch(() => props.editable, () => {
    scheduleUpdate()
})

watch(() => tableActions.isInTable.value, inTable => {
    if (!inTable) {
        hoveredTargetRef.value = null
        pinnedTargetRef.value = null
    }

    scheduleUpdate()
})

watch(rowMenuOpen, isOpen => handleMenuOpenChange(isOpen, "table-row-menu"))
watch(columnMenuOpen, isOpen => handleMenuOpenChange(isOpen, "table-column-menu"))
watch(() => floatingMenuState?.activeMenu.value, activeMenu => {
    if (!activeMenu || activeMenu === "table-row-menu" || activeMenu === "table-column-menu") {
        return
    }

    if (rowMenuOpen.value || columnMenuOpen.value) {
        rowMenuOpen.value = false
        columnMenuOpen.value = false
        clearHoverTarget()
        scheduleUpdate()
    }
})
watch(() => floatingMenuState?.closeSignal.value, () => {
    rowMenuOpen.value = false
    columnMenuOpen.value = false
    clearHoverTarget()
    scheduleUpdate()
})
</script>

<template>
    <div v-if="isVisible" class="pointer-events-none absolute inset-0 z-20">
        <div
                v-if="rowHandlePosition"
                ref="rowHandleRef"
                data-table-affordance-handle="true"
                class="pointer-events-auto absolute"
                :style="{
                    top: `${rowHandlePosition.top}px`,
                    left: `${rowHandlePosition.left}px`,
                    transform: 'translate(-50%, -50%)'
                }"
        >
            <UButton
                    color="neutral"
                    variant="ghost"
                    size="xs"
                    icon="i-lucide-grip-horizontal"
                    :class="handleButtonClass"
                    :aria-label="t('editor.table.rowActions')"
                    @mousedown.prevent
                    @click.stop="openRowMenu"
            />
        </div>

        <UPopover v-model:open="rowMenuOpen" :content="{sideOffset: 4}" :reference="rowHandleRef">
            <template #content>
                <div :class="affordanceMenuClass">
                    <div :class="sectionLabelClass">
                        {{ t('editor.table.rowActions') }}
                    </div>
                    <div class="flex flex-col gap-0.5 p-1">
                        <UButton
                                v-for="action in rowHandleActions"
                                :key="action.key"
                                :color="action.color || 'neutral'"
                                :disabled="action.disabled"
                                variant="ghost"
                                size="xs"
                                :icon="action.icon"
                                :class="affordanceMenuItemClass"
                                @click="handleActionSelect(action, () => rowMenuOpen = false)"
                        >
                            {{ action.label }}
                        </UButton>
                    </div>
                </div>
            </template>
        </UPopover>

        <div
                v-if="columnHandlePosition"
                ref="columnHandleRef"
                data-table-affordance-handle="true"
                class="pointer-events-auto absolute"
                :style="{
                    top: `${columnHandlePosition.top}px`,
                    left: `${columnHandlePosition.left}px`,
                    transform: 'translate(-50%, -50%)'
                }"
        >
            <UButton
                    color="neutral"
                    variant="ghost"
                    size="xs"
                    icon="i-lucide-grip-vertical"
                    :class="handleButtonClass"
                    :aria-label="t('editor.table.columnActions')"
                    @mousedown.prevent
                    @click.stop="openColumnMenu"
            />
        </div>

        <UPopover v-model:open="columnMenuOpen" :content="{sideOffset: 4}" :reference="columnHandleRef">
            <template #content>
                <div :class="affordanceMenuClass">
                    <div :class="sectionLabelClass">
                        {{ t('editor.table.columnActions') }}
                    </div>
                    <div class="flex flex-col gap-0.5 p-1">
                        <UButton
                                v-for="action in columnHandleActions"
                                :key="action.key"
                                :color="action.color || 'neutral'"
                                :disabled="action.disabled"
                                variant="ghost"
                                size="xs"
                                :icon="action.icon"
                                :class="affordanceMenuItemClass"
                                @click="handleActionSelect(action, () => columnMenuOpen = false)"
                        >
                            {{ action.label }}
                        </UButton>
                    </div>
                </div>
            </template>
        </UPopover>
    </div>
</template>
