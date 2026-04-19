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
import {
    DEFAULT_TABLE_INSERT_OPTIONS,
    normalizeTableInsertSize,
    type TableInsertOptions,
} from "@/components/structuraltext/composables/useStructuralTextInsertController"
import {editorModalUi} from "@/components/structuraltext/editorUi"

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

const props = withDefaults(defineProps<Props>(), {
    open: false,
    initialRows: DEFAULT_TABLE_INSERT_OPTIONS.rows,
    initialCols: DEFAULT_TABLE_INSERT_OPTIONS.cols,
    initialWithHeaderRow: DEFAULT_TABLE_INSERT_OPTIONS.withHeaderRow,
})

const emit = defineEmits<Emits>()
const {t} = useI18n()
const modalUi = editorModalUi

const rows = ref(String(props.initialRows))
const cols = ref(String(props.initialCols))
const withHeaderRow = ref(props.initialWithHeaderRow)
const hoveredGridRows = ref<number | null>(null)
const hoveredGridCols = ref<number | null>(null)

const gridLimit = 10
const gridRows = Array.from({length: gridLimit}, (_value, index) => index + 1)
const gridCols = Array.from({length: gridLimit}, (_value, index) => index + 1)

const normalizedRows = computed(() => normalizeTableInsertSize(rows.value, props.initialRows))
const normalizedCols = computed(() => normalizeTableInsertSize(cols.value, props.initialCols))
const previewRows = computed(() => hoveredGridRows.value ?? normalizedRows.value)
const previewCols = computed(() => hoveredGridCols.value ?? normalizedCols.value)

const closeModal = () => {
    emit("update:open", false)
}

const resetForm = () => {
    rows.value = String(props.initialRows)
    cols.value = String(props.initialCols)
    withHeaderRow.value = props.initialWithHeaderRow
    hoveredGridRows.value = null
    hoveredGridCols.value = null
}

const previewGridSize = (gridRowsValue: number | null, gridColsValue: number | null) => {
    hoveredGridRows.value = gridRowsValue
    hoveredGridCols.value = gridColsValue
}

const selectGridSize = (gridRowsValue: number, gridColsValue: number) => {
    rows.value = String(gridRowsValue)
    cols.value = String(gridColsValue)
    previewGridSize(null, null)
}

const isGridCellActive = (gridRow: number, gridCol: number) => {
    return gridRow <= previewRows.value && gridCol <= previewCols.value
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
    <UModal :open="open" :ui="modalUi" @update:open="closeModal">
        <template #content>
            <div class="p-6">
                <div class="mb-6 flex items-center justify-between">
                    <div>
                        <h3 class="text-base font-semibold tracking-[-0.01em] text-highlighted">
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
                            :aria-label="t('common.close')"
                            @click="closeModal"
                    />
                </div>

                <div class="space-y-5">
                    <div class="space-y-2">
                        <div class="text-sm font-medium text-highlighted">
                            {{ t("editor.table.insertDialogDescription") }}
                        </div>
                        <div class="rounded-md border border-default bg-default p-3">
                            <div class="mb-3 text-sm font-medium text-highlighted">
                                {{ previewRows }} × {{ previewCols }}
                            </div>
                            <div class="inline-grid gap-1"
                                 :style="{ gridTemplateColumns: `repeat(${gridLimit}, minmax(0, 1fr))` }"
                                 @mouseleave="previewGridSize(null, null)">
                                <button
                                        v-for="cellIndex in gridLimit * gridLimit"
                                        :key="cellIndex"
                                        type="button"
                                        class="h-5 w-5 rounded-[4px] border transition-colors"
                                        :class="isGridCellActive(Math.ceil(cellIndex / gridLimit), ((cellIndex - 1) % gridLimit) + 1)
                                            ? 'border-primary/40 bg-primary/12'
                                            : 'border-default bg-transparent hover:border-primary/30 hover:bg-elevated/30'"
                                        :aria-label="`${Math.ceil(cellIndex / gridLimit)} x ${((cellIndex - 1) % gridLimit) + 1}`"
                                        @focus="previewGridSize(Math.ceil(cellIndex / gridLimit), ((cellIndex - 1) % gridLimit) + 1)"
                                        @mouseenter="previewGridSize(Math.ceil(cellIndex / gridLimit), ((cellIndex - 1) % gridLimit) + 1)"
                                        @click="selectGridSize(Math.ceil(cellIndex / gridLimit), ((cellIndex - 1) % gridLimit) + 1)"
                                />
                            </div>
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
                                        color="neutral"
                                        variant="outline"
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
                                        color="neutral"
                                        variant="outline"
                                        class="w-full"
                                        @blur="syncCols"
                                        @keydown="handleKeydown"
                                />
                            </UFormField>
                        </div>

                        <div class="rounded-md border border-default bg-elevated/30 px-3 py-3">
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
                            variant="outline"
                            @click="closeModal"
                    >
                        {{ t("editor.modal.cancel") }}
                    </UButton>
                    <UButton
                            color="primary"
                            variant="soft"
                            @click="handleConfirm"
                    >
                        {{ t("editor.modal.insert") }}
                    </UButton>
                </div>
            </div>
        </template>
    </UModal>
</template>
