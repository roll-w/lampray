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
import type {ContextMenuItem} from "@nuxt/ui";
import {BASIC_COLORS} from "@/components/structuraltext/utils/color.ts";

interface Props {
    editor: Editor
    editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    editable: true
})

const {t} = useI18n()

const isInTable = computed(() => props.editor.isActive("table"))
const isInCell = computed(() => props.editor.isActive("tableCell") || props.editor.isActive("tableHeader"))
const canMergeCells = computed(() => props.editor.can().mergeCells())
const canSplitCell = computed(() => props.editor.can().splitCell())

const menuOpen = ref(false)

const colors = BASIC_COLORS

function selectColor(color: AttributeColor | null) {
    props.editor.chain().focus().updateAttributes("tableCell", {backgroundColor: color}).run()
    props.editor.chain().focus().updateAttributes("tableHeader", {backgroundColor: color}).run()
    menuOpen.value = false
}

const menuItems = computed(() => {
    if (!props.editable || !isInTable.value) {
        return []
    }

    const items: ContextMenuItem[][] = [
        [
            {
                label: t("editor.table.insertColumnBefore"),
                icon: "i-lucide-columns-2",
                onSelect: () => props.editor.chain().focus().addColumnBefore().run()
            },
            {
                label: t("editor.table.insertColumnAfter"),
                icon: "i-lucide-columns-2",
                onSelect: () => props.editor.chain().focus().addColumnAfter().run()
            },
            {
                label: t("editor.table.deleteColumn"),
                icon: "i-lucide-trash-2",
                color: "error" as const,
                onSelect: () => props.editor.chain().focus().deleteColumn().run()
            }
        ],
        [
            {
                label: t("editor.table.insertRowBefore"),
                icon: "i-lucide-rows-2",
                onSelect: () => props.editor.chain().focus().addRowBefore().run()
            },
            {
                label: t("editor.table.insertRowAfter"),
                icon: "i-lucide-rows-2",
                onSelect: () => props.editor.chain().focus().addRowAfter().run()
            },
            {
                label: t("editor.table.deleteRow"),
                icon: "i-lucide-trash-2",
                color: "error" as const,
                onSelect: () => props.editor.chain().focus().deleteRow().run()
            }
        ]
    ]

    if (isInCell.value) {
        items.push([
            {
                label: t("editor.table.mergeCells"),
                icon: "i-lucide-merge",
                disabled: !canMergeCells.value,
                onSelect: () => props.editor.chain().focus().mergeCells().run()
            },
            {
                label: t("editor.table.splitCell"),
                icon: "i-lucide-ungroup",
                disabled: !canSplitCell.value,
                onSelect: () => props.editor.chain().focus().splitCell().run()
            }
        ])

        items.push([
            {
                label: t("editor.table.backgroundColor"),
                icon: "i-lucide-palette",
                children: [
                    {
                        type: "label",
                        slot: "color-grid" as const,
                        onSelect: (e: Event) => {
                            e.stopPropagation()
                        }
                    }
                ]
            }
        ])

        items.push([
            {
                label: t("editor.table.toggleHeaderRow"),
                icon: "i-lucide-rows",
                onSelect: () => props.editor.chain().focus().toggleHeaderRow().run()
            },
            {
                label: t("editor.table.toggleHeaderColumn"),
                icon: "i-lucide-columns",
                onSelect: () => props.editor.chain().focus().toggleHeaderColumn().run()
            },
            {
                label: t("editor.table.toggleHeaderCell"),
                icon: "i-lucide-square-stack",
                onSelect: () => props.editor.chain().focus().toggleHeaderCell().run()
            }
        ])
    }

    items.push([
        {
            label: t("editor.table.deleteTable"),
            icon: "i-lucide-trash-2",
            color: "error" as const,
            onSelect: () => props.editor.chain().focus().deleteTable().run()
        }
    ])

    return items
})
</script>

<template>
    <UContextMenu v-if="editable && isInTable" :items="menuItems" v-model:open="menuOpen">
        <slot/>
        <template #color-grid>
            <div class="p-2 space-y-2">
                <div class="grid grid-cols-6 gap-2">
                    <UButton
                            type="button"
                            class="w-6 h-6 rounded-sm border border-default flex items-center justify-center"
                            aria-label="No color"
                            variant="ghost"
                            color="neutral"
                            size="sm"
                            square
                            @click="selectColor(null)">
                        <span class="text-xs text-muted">×</span>
                    </UButton>
                    <UButton
                            v-for="color in colors"
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
