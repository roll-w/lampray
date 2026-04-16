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

interface EdgePosition {
    top: number
    left: number
}

const props = defineProps<Props>()
const {t} = useI18n()
const tableActions = useTableActions(props.editor, t)
const activeTableElement = ref<HTMLTableElement | null>(null)
const topPosition = ref<EdgePosition | null>(null)
const bottomPosition = ref<EdgePosition | null>(null)
const leftPosition = ref<EdgePosition | null>(null)
const rightPosition = ref<EdgePosition | null>(null)

let frameId = 0

const isVisible = computed(() => {
    return props.editable && tableActions.isInTable.value && !!activeTableElement.value && !!props.surface
})

function getNodeElement(node: Node | null) {
    if (!node) {
        return null
    }

    return node.nodeType === Node.TEXT_NODE ? node.parentElement : node as HTMLElement
}

function updatePositions() {
    if (!props.editable || !props.surface || !tableActions.isInTable.value) {
        activeTableElement.value = null
        topPosition.value = null
        bottomPosition.value = null
        leftPosition.value = null
        rightPosition.value = null
        return
    }

    const domAtPos = props.editor.view.domAtPos(props.editor.state.selection.from)
    const tableElement = getNodeElement(domAtPos.node)?.closest("table") as HTMLTableElement | null
    if (!tableElement) {
        activeTableElement.value = null
        topPosition.value = null
        bottomPosition.value = null
        leftPosition.value = null
        rightPosition.value = null
        return
    }

    const surfaceRect = props.surface.getBoundingClientRect()
    const tableRect = tableElement.getBoundingClientRect()
    const offset = 12

    activeTableElement.value = tableElement
    topPosition.value = {
        top: tableRect.top - surfaceRect.top - offset,
        left: tableRect.left - surfaceRect.left + tableRect.width / 2,
    }
    bottomPosition.value = {
        top: tableRect.bottom - surfaceRect.top + offset,
        left: tableRect.left - surfaceRect.left + tableRect.width / 2,
    }
    leftPosition.value = {
        top: tableRect.top - surfaceRect.top + tableRect.height / 2,
        left: tableRect.left - surfaceRect.left - offset,
    }
    rightPosition.value = {
        top: tableRect.top - surfaceRect.top + tableRect.height / 2,
        left: tableRect.right - surfaceRect.left + offset,
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

onMounted(() => {
    props.editor.on("selectionUpdate", scheduleUpdate)
    props.editor.on("focus", scheduleUpdate)
    props.editor.on("blur", scheduleUpdate)
    window.addEventListener("resize", scheduleUpdate)
    window.addEventListener("scroll", scheduleUpdate, true)
    scheduleUpdate()
})

onBeforeUnmount(() => {
    clearScheduledUpdate()
    props.editor.off("selectionUpdate", scheduleUpdate)
    props.editor.off("focus", scheduleUpdate)
    props.editor.off("blur", scheduleUpdate)
    window.removeEventListener("resize", scheduleUpdate)
    window.removeEventListener("scroll", scheduleUpdate, true)
})

watch(() => props.surface, () => {
    scheduleUpdate()
})

watch(() => props.editable, () => {
    scheduleUpdate()
})
</script>

<template>
    <div v-if="isVisible" class="pointer-events-none absolute inset-0 z-20">
        <UTooltip :text="t('editor.table.insertRowBefore')">
            <div
                    v-if="topPosition"
                    class="pointer-events-auto absolute"
                    :style="{
                        top: `${topPosition.top}px`,
                        left: `${topPosition.left}px`,
                        transform: 'translate(-50%, -50%)'
                    }"
            >
                <UButton
                        color="primary"
                        variant="soft"
                        size="xs"
                        icon="i-lucide-plus"
                        class="rounded-full border border-default shadow-sm"
                        @mousedown.prevent
                        @click="tableActions.insertRowAtTop"
                />
            </div>
        </UTooltip>

        <UTooltip :text="t('editor.table.insertRowAfter')">
            <div
                    v-if="bottomPosition"
                    class="pointer-events-auto absolute"
                    :style="{
                        top: `${bottomPosition.top}px`,
                        left: `${bottomPosition.left}px`,
                        transform: 'translate(-50%, -50%)'
                    }"
            >
                <UButton
                        color="primary"
                        variant="soft"
                        size="xs"
                        icon="i-lucide-plus"
                        class="rounded-full border border-default shadow-sm"
                        @mousedown.prevent
                        @click="tableActions.insertRowAtBottom"
                />
            </div>
        </UTooltip>

        <UTooltip :text="t('editor.table.insertColumnBefore')">
            <div
                    v-if="leftPosition"
                    class="pointer-events-auto absolute"
                    :style="{
                        top: `${leftPosition.top}px`,
                        left: `${leftPosition.left}px`,
                        transform: 'translate(-50%, -50%)'
                    }"
            >
                <UButton
                        color="primary"
                        variant="soft"
                        size="xs"
                        icon="i-lucide-plus"
                        class="rounded-full border border-default shadow-sm"
                        @mousedown.prevent
                        @click="tableActions.insertColumnAtLeft"
                />
            </div>
        </UTooltip>

        <UTooltip :text="t('editor.table.insertColumnAfter')">
            <div
                    v-if="rightPosition"
                    class="pointer-events-auto absolute"
                    :style="{
                        top: `${rightPosition.top}px`,
                        left: `${rightPosition.left}px`,
                        transform: 'translate(-50%, -50%)'
                    }"
            >
                <UButton
                        color="primary"
                        variant="soft"
                        size="xs"
                        icon="i-lucide-plus"
                        class="rounded-full border border-default shadow-sm"
                        @mousedown.prevent
                        @click="tableActions.insertColumnAtRight"
                />
            </div>
        </UTooltip>
    </div>
</template>
