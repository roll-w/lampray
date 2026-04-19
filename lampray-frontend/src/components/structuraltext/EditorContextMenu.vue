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
import type {Editor} from "@tiptap/core";
import {computed, ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {CellSelection} from "@tiptap/pm/tables";
import type {AttributeColor} from "@/components/structuraltext/types";
import {
    useStructuralTextFloatingMenuState
} from "@/components/structuraltext/composables/useStructuralTextFloatingMenuState";
import {useTableActions} from "@/components/structuraltext/composables/useTableActions";
import {
    editorColorButtonClass,
    editorContextMenuUi,
    editorMenuSurfaceClass,
    editorSectionLabelClass,
} from "@/components/structuraltext/editorUi";

interface Props {
    editor: Editor
    editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    editable: true
})

const {t} = useI18n()
const floatingMenuState = useStructuralTextFloatingMenuState()
const {
    colors: tableColors,
    selectColor: selectTableColor,
    isInTable,
    contextMenuItems,
} = useTableActions(props.editor, t)

const menuOpen = ref(false)
const colorMenuSurfaceClass = editorMenuSurfaceClass
const colorButtonClass = editorColorButtonClass
const contextMenuUi = editorContextMenuUi
const sectionLabelClass = editorSectionLabelClass

function isMultiCellSelection() {
    const selection = props.editor.state.selection
    return selection instanceof CellSelection && selection.$anchorCell.pos !== selection.$headCell.pos
}

function selectColor(color: AttributeColor | null) {
    selectTableColor(color)
    menuOpen.value = false
}

const menuItems = computed(() => {
    if (!props.editable) {
        return []
    }

    return contextMenuItems.value
})

watch(menuOpen, isOpen => {
    if (isOpen) {
        if (isMultiCellSelection()) {
            floatingMenuState?.preserveTableSelection(props.editor.state.selection)
        }

        floatingMenuState?.openMenu("context-menu")
        return
    }

    floatingMenuState?.closeMenu("context-menu")
})

watch(() => floatingMenuState?.activeMenu.value, activeMenu => {
    if (menuOpen.value && activeMenu && activeMenu !== "context-menu") {
        menuOpen.value = false
    }
})

watch(() => floatingMenuState?.closeSignal.value, () => {
    menuOpen.value = false
})
</script>

<template>
    <UContextMenu v-if="editable && isInTable"
                  :items="menuItems"
                  :ui="contextMenuUi"
                  :content="{collisionPadding: 12}"
                  v-model:open="menuOpen">
        <slot/>
        <template #color-grid>
            <div :class="colorMenuSurfaceClass">
                <div class="mb-2 flex items-center gap-2" :class="sectionLabelClass">
                    <UIcon name="i-lucide-palette" class="h-3.5 w-3.5"/>
                    <span>{{ t('editor.table.backgroundColor') }}</span>
                </div>
                <div class="grid grid-cols-6 gap-1">
                    <UButton
                            type="button"
                            class="flex items-center justify-center"
                            :class="colorButtonClass"
                            :aria-label="t('editor.table.noColor')"
                            variant="ghost"
                            color="neutral"
                            size="sm"
                            square
                            @click="selectColor(null)">
                        <span class="text-xs text-muted">×</span>
                    </UButton>
                    <UButton
                            v-for="color in tableColors"
                            :key="color.name"
                            type="button"
                            :aria-label="color.name"
                            :class="[colorButtonClass, color.backgroundClass, color.hoverClass]"
                            variant="ghost"
                            color="neutral"
                            size="sm"
                            square
                            @click="selectColor(color.name)">
                    </UButton>
                </div>
            </div>
        </template>
    </UContextMenu>
    <slot v-else/>
</template>
