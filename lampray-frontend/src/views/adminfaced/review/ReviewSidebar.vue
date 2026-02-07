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
import {ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import type {ReviewFeedbackEntry, ReviewJobView} from "@/services/content/review.type";
import {ReviewCategory, ReviewSeverity, ReviewVerdict} from "@/services/content/review.type";
import ReviewActionPanel from "./ReviewActionPanel.vue";
import ReviewFeedbackEntries from "./ReviewFeedbackEntries.vue";

const props = defineProps<{
    job?: ReviewJobView;
    entries: ReviewFeedbackEntry[];
    summary: string;
    loading?: boolean;
    disabled?: boolean;
    creatingEntry?: boolean;
    initialEntryText?: string;
    initialEntryLocation?: any; // ContentLocationRange
}>();

const emit = defineEmits<{
    (e: 'update:summary', value: string): void;
    (e: 'submit-review', verdict: ReviewVerdict): void;
    (e: 'submit-entry', entry: ReviewFeedbackEntry): void;
    (e: 'cancel-entry'): void;
    (e: 'remove-entry', index: number): void;
    (e: 'locate-entry', entry: ReviewFeedbackEntry): void;
    (e: 'create-general-entry'): void;
}>();

const {t} = useI18n();

const formState = ref({
    message: '',
    suggestion: '',
    severity: ReviewSeverity.MINOR,
    category: ReviewCategory.CONTENT_QUALITY
});

const severityOptions = [
    {label: t('views.adminfaced.review.severity.MINOR'), value: ReviewSeverity.MINOR},
    {label: t('views.adminfaced.review.severity.MAJOR'), value: ReviewSeverity.MAJOR},
    {label: t('views.adminfaced.review.severity.CRITICAL'), value: ReviewSeverity.CRITICAL},
    {label: t('views.adminfaced.review.severity.INFO'), value: ReviewSeverity.INFO}
];

const categoryOptions = Object.values(ReviewCategory).map(c => ({
    label: c, // TODO: Add i18n for categories if needed, or just capitalize
    value: c
}));

// Watch for external trigger to start creating entry
watch(() => props.creatingEntry, (newVal) => {
    if (newVal) {
        // Clear form when starting new entry
        formState.value.message = '';
        formState.value.suggestion = '';
    }
});

const handleEntrySubmit = () => {
    if (!formState.value.message.trim()) return;

    emit("submit-entry", {
        message: formState.value.message,
        suggestion: formState.value.suggestion,
        severity: formState.value.severity,
        category: formState.value.category,
        locationRange: props.initialEntryLocation,
        reviewerSource: {
            isAutomatic: false,
            reviewerName: 'manual'
        }
    });

    // Reset form
    formState.value.message = '';
    formState.value.suggestion = '';
};
</script>

<template>
    <div class="h-full flex flex-col border-l border-neutral-200 dark:border-neutral-800">
        <div class="px-6 py-5 border-b border-neutral-200 dark:border-neutral-800 flex items-start justify-between sticky top-0 z-0">
            <div class="flex flex-col gap-1">
                <div class="flex items-center gap-2">
                    <span v-if="job" class="text-xs font-mono text-neutral-300">#{{ job.id }}</span>
                </div>
                <h2 class="text-xl font-bold text-neutral-900 dark:text-white tracking-tight">
                    Review Panel
                </h2>
                <div class="text-xs text-neutral-500 font-medium">
                    {{ entries.length }} {{ t('views.adminfaced.review.entries') }}
                </div>
            </div>

            <div class="flex items-center gap-4">
                <div class="relative size-10 flex items-center justify-center">
                    <!-- TODO: progress-->
                </div>

                <UTooltip :text="t('views.adminfaced.review.addGeneralEntry')">
                    <UButton
                            color="neutral"
                            icon="i-lucide-plus"
                            size="xs"
                            variant="ghost"
                            @click="emit('create-general-entry')"
                    />
                </UTooltip>
            </div>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-8">
            <div v-if="creatingEntry" class="animate-in fade-in slide-in-from-right-4 duration-300">
                <div class="p-4 rounded-xl border space-y-4 border-neutral-200 dark:border-neutral-800">
                    <div class="flex items-center justify-between mb-2">
                         <span class="text-xs font-bold text-primary-600 dark:text-primary-400 uppercase tracking-wider">
                            {{ t('views.adminfaced.review.newFeedback') }}
                         </span>
                        <UButton
                                color="neutral"
                                icon="i-lucide-x"
                                size="xs"
                                variant="ghost"
                                @click="emit('cancel-entry')"
                        />
                    </div>

                    <div v-if="initialEntryText"
                         class="bg-white dark:bg-neutral-900 rounded-md p-3 border border-neutral-200 dark:border-neutral-800 shadow-sm">
                        <div class="text-[10px] uppercase font-bold text-neutral-400 mb-1 tracking-wider flex justify-between">
                            <span>{{ t('views.adminfaced.review.reviewEntryContextLabel') }}</span>
                            <span v-if="initialEntryLocation" class="font-mono text-neutral-300">
                                {{ initialEntryLocation.startInNode }}:{{ initialEntryLocation.endInNode }}
                            </span>
                        </div>
                        <p class="text-xs text-neutral-600 dark:text-neutral-300 italic line-clamp-3">
                            "{{ initialEntryText }}"
                        </p>
                    </div>

                    <UFormField :label="t('views.adminfaced.review.category')">
                        <USelectMenu
                                v-model="formState.category"
                                :options="categoryOptions"
                                option-attribute="label"
                                size="sm"
                                value-attribute="value"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.review.reviewEntrySeverity')">
                        <UTabs v-model="formState.severity"
                               :items="severityOptions"
                               size="xs"
                               variant="pill"
                        />
                    </UFormField>
                    <UFormField :label="t('views.adminfaced.review.reviewEntryMessage')">
                        <UTextarea
                                v-model="formState.message"
                                :placeholder="t('views.adminfaced.review.reviewEntryMessagePlaceholder')"
                                :rows="3"
                                autoresize
                                class="w-full bg-white dark:bg-neutral-900 p-2 rounded-md"
                        />
                    </UFormField>
                    <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')">
                        <UTextarea
                                v-model="formState.suggestion"
                                :placeholder="t('views.adminfaced.review.reviewEntrySuggestionPlaceholder')"
                                :rows="3"
                                autoresize
                                class="w-full bg-white dark:bg-neutral-900 p-2 rounded-md"
                        />
                    </UFormField>

                    <div class="flex justify-end pt-2">
                        <UButton
                                :disabled="!formState.message.trim()"
                                color="primary"
                                size="sm"
                                variant="solid"
                                @click="handleEntrySubmit"
                        >
                            {{ t('views.adminfaced.review.reviewEntryAdd') }}
                        </UButton>
                    </div>
                </div>
            </div>

            <ReviewFeedbackEntries
                    :entries="entries"
                    @locate="e => emit('locate-entry', e)"
                    @remove="i => emit('remove-entry', i)"
            />

            <div class="pt-8 border-t border-neutral-100 dark:border-neutral-800">
                <ReviewActionPanel
                        :disabled="disabled"
                        :loading="loading"
                        :summary="summary"
                        @submit="verdict => emit('submit-review', verdict)"
                        @update:summary="val => emit('update:summary', val)"
                />
            </div>
        </div>
    </div>
</template>
