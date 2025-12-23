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
import {ref, watch} from "vue"
import {useI18n} from "vue-i18n"

interface Props {
    open: boolean
    initialUrl?: string
    initialAlt?: string
}

interface Emits {
    (e: "update:open", value: boolean): void
    (e: "confirm", data: { url: string; alt?: string }): void
}

const props = withDefaults(defineProps<Props>(), {
    open: false,
    initialUrl: "",
    initialAlt: ""
})

const emit = defineEmits<Emits>()
const {t} = useI18n()

const imageUrl = ref("")
const imageAlt = ref("")
const urlError = ref("")

watch(() => props.open, (newVal) => {
    if (newVal) {
        imageUrl.value = props.initialUrl || ""
        imageAlt.value = props.initialAlt || ""
        urlError.value = ""
    }
})

const closeModal = () => {
    emit("update:open", false)
}

const validateUrl = (): boolean => {
    if (!imageUrl.value.trim()) {
        urlError.value = t("editor.modal.imageUrlRequired")
        return false
    }
    urlError.value = ""
    return true
}

const handleConfirm = () => {
    if (!validateUrl()) {
        return
    }
    emit("confirm", {
        url: imageUrl.value.trim(),
        alt: imageAlt.value.trim() || undefined
    })
    closeModal()
}

const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === "Enter") {
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
                        {{ t("editor.modal.insertImage") }}
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
                            :label="t('editor.modal.imageUrl')"
                            :error="urlError || false"
                    >
                        <UInput
                                v-model="imageUrl"
                                :placeholder="t('editor.modal.imageUrlPlaceholder')"
                                autofocus
                                @keydown="handleKeydown"
                                class="w-full"
                        />
                    </UFormField>
                    <UFormField :label="t('editor.modal.altText')">
                        <UInput
                                v-model="imageAlt"
                                :placeholder="t('editor.modal.altTextPlaceholder')"
                                @keydown="handleKeydown"
                                class="w-full"
                        />
                    </UFormField>
                </div>

                <div class="flex justify-end gap-2">
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

