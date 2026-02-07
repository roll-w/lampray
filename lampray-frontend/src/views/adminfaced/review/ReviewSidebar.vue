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
import {onMounted, ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import type {ContentLocationRange, ReviewFeedbackEntry, ReviewJobView} from "@/services/content/review.type";
import {ReviewCategory, ReviewSeverity, ReviewVerdict} from "@/services/content/review.type";
import ReviewActionPanel from "./ReviewActionPanel.vue";
import ReviewFeedbackEntries from "./ReviewFeedbackEntries.vue";
import {useAxios} from "@/composables/useAxios.ts";
import {reviewService} from "@/services/content/review.service.ts";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";

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
}>();

const emit = defineEmits<{
    (e: 'update:summary', value: string): void;
    (e: 'update:draft', value: ReviewEntryDraft | null): void;
    (e: 'submit-success'): void;
    (e: 'locate-entry', entry: ReviewFeedbackEntry): void;
}>();

const {t} = useI18n();
const axios = useAxios();
const reviewApi = reviewService(axios);
const toast = useToast();

const entries = ref<ReviewFeedbackEntry[]>([]);
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

const loadEntries = async () => {
    if (!props.job || !props.taskId) return;
    loading.value = true;
    try {
        const response = await reviewApi.getReviewJob(props.job.id);
        const jobDetails = response.data.data;
        if (jobDetails && jobDetails.tasks) {
            const task = jobDetails.tasks.find(t => t.taskId === props.taskId);
            if (task && task.feedback && task.feedback.entries) {
                entries.value = task.feedback.entries;
            } else {
                entries.value = [];
            }
        }
    } catch (e) {
        console.error("Failed to load entries", e);
    } finally {
        loading.value = false;
    }
};

watch(() => props.job.id, () => {
    loadEntries();
}, {immediate: true});


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
    emit('update:draft', { text: '', location: undefined });
};

const handleEntrySubmit = () => {
    if (!formState.value.message.trim()) return;

    const newEntry: ReviewFeedbackEntry = {
        message: formState.value.message,
        suggestion: formState.value.suggestion,
        severity: formState.value.severity,
        category: formState.value.category,
        locationRange: props.draft?.location,
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

const handleSubmitReview = async (verdict: ReviewVerdict) => {
    if (props.disabled) return;

    if (verdict !== ReviewVerdict.APPROVED && entries.value.length === 0 && !props.summary.trim()) {
         toast.add(newErrorToastFromError(
                new Error(t("views.adminfaced.review.validation.reasonRequired")),
                t("views.adminfaced.review.validation.title")
        ));
        return;
    }

    submitting.value = true;
    try {
        await reviewApi.makeReview(props.job.id, props.taskId, {
            verdict: verdict,
            entries: entries.value,
            summary: props.summary.trim() || undefined
        });
        toast.add(newSuccessToast(t("views.adminfaced.review.approveSuccess")));
        emit('submit-success');
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        submitting.value = false;
    }
};

</script>

<template>
    <div class="h-full max-h-[calc(100vh-64px)] flex flex-col border-l border-neutral-200 dark:border-neutral-800">
        <div class="px-5 py-4 border-b border-neutral-200 dark:border-neutral-800 flex items-center justify-between sticky top-0 bg-white/80 dark:bg-neutral-900/80 backdrop-blur-sm z-10">
            <div class="flex flex-col gap-0.5">
                <div class="flex items-center gap-2">
                    <span class="text-xs font-mono text-neutral-400">#{{ job.id }}</span>
                </div>
                <h2 class="text-sm font-semibold text-neutral-900 dark:text-white tracking-tight">
                    Review Panel
                </h2>
            </div>

            <div class="flex items-center gap-3">
                <div class="relative w-20 flex items-center justify-center" v-if="progress !== undefined">
                     <UProgress :value="progress" size="xs" :max="100" color="primary"/>
                </div>

                <UTooltip :text="t('views.adminfaced.review.addGeneralEntry')">
                    <UButton
                            color="neutral"
                            icon="i-lucide-plus"
                            size="xs"
                            variant="ghost"
                            :ui="{ rounded: 'rounded-full' }"
                            @click="handleCreateGeneralEntry"
                    />
                </UTooltip>
            </div>
        </div>

        <div class="flex-1 overflow-y-auto p-5 space-y-6">
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
                            <span v-if="draft.location" class="font-mono text-neutral-400">
                                {{ draft.location.startInNode }}:{{ draft.location.endInNode }}
                            </span>
                        </div>
                        <p class="text-xs text-neutral-600 dark:text-neutral-400 italic line-clamp-3 font-serif">
                            "{{ draft.text }}"
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
                                size="sm"
                                variant="outline"
                                class="w-full"
                        />
                    </UFormField>
                    <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')">
                        <UTextarea
                                v-model="formState.suggestion"
                                :placeholder="t('views.adminfaced.review.reviewEntrySuggestionPlaceholder')"
                                :rows="2"
                                autoresize
                                size="sm"
                                variant="outline"
                                class="w-full"
                        />
                    </UFormField>

                    <div class="flex justify-end pt-2">
                        <UButton
                                :disabled="!formState.message.trim()"
                                color="primary"
                                size="sm"
                                variant="solid"
                                block
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
