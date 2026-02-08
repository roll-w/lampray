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
import {computed, ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import type {ReviewFeedbackEntry, ReviewJobView} from "@/services/content/review.type";
import {ReviewCategory, ReviewSeverity, ReviewVerdict} from "@/services/content/review.type";
import ReviewActionPanel from "./ReviewActionPanel.vue";
import ReviewFeedbackEntries from "./ReviewFeedbackEntries.vue";
import {useAxios} from "@/composables/useAxios.ts";
import {reviewService} from "@/services/content/review.service.ts";
import type {LocalReviewEntry} from "@/views/adminfaced/review/ReviewQueuePage.vue";
import type {ContentLocationRange} from "@/components/structuraltext/types.ts";

export interface ReviewEntryDraft {
    text?: string;
    location?: ContentLocationRange;
}

const props = defineProps<{
    job: ReviewJobView;
    taskId: string;
    summary: string;
    disabled?: boolean;
    draft?: ReviewEntryDraft | null;
    progress?: number;
    entries?: LocalReviewEntry[];
    isFirst?: boolean;
    isLast?: boolean;
    selectedEntry?: ReviewFeedbackEntry | null;
}>();

const emit = defineEmits<{
    (e: 'update:summary', value: string): void;
    (e: 'update:draft', value: ReviewEntryDraft | null): void;
    (e: 'update:entries', value: any[]): void;
    (e: 'submit', verdict: ReviewVerdict): void;
    (e: 'locate-entry', entry: ReviewFeedbackEntry | null): void;
    (e: 'prev-job'): void;
    (e: 'next-job'): void;
}>();

const {t} = useI18n();
const axios = useAxios();
const reviewApi = reviewService(axios);
const toast = useToast();

const localEntries = ref<ReviewFeedbackEntry[]>([]);
const entries = computed({
    get: () => props.entries ?? localEntries.value,
    set: (val) => {
        localEntries.value = val;
        emit('update:entries', val);
    }
});
const loading = ref(false);
const submitting = ref(false);

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
    label: c,
    value: c
}));


// Watch for external trigger to start creating entry
watch(() => props.draft, (newVal) => {
    if (newVal) {
        // Clear form when starting new entry
        formState.value.message = '';
        formState.value.suggestion = '';
    }
});

const handleCancelEntry = () => {
    emit('update:draft', null);
};

const handleCreateGeneralEntry = () => {
    emit('update:draft', {text: '', location: undefined});
};

const handleEntrySubmit = () => {
    if (!formState.value.message.trim()) return;

    const newEntry: LocalReviewEntry = {
        message: formState.value.message,
        suggestion: formState.value.suggestion,
        severity: formState.value.severity,
        category: formState.value.category,
        locationRange: props.draft?.location,
        originalText: props.draft?.text,
        reviewerSource: {
            isAutomatic: false,
            reviewerName: 'manual'
        }
    };

    entries.value.unshift(newEntry);
    emit('update:draft', null);

    // Reset form
    formState.value.message = '';
    formState.value.suggestion = '';
};

const handleRemoveEntry = (index: number) => {
    entries.value.splice(index, 1);
};

const handleSelectEntry = (entry: ReviewFeedbackEntry) => {
    // Toggle: if already selected, deselect (emit null)
    if (props.selectedEntry === entry) {
        emit("locate-entry", null);
    } else {
        emit("locate-entry", entry);
    }
};

const handleSubmitReview = (verdict: ReviewVerdict) => {
    emit("submit", verdict);
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
                    <span class="text-xs font-mono text-neutral-400">#{{ job.id }}</span>
                </div>
                <div v-if="progress !== undefined" class="relative w-full flex items-center justify-center">
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
                            @click="emit('prev-job')"
                    />
                    <UButton
                            :disabled="isLast"
                            :ui="{ rounded: 'rounded-full' }"
                            color="neutral"
                            icon="i-lucide-chevron-right"
                            size="xs"
                            variant="ghost"
                            @click="emit('next-job')"
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
                                class="w-full"
                                size="sm"
                                variant="outline"
                        />
                    </UFormField>
                    <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')">
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
                    :selected-entry="props.selectedEntry"
                    @locate="handleSelectEntry"
                    @select="handleSelectEntry"
                    @remove="handleRemoveEntry"
            />

            <div class="pt-6 border-t border-neutral-100 dark:border-neutral-800">
                <ReviewActionPanel
                        :disabled="disabled || submitting"
                        :loading="submitting"
                        :summary="summary"
                        @submit="handleSubmitReview"
                        @update:summary="val => emit('update:summary', val)"
                />
            </div>
        </div>
    </div>
</template>
