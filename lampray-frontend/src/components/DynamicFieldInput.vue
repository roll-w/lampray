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
<script lang="ts">
export type FieldType = 'text' | 'switch' | 'checkbox' | 'select' | 'date' | 'image'

export interface FieldConfig {
    key: string
    name?: string
    modifiable?: boolean
    type?: FieldType
    tip?: string
    info?: string
    placeholder?: string
    render?: (value: any) => string
    extra?: Record<string, any>
    options?: Array<{ label: string; value: any }>
}

export const findFieldConfig = (configs: FieldConfig[], key: string): FieldConfig | null => {
    for (const config of configs) {
        if (config.key === key) {
            return config
        }
    }
    return null
}

</script>

<script setup lang="ts">
import type {PropType} from 'vue'
import {computed, ref, watch} from 'vue'

const props = defineProps({
    value: {
        type: [String, Number, Boolean, null] as PropType<string | number | boolean | any | null>,
        required: true
    },
    name: {
        type: String,
        default: ''
    },
    modifiable: {
        type: Boolean,
        default: false
    },
    placeholder: {
        type: String,
        default: ''
    },
    type: {
        type: String as PropType<FieldType>,
        default: 'text'
    },
    tip: {
        type: String,
        default: ''
    },
    info: {
        type: String,
        default: ''
    },
    key: {
        type: String,
        default: ''
    },
    render: {
        type: Function as PropType<(value: any) => string>,
        default: null
    },
    extra: {
        type: Object as PropType<Record<string, any>>,
        default: () => ({})
    },
    loading: {
        type: Boolean,
        default: false
    },
    config: {
        type: Object as PropType<FieldConfig>,
        default: () => ({})
    }
})

const emit = defineEmits<{
    (e: 'update:value', value: string | number | boolean): void
}>()

const mergedConfig = computed<FieldConfig>(() => {
    if (Object.keys(props.config).length > 0 && props.config.name) {
        return props.config
    }
    return {
        key: props.key,
        name: props.name,
        modifiable: props.modifiable,
        type: props.type,
        tip: props.tip,
        info: props.info,
        placeholder: props.placeholder,
        render: props.render,
        extra: props.extra
    }
})

const getFormattedValue = (): string => {
    if (mergedConfig.value.render) {
        return mergedConfig.value.render(props.value)
    }
    if (props.render) {
        return props.render(props.value)
    }
    if (props.value === null || props.value === undefined) {
        return ''
    }

    return String(props.value)
}

const inputValue = ref(getFormattedValue())

watch(() => props.value,
        (newValue) => {
            inputValue.value = getFormattedValue()
        }
);

const handleUpdate = (newValue: any) => {
    emit('update:value', newValue)
}
</script>

<template>
    <UFormField>
        <template #label>
            <UTooltip v-if="mergedConfig.tip" :text="mergedConfig.tip">
                <div class="flex items-center gap-2 cursor-help">
                    <span class="text-base font-medium">{{ mergedConfig.name }}</span>
                </div>
            </UTooltip>
            <div v-else class="flex items-center gap-2">
                <span class="text-base font-medium">{{ mergedConfig.name }}</span>
            </div>
            <p v-if="mergedConfig.info" class="text-sm text-gray-500 mt-1">
                {{ mergedConfig.info }}
            </p>
        </template>

        <div class="pt-1" v-if="!loading">
            <!-- Switch -->
            <USwitch
                    class="w-full"
                    :name="mergedConfig.key"
                    v-if="mergedConfig.type === 'switch'"
                    :model-value="inputValue === 'true'"
                    :disabled="!mergedConfig.modifiable"
                    @update:model-value="handleUpdate($event)"
            />

            <!-- Checkbox -->
            <UCheckbox
                    class="w-full"
                    :name="mergedConfig.key"
                    v-else-if="mergedConfig.type === 'checkbox'"
                    :model-value="inputValue === 'true'"
                    :disabled="!mergedConfig.modifiable"
                    @update:model-value="handleUpdate($event)"
            />

            <!-- Select -->
            <USelect
                    class="w-full"
                    v-else-if="mergedConfig.type === 'select'"
                    :model-value="inputValue"
                    :name="mergedConfig.key"
                    :items="mergedConfig.options || []"
                    :multiple="(mergedConfig.extra?.multiple) || false"
                    :disabled="!mergedConfig.modifiable"
                    @update:model-value="handleUpdate($event)"
            />

            <!-- Date Picker -->
            <UInput
                    class="w-full"
                    :name="mergedConfig.key"
                    v-else-if="mergedConfig.type === 'date'"
                    :model-value="inputValue"
                    :disabled="!mergedConfig.modifiable"
                    type="date"
                    @update:model-value="handleUpdate($event)"
            />

            <div v-else-if="mergedConfig.type === 'image'" class="rounded-xl overflow-hidden">
                <!--TODO: image viewer and uploader-->
            </div>

            <UInput
                    class="w-full"
                    :name="mergedConfig.key"
                    v-else-if="mergedConfig.modifiable"
                    :model-value="inputValue"
                    :placeholder="mergedConfig.placeholder"
                    @update:model-value="handleUpdate($event)"
            />

            <div v-else class="text-lg ">
                {{ getFormattedValue() }}
            </div>
        </div>
        <USkeleton v-else class="w-full h-10"/>
    </UFormField>
</template>