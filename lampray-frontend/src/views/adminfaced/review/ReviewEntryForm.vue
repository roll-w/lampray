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
import {ref} from "vue";
import {ReviewCategory, type ReviewFeedbackEntry, ReviewSeverity} from "@/services/content/review.type";
import {useI18n} from "vue-i18n";

/**
 * @author RollW
 */

const props = defineProps<{
    initialMessage?: string;
    location?: any;
}>();

const emit = defineEmits<{
    (e: 'submit', entry: ReviewFeedbackEntry): void;
    (e: 'cancel'): void;
}>();

const {t} = useI18n();

const form = ref({
    category: ReviewCategory.CONTENT_QUALITY,
    severity: ReviewSeverity.MINOR,
    message: props.initialMessage || "",
    suggestion: ""
});

const categoryOptions = [
    {label: t("views.adminfaced.review.reviewCategories.CONTENT_QUALITY"), value: ReviewCategory.CONTENT_QUALITY},
    {label: t("views.adminfaced.review.reviewCategories.GRAMMAR"), value: ReviewCategory.GRAMMAR},
    {label: t("views.adminfaced.review.reviewCategories.FORMAT"), value: ReviewCategory.FORMAT},
    {label: t("views.adminfaced.review.reviewCategories.POLICY_VIOLATION"), value: ReviewCategory.POLICY_VIOLATION},
    {label: t("views.adminfaced.review.reviewCategories.SENSITIVE_CONTENT"), value: ReviewCategory.SENSITIVE_CONTENT},
    {label: t("views.adminfaced.review.reviewCategories.COPYRIGHT"), value: ReviewCategory.COPYRIGHT},
    {label: t("views.adminfaced.review.reviewCategories.TECHNICAL"), value: ReviewCategory.TECHNICAL},
    {label: t("views.adminfaced.review.reviewCategories.OTHER"), value: ReviewCategory.OTHER}
];

const severityOptions = [
    {label: t("views.adminfaced.review.reviewSeverities.CRITICAL"), value: ReviewSeverity.CRITICAL},
    {label: t("views.adminfaced.review.reviewSeverities.MAJOR"), value: ReviewSeverity.MAJOR},
    {label: t("views.adminfaced.review.reviewSeverities.MINOR"), value: ReviewSeverity.MINOR},
    {label: t("views.adminfaced.review.reviewSeverities.INFO"), value: ReviewSeverity.INFO}
];

const handleSubmit = () => {
    if (!form.value.message.trim()) return;

    emit('submit', {
        category: form.value.category,
        severity: form.value.severity,
        message: form.value.message.trim(),
        suggestion: form.value.suggestion.trim() || undefined,
        locationRange: props.location,
        reviewerSource: {
            isAutomatic: false,
            reviewerName: "manual"
        }
    });
};
</script>

<template>
    <div class="p-6 space-y-6">
        <div class="flex items-center justify-between border-b border-neutral-100 dark:border-neutral-800 pb-4 mb-2">
            <h3 class="text-md font-bold text-neutral-900 dark:text-white tracking-tight">
                {{ t('views.adminfaced.review.addFeedback') }}
            </h3>
            <UButton
                    class="rounded-full"
                    color="neutral"
                    icon="i-lucide-x"
                    variant="ghost"
                    @click="emit('cancel')"
            />
        </div>

        <div class="grid grid-cols-2 gap-4">
            <UFormField :label="t('views.adminfaced.review.reviewEntryCategory')">
                <USelectMenu
                        v-model="form.category"
                        :items="categoryOptions"
                        :ui="{ rounded: 'rounded-lg' }"
                        size="md"
                        value-key="value"
                />
            </UFormField>
            <UFormField :label="t('views.adminfaced.review.reviewEntrySeverity')">
                <USelectMenu
                        v-model="form.severity"
                        :items="severityOptions"
                        :ui="{ rounded: 'rounded-lg' }"
                        size="md"
                        value-key="value"
                />
            </UFormField>
        </div>

        <UFormField :label="t('views.adminfaced.review.reviewEntryMessage')">
            <UTextarea
                    v-model="form.message"
                    :placeholder="t('views.adminfaced.review.reviewEntryMessagePlaceholder')"
                    :rows="3"
                    :ui="{ rounded: 'rounded-lg' }"
                    autofocus
                    size="md"
            />
        </UFormField>

        <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')">
            <UTextarea
                    v-model="form.suggestion"
                    :placeholder="t('views.adminfaced.review.reviewEntrySuggestionPlaceholder')"
                    :rows="2"
                    :ui="{ rounded: 'rounded-lg' }"
                    size="md"
            />
        </UFormField>

        <div class="flex gap-3 pt-4 border-t border-neutral-100 dark:border-neutral-800">
            <UButton
                    block
                    class="rounded-lg"
                    color="primary"
                    @click="handleSubmit"
            >
                {{ t('views.adminfaced.review.submitEntry') }}
            </UButton>
            <UButton
                    block
                    class="rounded-lg"
                    color="neutral"
                    variant="ghost"
                    @click="emit('cancel')">
                {{ t('common.cancel') }}
            </UButton>
        </div>
    </div>
</template>
