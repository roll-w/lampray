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
import {useTableActions} from "@/components/structuraltext/composables/useTableActions"

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

const hoveredCellPos = ref<number | null>(null)
const pinnedCellPos = ref<number | null>(null)
const rowHandlePosition = ref<HandlePosition | null>(null)
const columnHandlePosition = ref<HandlePosition | null>(null)
const rowMenuOpen = ref(false)
const columnMenuOpen = ref(false)
const supportsHoverHandles = ref(false)

let frameId = 0
let hoverCapabilityQuery: MediaQueryList | null = null

const isMenuOpen = computed(() => rowMenuOpen.value || columnMenuOpen.value)
const currentSelectionCellPos = computed(() => tableActions.getCurrentCellTarget()?.cellPos ?? null)
const activeCellPos = computed(() => {
    if (isMenuOpen.value) {
        return pinnedCellPos.value ?? hoveredCellPos.value ?? currentSelectionCellPos.value
    }

    return hoveredCellPos.value ?? currentSelectionCellPos.value
})
const activeTarget = computed(() => {
    if (activeCellPos.value == null) {
        return null
    }

    return tableActions.getCellTargetAtPos(activeCellPos.value)
})

const rowHandleActions = computed(() => tableActions.getRowHandleActions(activeTarget.value))
const columnHandleActions = computed(() => tableActions.getColumnHandleActions(activeTarget.value))
const affordanceMenuClass = "min-w-48 rounded-xl border border-default bg-default/95 p-1.5 shadow-xl backdrop-blur-sm"
const affordanceMenuItemClass = "justify-start rounded-lg px-2.5 py-1.5"

const isVisible = computed(() => {
    return props.editable &&
        supportsHoverHandles.value &&
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

function getCellPosFromElement(cellElement: HTMLTableCellElement) {
    try {
        const domPos = props.editor.view.posAtDOM(cellElement, 0)
        return tableActions.getCellTargetAtPos(domPos)?.cellPos ?? null
    } catch (_error) {
        return null
    }
}

function getCellElementAtPos(pos: number) {
    const domNode = props.editor.view.nodeDOM(pos)
    return domNode instanceof HTMLTableCellElement ? domNode : null
}

function clearHoverTarget() {
    hoveredCellPos.value = null
    if (!isMenuOpen.value && currentSelectionCellPos.value == null) {
        pinnedCellPos.value = null
    }
}

function updatePositions() {
    if (!props.editable || !props.surface || !supportsHoverHandles.value) {
        rowHandlePosition.value = null
        columnHandlePosition.value = null
        return
    }

    const cellPos = activeCellPos.value
    if (cellPos == null) {
        rowHandlePosition.value = null
        columnHandlePosition.value = null
        return
    }

    const cellElement = getCellElementAtPos(cellPos)
    const target = activeTarget.value
    if (!cellElement || !target) {
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

    const cellPos = getCellPosFromElement(cellElement)
    hoveredCellPos.value = cellPos
    if (!isMenuOpen.value) {
        pinnedCellPos.value = cellPos
    }
    scheduleUpdate()
}

function handleSurfaceMouseLeave() {
    clearHoverTarget()
    scheduleUpdate()
}

function handleSelectionChange() {
    if (!isMenuOpen.value && hoveredCellPos.value == null) {
        pinnedCellPos.value = currentSelectionCellPos.value
    }

    scheduleUpdate()
}

function handleMenuOpenChange(isOpen: boolean) {
    if (isOpen) {
        pinnedCellPos.value = activeCellPos.value ?? tableActions.getCurrentCellTarget()?.cellPos ?? null
    } else if (!isMenuOpen.value && hoveredCellPos.value == null) {
        pinnedCellPos.value = null
    }

    scheduleUpdate()
}

function handleActionSelect(action: { onSelect: () => void }, closeMenu: () => void) {
    action.onSelect()
    closeMenu()
    pinnedCellPos.value = tableActions.getCurrentCellTarget()?.cellPos ?? pinnedCellPos.value
    scheduleUpdate()
}

onMounted(() => {
    updateHoverCapability()
    hoverCapabilityQuery = window.matchMedia("(hover: hover) and (pointer: fine)")

    props.surface?.addEventListener("mousemove", handleSurfaceMouseMove)
    props.surface?.addEventListener("mouseleave", handleSurfaceMouseLeave)
    props.editor.on("selectionUpdate", handleSelectionChange)
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
        hoveredCellPos.value = null
        pinnedCellPos.value = null
    }

    scheduleUpdate()
})

watch(rowMenuOpen, handleMenuOpenChange)
watch(columnMenuOpen, handleMenuOpenChange)
</script>

<template>
    <div v-if="isVisible" class="pointer-events-none absolute inset-0 z-20">
        <UPopover v-model:open="rowMenuOpen">
            <div
                    v-if="rowHandlePosition"
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
                        variant="soft"
                        size="xs"
                        icon="i-lucide-grip-horizontal"
                        class="rounded-full border border-default bg-default/90 shadow-sm"
                        :aria-label="t('editor.table.rowActions')"
                        @mousedown.prevent
                />
            </div>

            <template #content>
                <div :class="affordanceMenuClass">
                    <div class="px-2 py-1 text-[11px] font-medium text-muted">
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

        <UPopover v-model:open="columnMenuOpen">
            <div
                    v-if="columnHandlePosition"
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
                        variant="soft"
                        size="xs"
                        icon="i-lucide-grip-vertical"
                        class="rounded-full border border-default bg-default/90 shadow-sm"
                        :aria-label="t('editor.table.columnActions')"
                        @mousedown.prevent
                />
            </div>

            <template #content>
                <div :class="affordanceMenuClass">
                    <div class="px-2 py-1 text-[11px] font-medium text-muted">
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
