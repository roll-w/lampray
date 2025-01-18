<!--
  - Copyright (C) 2023 RollW
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

<script setup>
import {ref, watch} from "vue";
import {NCheckbox, NDatePicker, NFormItem, NIcon, NImage, NInput, NSelect, NSwitch, NTooltip} from "naive-ui";

const props = defineProps({
    value: {
        type: [String, Number, Boolean],
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
        // type: text, switch, checkbox, select, date, image
        type: String,
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
        type: Function,
        default: null
    },
    extra: {
        type: Object,
        default: {}
    },
    // allow putting all configs in one object
    config: {
        type: Object,
        default: {}
    },
})


watch(() => props.value, (newValue) => {
    inputValue.value = newValue
})

const emit = defineEmits(['update:value'])

const config = (props.config || {}).name ? props.config : {
    key: props.key,
    name: props.name,
    modifiable: props.modifiable,
    type: props.type,
    tip: props.tip,
    info: props.info,
    placeholder: props.placeholder,
    render: props.render,
    extra: props.extra,
}

const getFormattedValue = () => {
    if (props.config.render) {
        return props.config.render(props.value)
    }
    if (props.render) {
        return props.render(props.value)
    }

    return props.value
}

const inputValue = ref(getFormattedValue())

</script>

<template>
    <n-form-item :path="config.key"
                 :show-label="false">
        <div class="flex-col w-full ">
            <div class="text-base">
                <div class="pb-2 w-auto flex">
                    <div>
                        {{ config.name }}
                    </div>
                    <n-tooltip v-if="config.tip" placement="top" trigger="hover">
                        <template #trigger>
                            <n-icon size="15">
                                <template #default>
                                    <svg fill="currentColor" viewBox="0 0 24 24"
                                         xmlns="http://www.w3.org/2000/svg">
                                        <path d="M12 2C6.486 2 2 6.486 2 12s4.486 10 10 10 10-4.486 10-10S17.514 2 12 2zm1 16h-2v-6h2v6zm0-8h-2V7h2v3z"/>
                                    </svg>
                                </template>
                            </n-icon>
                        </template>
                        {{ config.tip }}
                    </n-tooltip>
                </div>
            </div>
            <div v-if="config.info" class="text-neutral-500 text-sm pb-2">
                {{ config.info }}
            </div>
            <div class="pt-1">
                <n-switch v-if="config.type === 'switch'"
                          v-model:value="inputValue"
                          :disabled="!config.modifiable"
                          size="large"
                          @update:value="emit('update:value', $event)"/>
                <n-checkbox v-else-if="config.type === 'checkbox'"
                            v-model:checked="inputValue"
                            :disabled="!config.modifiable"
                            size="large"
                            @update:checked="emit('update:value', $event)"/>
                <n-select v-else-if="config.type === 'select'"
                          v-model:value="inputValue"
                          :disabled="!config.modifiable"
                          :multiple="(config.extra || {}).multiple || false"
                          :options="config.options"
                          size="large"
                          @update:value="emit('update:value', $event)"/>
                <n-date-picker v-else-if="config.type === 'date'"
                               v-model:value="inputValue"
                               :input-readonly="!config.modifiable"
                               :type="config.type || 'date'"
                               class="w-full"
                               size="large"
                               @update:value="emit('update:value', $event)">

                </n-date-picker>
                <div v-else-if="config.type === 'image'">
                    <n-image
                            :img-props="{
                                style: {
                                    height: '30vh',
                                }
                            }"
                            :show-toolbar="false"
                            :src="inputValue"
                            class="rounded-xl"
                            lazy
                            @update:value="emit('update:value', $event)"/>
                </div>
                <n-input v-else-if="config.modifiable"
                         v-model:value="inputValue"
                         :placeholder="config.placeholder"
                         size="large"
                         @update:value="emit('update:value', $event)"/>
                <div v-else class="text-xl">
                    {{ getFormattedValue() }}
                </div>
            </div>
        </div>
    </n-form-item>
</template>
