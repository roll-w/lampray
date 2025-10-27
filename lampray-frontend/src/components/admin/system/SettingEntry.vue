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
import type {PropType} from "vue";
import {ref, watch} from "vue";
import {SettingSource, type SettingVo} from "@/services/system/system.type.ts";
import {useI18n} from "vue-i18n";

const props = defineProps({
    setting: {
        type: Object as PropType<SettingVo>,
        required: true
    },
    onChange: {
        type: Function as PropType<(setting: SettingVo) => void>,
        required: false
    },
    onDelete: {
        type: Function as PropType<(setting: SettingVo) => void>,
        required: false
    },
    onReset: {
        type: Function as PropType<(setting: SettingVo) => void>,
        required: false
    }
})

const {t} = useI18n()
const inputValue = ref<any>(props.setting.value)
const editable = props.setting.supportedSources.includes(SettingSource.DATABASE)

watch(inputValue, (newVal) => {
    if (newVal === props.setting.value) {
        return
    }
    if (props.onChange) {
        props.onChange({
            ...props.setting,
            value: newVal
        })
    }
})

const resetSetting = () => {
    inputValue.value = props.setting.value
    if (props.onReset) {
        props.onReset({
            ...props.setting,
            value: inputValue.value
        })
    }
}

const showDeleteConfirmModalState = ref(false)

const deleteSetting = () => {
    if (props.onDelete) {
        props.onDelete(props.setting)
    }
    showDeleteConfirmModalState.value = false
}

</script>

<template>
    <div class="py-3">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 w-full">
            <div class="text-lg font-medium">{{ setting.key }}</div>
            <UBadge variant="subtle" color="success" size="md" class="font-medium">
                {{ setting.source }}
            </UBadge>
        </div>

        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1 whitespace-pre-line">
            {{ setting.description }}
        </p>
        <div class="w-full mt-2 flex flex-row items-center gap-2">
            <template v-if="setting.type === 'BOOLEAN'">
                <USwitch
                        v-model="inputValue"
                        :disabled="!editable"
                        class="max-w-full flex-1"
                />
            </template>

            <template v-else-if="setting.type === 'STRING'">
                <UInput
                        v-model="inputValue"
                        size="md"
                        :disabled="!editable"
                        class="max-w-full flex-1"
                />
            </template>

            <template v-else-if="setting.type === 'INT' || setting.type === 'LONG'">
                <UInput
                        v-model="inputValue"
                        type="number"
                        size="md"
                        :disabled="!editable"
                        inputmode="numeric"
                        class="max-w-full flex-1"
                />
            </template>

            <template v-else-if="setting.type === 'FLOAT' || setting.type === 'DOUBLE'">
                <UInput
                        v-model="inputValue"
                        type="number"
                        size="md"
                        :disabled="!editable"
                        inputmode="decimal"
                        step="0.01"
                        class="max-w-full flex-1"
                />
            </template>

            <template v-else-if="setting.type === 'STRING_SET'">
                <USelect
                        v-model="inputValue"
                        multiple
                        :disabled="!editable"
                        :creatable="true"
                        placeholder="输入或选择选项"
                        class="max-w-full flex-1"
                />
            </template>

            <template v-else>
                <UInput
                        v-model="inputValue"
                        size="md"
                        :disabled="!editable"
                        class="max-w-full flex-1"
                />
            </template>
            <div v-if="editable" class="flex flex-row gap-2">
                <UButton
                        color="primary"
                        variant="outline"
                        size="md"
                        icon="i-lucide-rotate-ccw"
                        @click="resetSetting"
                >
                </UButton>
                <UButton
                        color="error"
                        variant="outline"
                        size="md"
                        icon="i-lucide-trash-2"
                        @click="showDeleteConfirmModalState = true"
                />
                <UModal v-model:open="showDeleteConfirmModalState">
                    <template #content>
                        <div class="p-4">
                            <div class="text-lg font-medium mb-4">{{ t("views.adminfaced.system.settings.deleteTitle")}} </div>
                            <p class="mb-4">{{ t("views.adminfaced.system.settings.deleteDescription", {key: setting.key}) }}</p>
                            <div class="flex justify-end space-x-2">
                                <UButton
                                        color="neutral"
                                        variant="outline"
                                        @click="showDeleteConfirmModalState = false"
                                >
                                    {{ t("common.cancel") }}
                                </UButton>
                                <UButton
                                        color="error"
                                        variant="solid"
                                        @click="deleteSetting"
                                >
                                    {{ t("common.delete") }}
                                </UButton>
                            </div>
                        </div>
                    </template>
                </UModal>
            </div>


        </div>


    </div>
</template>