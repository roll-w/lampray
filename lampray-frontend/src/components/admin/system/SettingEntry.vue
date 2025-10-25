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
import type {PropType} from 'vue'
import {ref, watch} from 'vue'
import {SettingSource, type SettingVo} from "@/services/system/system.type.ts";

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

const inputValue = ref<any>(props.setting.value)
const editable = props.setting.source === SettingSource.DATABASE

watch(inputValue, (newVal) => {
    if (props.onChange) {
        props.onChange({
            ...props.setting,
            value: newVal
        })
    }
})
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

        <div class="w-full mt-2">
            <template v-if="setting.type === 'BOOLEAN'">
                <USwitch
                        v-model="inputValue"
                        :disabled="!editable"
                        class="w-full"
                />
            </template>

            <template v-else-if="setting.type === 'STRING'">
                <UInput
                        v-model="inputValue"
                        size="md"
                        :disabled="!editable"
                        class="w-full"
                />
            </template>

            <template v-else-if="setting.type === 'INT' || setting.type === 'LONG'">
                <UInput
                        v-model="inputValue"
                        type="number"
                        size="md"
                        :disabled="!editable"
                        inputmode="numeric"
                        class="w-full"
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
                        class="w-full"
                />
            </template>

            <template v-else-if="setting.type === 'STRING_SET'">
                <USelect
                        v-model="inputValue"
                        multiple
                        :disabled="!editable"
                        :creatable="true"
                        :options="[]"
                        placeholder="输入或选择选项"
                        class="w-full"
                />
            </template>

            <template v-else>
                <UInput
                        v-model="inputValue"
                        size="md"
                        :disabled="!editable"
                        class="w-full"
                />
            </template>
        </div>
    </div>
</template>