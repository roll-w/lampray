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
import {computed, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {parseHttpUrl} from '@/components/structuraltext/composables/useEditorActions'
import {editorModalUi} from '@/components/structuraltext/editorUi'

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
const modalUi = editorModalUi

const linkUrl = ref('')
const linkText = ref('')
const urlError = ref('')

const showTextField = computed(() => !props.isEditing)
const isTextDisabled = computed(() => !!props.initialText?.trim())

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
    const trimmedUrl = linkUrl.value.trim()
    if (!trimmedUrl) {
        urlError.value = t('editor.modal.urlRequired')
        return false
    }

    if (!parseHttpUrl(trimmedUrl)) {
        urlError.value = t('editor.modal.urlInvalid')
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
}

const handleRemove = () => {
    emit('remove')
}

const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Enter') {
        event.preventDefault()
        handleConfirm()
    }
}
</script>

<template>
    <UModal :open="open" :ui="modalUi" @update:open="closeModal">
        <template #content>
            <div class="p-6">
                <div class="flex items-center justify-between mb-6">
                    <h3 class="text-base font-semibold tracking-[-0.01em] text-highlighted">
                        {{ isEditing ? t('editor.modal.editLink') : t('editor.modal.insertLink') }}
                    </h3>
                    <UButton
                            color="neutral"
                            variant="ghost"
                            icon="i-lucide-x"
                            :aria-label="t('common.close')"
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
                                color="neutral"
                                variant="outline"
                                autofocus
                                @keydown="handleKeydown"
                                class="w-full"
                        />
                    </UFormField>
                    <UFormField
                            v-if="showTextField"
                            :label="t('editor.modal.text')"
                    >
                        <UInput
                                v-model="linkText"
                                :disabled="isTextDisabled"
                                :placeholder="t('editor.modal.textPlaceholder')"
                                color="neutral"
                                variant="outline"
                                class="w-full"
                        />
                    </UFormField>
                </div>

                <div class="flex justify-between">
                    <UButton
                            v-if="isEditing"
                            color="error"
                            variant="soft"
                            @click="handleRemove"
                    >
                        {{ t('editor.modal.removeLink') }}
                    </UButton>
                    <div v-else/>
                    <div class="flex gap-2">
                        <UButton
                                color="neutral"
                                variant="outline"
                                @click="closeModal"
                        >
                            {{ t('editor.modal.cancel') }}
                        </UButton>
                        <UButton
                                color="primary"
                                variant="soft"
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
