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
import {computed, ref, watch} from "vue"
import {useI18n} from "vue-i18n"
import type {TableInsertOptions} from "@/components/structuraltext/composables/useTableActions"

interface Props {
    open: boolean
    initialRows?: number
    initialCols?: number
    initialWithHeaderRow?: boolean
}

interface Emits {
    (e: "update:open", value: boolean): void
    (e: "confirm", value: TableInsertOptions): void
}

interface TablePreset {
    label: string
    rows: number
    cols: number
}

const props = withDefaults(defineProps<Props>(), {
    open: false,
    initialRows: 3,
    initialCols: 3,
    initialWithHeaderRow: true,
})

const emit = defineEmits<Emits>()
const {t} = useI18n()

const rows = ref(String(props.initialRows))
const cols = ref(String(props.initialCols))
const withHeaderRow = ref(props.initialWithHeaderRow)

const presets: TablePreset[] = [
    {label: "2 × 2", rows: 2, cols: 2},
    {label: "3 × 3", rows: 3, cols: 3},
    {label: "4 × 4", rows: 4, cols: 4},
    {label: "3 × 5", rows: 3, cols: 5},
]

function clampSize(value: string, fallback: number) {
    const parsedValue = Number.parseInt(value, 10)
    if (Number.isNaN(parsedValue)) {
        return fallback
    }
    return Math.min(10, Math.max(1, parsedValue))
}

const normalizedRows = computed(() => clampSize(rows.value, props.initialRows))
const normalizedCols = computed(() => clampSize(cols.value, props.initialCols))

const closeModal = () => {
    emit("update:open", false)
}

const resetForm = () => {
    rows.value = String(props.initialRows)
    cols.value = String(props.initialCols)
    withHeaderRow.value = props.initialWithHeaderRow
}

const applyPreset = (preset: TablePreset) => {
    rows.value = String(preset.rows)
    cols.value = String(preset.cols)
}

const isPresetActive = (preset: TablePreset) => {
    return normalizedRows.value === preset.rows && normalizedCols.value === preset.cols
}

const syncRows = () => {
    rows.value = String(normalizedRows.value)
}

const syncCols = () => {
    cols.value = String(normalizedCols.value)
}

const handleConfirm = () => {
    syncRows()
    syncCols()

    emit("confirm", {
        rows: normalizedRows.value,
        cols: normalizedCols.value,
        withHeaderRow: withHeaderRow.value,
    })
}

const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === "Enter") {
        event.preventDefault()
        handleConfirm()
    }
}

watch(() => props.open, (newValue) => {
    if (newValue) {
        resetForm()
    }
})
</script>

<template>
    <UModal :open="open" @update:open="closeModal">
        <template #content>
            <div class="p-6">
                <div class="mb-6 flex items-center justify-between">
                    <div>
                        <h3 class="text-lg font-semibold">
                            {{ t("editor.table.insertDialogTitle") }}
                        </h3>
                        <p class="mt-1 text-sm text-muted">
                            {{ t("editor.table.insertDialogDescription") }}
                        </p>
                    </div>
                    <UButton
                            color="neutral"
                            variant="ghost"
                            icon="i-lucide-x"
                            @click="closeModal"
                    />
                </div>

                <div class="space-y-5">
                    <div class="space-y-2">
                        <div class="text-sm font-medium text-highlighted">
                            {{ t("editor.table.quickPresets") }}
                        </div>
                        <div class="grid grid-cols-2 gap-2">
                            <UButton
                                    v-for="preset in presets"
                                    :key="preset.label"
                                    color="primary"
                                    :variant="isPresetActive(preset) ? 'soft' : 'outline'"
                                    class="justify-center"
                                    @click="applyPreset(preset)"
                            >
                                {{ preset.label }}
                            </UButton>
                        </div>
                    </div>

                    <div class="space-y-3">
                        <div class="text-sm font-medium text-highlighted">
                            {{ t("editor.table.customSize") }}
                        </div>
                        <div class="grid grid-cols-2 gap-3">
                            <UFormField :label="t('editor.table.rows')">
                                <UInput
                                        v-model="rows"
                                        type="number"
                                        min="1"
                                        max="10"
                                        class="w-full"
                                        @blur="syncRows"
                                        @keydown="handleKeydown"
                                />
                            </UFormField>
                            <UFormField :label="t('editor.table.columns')">
                                <UInput
                                        v-model="cols"
                                        type="number"
                                        min="1"
                                        max="10"
                                        class="w-full"
                                        @blur="syncCols"
                                        @keydown="handleKeydown"
                                />
                            </UFormField>
                        </div>

                        <div class="rounded-lg border border-default bg-default/40 px-3 py-3">
                            <div class="flex items-center justify-between gap-3">
                                <div>
                                    <div class="text-sm font-medium text-highlighted">
                                        {{ t("editor.table.withHeaderRow") }}
                                    </div>
                                    <div class="text-xs text-muted">
                                        {{ normalizedRows }} × {{ normalizedCols }}
                                    </div>
                                </div>
                                <UCheckbox v-model="withHeaderRow" />
                            </div>
                        </div>
                    </div>
                </div>

                <div class="mt-6 flex justify-end gap-2">
                    <UButton
                            color="neutral"
                            variant="ghost"
                            @click="closeModal"
                    >
                        {{ t("editor.modal.cancel") }}
                    </UButton>
                    <UButton
                            color="primary"
                            @click="handleConfirm"
                    >
                        {{ t("editor.modal.insert") }}
                    </UButton>
                </div>
            </div>
        </template>
    </UModal>
</template>
