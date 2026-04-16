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
import {computed} from "vue"
import {useI18n} from "vue-i18n"
import type {SlashCommandItem} from "@/components/structuraltext/extensions/SlashCommand"

interface Props {
    items: SlashCommandItem[]
    selectedIndex: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
    (e: "select", item: SlashCommandItem): void
    (e: "highlight", index: number): void
}>()
const {t} = useI18n()

const groups = computed(() => {
    return [
        {
            id: "slash-commands",
            ignoreFilter: true,
            items: props.items.map((item, index) => ({
                ...item,
                index,
            })),
        }
    ]
})
</script>

<template>
    <div class="w-[min(22rem,calc(100vw-1.5rem))] overflow-hidden rounded-xl border border-default bg-default shadow-xl">
        <UCommandPalette
                :groups="groups"
                :autofocus="false"
                :input="false"
                :highlight-on-hover="false"
                class="h-auto max-h-80"
                :ui="{
                    root: 'border-0 bg-transparent shadow-none',
                    content: 'p-2',
                    viewport: 'max-h-80',
                    group: 'space-y-1',
                    empty: 'px-3 py-4 text-sm text-muted'
                }"
        >
            <template #item="{ item }">
                <button
                        type="button"
                        class="flex w-full items-start gap-3 rounded-lg px-3 py-2 text-left transition-colors"
                        :class="item.index === selectedIndex
                            ? 'bg-primary/12 text-highlighted'
                            : 'text-default hover:bg-elevated/70'"
                        @mouseenter="emit('highlight', item.index)"
                        @click.prevent="emit('select', props.items[item.index])"
                >
                    <div class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-default bg-elevated/70">
                        <UIcon :name="item.icon" class="h-4 w-4"/>
                    </div>
                    <div class="min-w-0 flex-1">
                        <div class="text-sm font-medium">
                            {{ item.label }}
                        </div>
                        <div v-if="item.description" class="mt-0.5 text-xs text-muted">
                            {{ item.description }}
                        </div>
                    </div>
                </button>
            </template>

            <template #empty>
                <div class="px-3 py-4 text-sm text-muted">
                    {{ t("editor.slash.empty") }}
                </div>
            </template>
        </UCommandPalette>
    </div>
</template>
