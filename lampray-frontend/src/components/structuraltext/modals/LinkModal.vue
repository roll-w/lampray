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
import {ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'

interface Props {
    open: boolean
    initialUrl?: string
    initialText?: string
    isEditing?: boolean
}

interface Emits {
    (e: 'update:open', value: boolean): void
    (e: 'confirm', data: { url: string; text?: string }): void
    (e: 'remove'): void
}

const props = withDefaults(defineProps<Props>(), {
    open: false,
    initialUrl: '',
    initialText: '',
    isEditing: false
})

const emit = defineEmits<Emits>()
const {t} = useI18n()

const linkUrl = ref('')
const linkText = ref('')
const urlError = ref('')

watch(() => props.open, (newVal) => {
    if (newVal) {
        linkUrl.value = props.initialUrl || ''
        linkText.value = props.initialText || ''
        urlError.value = ''
    }
})

const closeModal = () => {
    emit('update:open', false)
}

const validateUrl = (): boolean => {
    if (!linkUrl.value.trim()) {
        urlError.value = t('editor.modal.urlRequired')
        return false
    }
    urlError.value = ''
    return true
}

const handleConfirm = () => {
    if (!validateUrl()) {
        return
    }
    emit('confirm', {
        url: linkUrl.value.trim(),
        text: linkText.value.trim() || undefined
    })
    closeModal()
}

const handleRemove = () => {
    emit('remove')
    closeModal()
}

const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Enter') {
        event.preventDefault()
        handleConfirm()
    }
}
</script>

<template>
    <UModal :open="open" @update:open="closeModal">
        <template #content>
            <div class="p-6">
                <div class="flex items-center justify-between mb-6">
                    <h3 class="text-lg font-semibold">
                        {{ isEditing ? t('editor.modal.editLink') : t('editor.modal.insertLink') }}
                    </h3>
                    <UButton
                            color="neutral"
                            variant="ghost"
                            icon="i-lucide-x"
                            @click="closeModal"
                    />
                </div>

                <div class="space-y-4 mb-6">
                    <UFormField
                            :label="t('editor.modal.url')"
                            :error="urlError || false"
                    >
                        <UInput
                                v-model="linkUrl"
                                :placeholder="t('editor.modal.urlPlaceholder')"
                                autofocus
                                @keydown="handleKeydown"
                        />
                    </UFormField>
                    <UFormField
                            v-if="linkText && !isEditing"
                            :label="t('editor.modal.text')"
                    >
                        <UInput
                                v-model="linkText"
                                disabled
                        />
                    </UFormField>
                </div>

                <div class="flex justify-between">
                    <UButton
                            v-if="isEditing"
                            color="error"
                            variant="ghost"
                            @click="handleRemove"
                    >
                        {{ t('editor.modal.removeLink') }}
                    </UButton>
                    <div v-else/>
                    <div class="flex gap-2">
                        <UButton
                                color="neutral"
                                variant="ghost"
                                @click="closeModal"
                        >
                            {{ t('editor.modal.cancel') }}
                        </UButton>
                        <UButton
                                color="primary"
                                @click="handleConfirm"
                        >
                            {{ isEditing ? t('editor.modal.update') : t('editor.modal.insert') }}
                        </UButton>
                    </div>
                </div>
            </div>
        </template>
    </UModal>
</template>

