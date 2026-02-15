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
import {ReviewCategory, ReviewSeverity} from "@/services/content/review.type";
import ReviewActionPanel from "./ReviewActionPanel.vue";
import ReviewFeedbackEntries from "./ReviewFeedbackEntries.vue";
import {
    type LocalReviewEntry,
    type ReviewEntryDraft,
    useReviewQueueActions,
    useReviewQueueDraft,
    useReviewQueueState,
    useReviewQueueSummary
} from "./reviewQueueContext.ts";

const {t} = useI18n();
const {
    job,
    entries,
    draft,
    selectedEntry,
    progress,
    isFirst,
    isLast,
    disabled,
    submitting
} = useReviewQueueState();
const {selectEntry, toggleEntrySelection, submitReview, prevJob, nextJob} = useReviewQueueActions();
const summary = useReviewQueueSummary();
const draftModel = useReviewQueueDraft();

const formState = ref({
    message: "",
    suggestion: "",
    severity: ReviewSeverity.CRITICAL,
    category: ReviewCategory.OTHER
});

const severityOptions = [
    {label: t('views.adminfaced.review.severity.CRITICAL'), value: ReviewSeverity.CRITICAL},
    {label: t('views.adminfaced.review.severity.MAJOR'), value: ReviewSeverity.MAJOR},
    {label: t('views.adminfaced.review.severity.MINOR'), value: ReviewSeverity.MINOR},
    {label: t('views.adminfaced.review.severity.INFO'), value: ReviewSeverity.INFO}
];

const categoryOptions = Object.values(ReviewCategory).map(c => ({
    label: t(`views.adminfaced.review.category.${c}`),
    value: c
}));

// Watch for external trigger to start creating entry
watch(() => draft.value, (newVal) => {
    if (newVal) {
        // Clear form when starting new entry
        formState.value.message = "";
        formState.value.suggestion = "";
    }
});

const handleCancelEntry = () => {
    draftModel.value = null;
};

const handleCreateGeneralEntry = () => {
    draftModel.value = {text: "", location: undefined};
};

const buildLocalEntry = (payload: {
    message: string;
    suggestion: string;
    severity: ReviewSeverity;
    category: ReviewCategory;
    location?: ReviewEntryDraft["location"];
    text?: ReviewEntryDraft["text"];
}): LocalReviewEntry => ({
    message: payload.message,
    suggestion: payload.suggestion,
    severity: payload.severity,
    category: payload.category,
    locationRange: payload.location,
    originalText: payload.text,
    reviewerSource: {
        isAutomatic: false,
        reviewerName: "manual"
    }
});

const handleEntrySubmit = () => {
    if (!formState.value.message.trim()) return;

    const newEntry = buildLocalEntry({
        message: formState.value.message,
        suggestion: formState.value.suggestion,
        severity: formState.value.severity,
        category: formState.value.category,
        location: draft.value?.location,
        text: draft.value?.text
    });

    entries.value.unshift(newEntry);
    draftModel.value = null;

    // Reset form
    formState.value.message = "";
    formState.value.suggestion = "";
};

const handleRemoveEntry = (index: number) => {
    const removedEntry = entries.value[index] ?? null;
    entries.value.splice(index, 1);
    if (removedEntry && removedEntry === selectedEntry.value) {
        selectEntry(null);
    }
};

const handleSelectEntry = (entry: LocalReviewEntry) => {
    toggleEntrySelection(entry);
};

</script>

<template>
    <div class="h-full max-h-[calc(100vh-64px)] flex flex-col border border-neutral-200 dark:border-neutral-800 rounded-xl">
        <div class="px-5 py-4 flex items-center justify-between">
            <div class="flex flex-col gap-0.5">
                <h2 class="text-sm font-semibold text-neutral-900 dark:text-white tracking-tight">
                    Review Panel
                </h2>
                <div class="flex items-center gap-2">
                    <span class="text-xs font-mono text-neutral-400">#{{ job?.id }}</span>
                </div>
                <div class="relative w-full flex items-center justify-center">
                    <UProgress :max="100" :model-value="progress" color="primary" size="sm"/>
                    <span class="ms-2 text-[10px] font-mono">
                        {{ progress }}%
                    </span>
                </div>
            </div>

            <div class="flex items-center gap-3">
                <UTooltip :text="t('views.adminfaced.review.reviewEntryAdd')">
                    <UButton
                            :ui="{ rounded: 'rounded-full' }"
                            color="neutral"
                            icon="i-lucide-plus"
                            size="xs"
                            variant="ghost"
                            @click="handleCreateGeneralEntry"
                    />
                </UTooltip>

                <div class="w-px h-3 bg-neutral-200 dark:bg-neutral-800 mx-1"/>

                <div class="flex items-center gap-0.5">
                    <UButton
                            :disabled="isFirst"
                            :ui="{ rounded: 'rounded-full' }"
                            color="neutral"
                            icon="i-lucide-chevron-left"
                            size="xs"
                            variant="ghost"
                            @click="prevJob"
                    />
                    <UButton
                            :disabled="isLast"
                            :ui="{ rounded: 'rounded-full' }"
                            color="neutral"
                            icon="i-lucide-chevron-right"
                            size="xs"
                            variant="ghost"
                            @click="nextJob"
                    />
                </div>
            </div>
        </div>

        <div class="flex-1 p-5 space-y-6">
            <div v-if="draft" class="animate-in fade-in slide-in-from-right-4 duration-300">
                <div class="p-4 rounded-lg border border-neutral-200 dark:border-neutral-800 space-y-4">
                    <div class="flex items-center justify-between">
                         <span class="text-xs font-semibold text-neutral-900 dark:text-white uppercase tracking-wider">
                            {{ t('views.adminfaced.review.newFeedback') }}
                         </span>
                        <UButton
                                color="neutral"
                                icon="i-lucide-x"
                                size="xs"
                                variant="ghost"
                                @click="handleCancelEntry"
                        />
                    </div>

                    <div v-if="draft.text"
                         class="rounded-md p-3 border border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/50">
                        <div class="text-[10px] uppercase font-bold text-neutral-400 mb-1 tracking-wider flex justify-between">
                            <span>{{ t('views.adminfaced.review.reviewEntryContextLabel') }}</span>
                        </div>
                        <div :title="draft.text"
                             class="w-full text-xs text-neutral-600 dark:text-neutral-400 italic font-mono truncate">
                            "{{ draft.text }}"
                        </div>
                    </div>

                    <UFormField :label="t('views.adminfaced.review.reviewEntryCategory')" size="sm">
                        <USelectMenu v-model="formState.category"
                                     :items="categoryOptions"
                                     class="w-full"
                                     size="sm"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.review.reviewEntrySeverity')" size="sm">
                        <UTabs v-model="formState.severity"
                               :items="severityOptions"
                               size="xs"
                               variant="pill"
                        />
                    </UFormField>
                    <UFormField :label="t('views.adminfaced.review.reviewEntryMessage')" size="sm">
                        <UTextarea
                                v-model="formState.message"
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
                                v-model="formState.suggestion"
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
                                :disabled="!formState.message.trim()"
                                block
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
                    :selected-entry="selectedEntry"
                    @locate="handleSelectEntry"
                    @remove="handleRemoveEntry"
                    @select="handleSelectEntry"
            />

            <div class="pt-6 border-t border-neutral-100 dark:border-neutral-800">
                <ReviewActionPanel
                        :disabled="disabled || submitting"
                        :loading="submitting"
                        :summary="summary"
                        @submit="submitReview"
                        @update:summary="val => summary = val"
                />
            </div>
        </div>
    </div>
</template>
