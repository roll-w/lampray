<!--
  - Copyright (C) 2023-2025 RollW
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
import {computed, ref} from "vue";
import {useI18n} from "vue-i18n";
import type {AttributeColor} from "@/components/structuraltext/types";
import {useTableActions} from "@/components/structuraltext/composables/useTableActions";

interface Props {
    editor: Editor
    editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    editable: true
})

const {t} = useI18n()
const tableActions = useTableActions(props.editor, t)

const menuOpen = ref(false)

function selectColor(color: AttributeColor | null) {
    tableActions.selectColor(color)
    menuOpen.value = false
}

const menuItems = computed(() => {
    if (!props.editable) {
        return []
    }

    return tableActions.contextMenuItems.value
})
</script>

<template>
    <UContextMenu v-if="editable && tableActions.isInTable" :items="menuItems" v-model:open="menuOpen">
        <slot/>
        <template #color-grid>
            <div class="p-2 space-y-2">
                <div class="grid grid-cols-6 gap-2">
                    <UButton
                            type="button"
                            class="w-6 h-6 rounded-sm border border-default flex items-center justify-center"
                            :aria-label="t('editor.table.noColor')"
                            variant="ghost"
                            color="neutral"
                            size="sm"
                            square
                            @click="selectColor(null)">
                        <span class="text-xs text-muted">×</span>
                    </UButton>
                    <UButton
                            v-for="color in tableActions.colors"
                            :key="color.name"
                            type="button"
                            :aria-label="color.name"
                            class="w-6 h-6 rounded-sm border border-default"
                            :class="[color.backgroundClass, color.hoverClass]"
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
