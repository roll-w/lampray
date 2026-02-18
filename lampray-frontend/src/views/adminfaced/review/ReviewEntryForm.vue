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

<script lang="ts" setup>
import {useI18n} from "vue-i18n";
import {ReviewCategory, ReviewSeverity} from "@/services/content/review.type";
import {ref} from "vue";

export interface EntryFormData {
    message: string;
    suggestion: string;
    severity: ReviewSeverity;
    category: ReviewCategory;
}

const props = defineProps<{
    modelValue: EntryFormData;
    title: string;
    submitLabel: string;
    contextText?: string;
}>();

const emit = defineEmits<{
    (e: "update:modelValue", value: EntryFormData): void;
    (e: "submit"): void;
    (e: "cancel"): void;
}>();

const {t} = useI18n();

const severityOptions = [
    {label: t("views.adminfaced.review.severity.CRITICAL"), value: ReviewSeverity.CRITICAL},
    {label: t("views.adminfaced.review.severity.MAJOR"), value: ReviewSeverity.MAJOR},
    {label: t("views.adminfaced.review.severity.MINOR"), value: ReviewSeverity.MINOR},
    {label: t("views.adminfaced.review.severity.INFO"), value: ReviewSeverity.INFO}
];

const categoryOptions = Object.values(ReviewCategory).map(c => ({
    label: t(`views.adminfaced.review.category.${c}`),
    value: c
}));

const getSeverityColor = (severity: ReviewSeverity) => {
    switch (severity) {
        case ReviewSeverity.CRITICAL:
            return "error";
        case ReviewSeverity.MAJOR:
            return "warning";
        case ReviewSeverity.MINOR:
            return "secondary";
        case ReviewSeverity.INFO:
            return "info";
        default:
            return "neutral";
    }
};

const formData = ref<EntryFormData>({...props.modelValue});

const handleSubmit = () => {
    if (!formData.value.message.trim()) return;
    // Only emit to parent on submit, not on every change
    emit("update:modelValue", {...formData.value});
    emit("submit");
};

const handleCancel = () => {
    emit("cancel");
};
</script>

<template>
    <div class="p-4 rounded-lg border border-neutral-200 dark:border-neutral-800 space-y-4">
        <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-neutral-900 dark:text-white uppercase tracking-wider">
                {{ title }}
            </span>
            <UButton
                color="neutral"
                icon="i-lucide-x"
                size="xs"
                variant="ghost"
                @click="handleCancel"
            />
        </div>

        <div v-if="contextText"
             class="rounded-md p-3 border border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/50">
            <div class="text-[10px] uppercase font-bold text-neutral-400 mb-1 tracking-wider flex justify-between">
                <span>{{ t('views.adminfaced.review.reviewEntryContextLabel') }}</span>
            </div>
            <div :title="contextText"
                 class="w-full text-xs text-neutral-600 dark:text-neutral-400 italic font-mono truncate">
                "{{ contextText }}"
            </div>
        </div>

        <UFormField :label="t('views.adminfaced.review.reviewEntryCategory')" size="sm">
            <USelectMenu
                v-model="formData.category"
                :items="categoryOptions"
                value-key="value"
                class="w-full"
                size="sm"
            />
        </UFormField>

        <UFormField :label="t('views.adminfaced.review.reviewEntrySeverity')" size="sm">
            <UTabs
                v-model="formData.severity"
                :items="severityOptions"
                size="xs"
                variant="pill"
                :color="getSeverityColor(formData.severity)"
            />
        </UFormField>

        <UFormField :label="t('views.adminfaced.review.reviewEntryMessage')" size="sm">
            <UTextarea
                v-model="formData.message"
                :placeholder="t('views.adminfaced.review.reviewEntryMessagePlaceholder')"
                :rows="3"
                autoresize
                class="w-full"
                size="sm"
                variant="outline"
            />
        </UFormField>

        <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')" size="sm">
            <UTextarea
                v-model="formData.suggestion"
                :placeholder="t('views.adminfaced.review.reviewEntrySuggestionPlaceholder')"
                :rows="2"
                autoresize
                class="w-full"
                size="sm"
                variant="outline"
            />
        </UFormField>

        <div class="flex justify-end pt-2">
            <UButton
                :disabled="!formData.message.trim()"
                block
                color="primary"
                size="sm"
                variant="solid"
                @click="handleSubmit"
            >
                {{ submitLabel }}
            </UButton>
        </div>
    </div>
</template>
