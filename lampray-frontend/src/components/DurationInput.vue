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
import {computed, ref, watch} from "vue"
import {useI18n} from "vue-i18n"

interface Props {
    modelValue?: number  // Total seconds
    placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
    modelValue: 0,
    placeholder: ""
})

const emit = defineEmits<{
    'update:modelValue': [value: number]
}>()

const {t} = useI18n()

const days = ref(0)
const hours = ref(0)
const minutes = ref(0)
const isPermanent = ref(false)

watch(() => props.modelValue, (value) => {
    if (value <= 0) {
        days.value = 0
        hours.value = 0
        minutes.value = 0
    }
    if (value < 0) {
        isPermanent.value = true
    } else {
        isPermanent.value = false
        let remaining = value
        days.value = Math.floor(remaining / 86400)
        remaining %= 86400
        hours.value = Math.floor(remaining / 3600)
        remaining %= 3600
        minutes.value = Math.floor(remaining / 60)
    }
}, {immediate: true})

const totalSeconds = computed(() => {
    if (isPermanent.value) {
        return -1  // Use -1 to represent permanent
    }
    return days.value * 86400 + hours.value * 3600 + minutes.value * 60
})

watch(totalSeconds, (value) => {
    emit('update:modelValue', value)
})

watch(isPermanent, (value) => {
    if (value) {
        days.value = 0
        hours.value = 0
        minutes.value = 0
    }
})
</script>

<template>
    <div class="space-y-3">
        <div class="flex items-center gap-2">
            <UCheckbox v-model="isPermanent" :label="t('common.duration.permanent')"/>
        </div>
        <div v-if="!isPermanent" class="grid grid-cols-3 gap-2">
            <UFormField :label="t('common.duration.days')">
                <UInput
                        v-model.number="days"
                        type="number"
                        :min="0"
                        class="w-full"
                        placeholder="0"
                />
            </UFormField>
            <UFormField :label="t('common.duration.hours')">
                <UInput
                        v-model.number="hours"
                        type="number"
                        :min="0"
                        :max="23"
                        class="w-full"
                        placeholder="0"
                />
            </UFormField>
            <UFormField :label="t('common.duration.minutes')">
                <UInput
                        v-model.number="minutes"
                        type="number"
                        :min="0"
                        :max="59"
                        class="w-full"
                        placeholder="0"
                />
            </UFormField>
        </div>
        <div v-if="!isPermanent" class="text-xs text-neutral-500">
            {{ t('common.duration.total', {days, hours, minutes}) }}
        </div>
    </div>
</template>

