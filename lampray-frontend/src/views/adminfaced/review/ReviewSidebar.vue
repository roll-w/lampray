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
import {ReviewCategory, ReviewSeverity} from "@/services/content/review.type";
import ReviewActionPanel from "@/views/adminfaced/review/ReviewActionPanel.vue";
import ReviewFeedbackEntries from "@/views/adminfaced/review/ReviewFeedbackEntries.vue";
import ReviewEntryForm, {type EntryFormData} from "@/views/adminfaced/review/ReviewEntryForm.vue";
import {
    type LocalReviewEntry,
    type ReviewEntryDraft,
    useReviewQueueActions,
    useReviewQueueDraft,
    useReviewQueueState
} from "@/views/adminfaced/review/reviewQueueContext.ts";
import {useReviewDraftStorage} from "@/views/adminfaced/review/useReviewDraftStorage.ts";

const {t} = useI18n();
const {
    job,
    task,
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
const {summary: summaryRef} = useReviewQueueState();
const draftModel = useReviewQueueDraft();
const {saveDraft, getDraft, clearDraft, hasDraft} = useReviewDraftStorage();

const editingIndex = ref<number>(-1);
const isRestoringDraft = ref(false);
const showDiscardConfirm = ref(false);

const isCreatingNew = computed(() => draft.value !== null && editingIndex.value === -1);
const currentDraftExists = computed(() => !!(job.value && task.value && hasDraft(job.value.id, task.value.taskId)));

const newEntryForm = ref<EntryFormData>({
    message: "",
    suggestion: "",
    severity: ReviewSeverity.CRITICAL,
    category: ReviewCategory.OTHER
});

const resetNewForm = () => {
    newEntryForm.value = {
        message: "",
        suggestion: "",
        severity: ReviewSeverity.CRITICAL,
        category: ReviewCategory.OTHER
    };
};

watch(() => draft.value, (newVal) => {
    if (newVal && editingIndex.value === -1) {
        resetNewForm();
    }
});

watch([() => job.value, () => task.value], ([newJob, newTask]) => {
    if (!newJob || !newTask) return;

    // reset editing state
    editingIndex.value = -1;

    // Auto-restore draft silently if exists
    const saved = getDraft(newJob.id, newTask.taskId);
    if (saved) {
        isRestoringDraft.value = true;
        entries.value = [...saved.entries];
        summaryRef.value = saved.summary;
        // ensure draft model is cleared so form doesn't treat it as "creating new"
        draftModel.value = null;
        isRestoringDraft.value = false;
    }
}, {immediate: true});

// Debounced save to batch rapid changes
let saveTimeout: ReturnType<typeof setTimeout> | null = null;
const scheduleSave = () => {
    if (saveTimeout) clearTimeout(saveTimeout);
    saveTimeout = setTimeout(() => {
        if (isRestoringDraft.value) return;
        if (job.value && task.value && entries.value.length > 0) {
            saveDraft(job.value.id, task.value.taskId, entries.value, summaryRef.value);
        }
    }, 100);
};

watch(entries, () => {
    scheduleSave();
}, {deep: true});

watch(summaryRef, () => {
    scheduleSave();
});

const confirmDiscardDraft = () => {
    if (!job.value || !task.value) return;
    clearDraft();
    showDiscardConfirm.value = false;
};

const handleCancelNew = () => {
    draftModel.value = null;
    resetNewForm();
};

const handleCancelEdit = () => {
    editingIndex.value = -1;
    // Clear draft when canceling edit to avoid being treated as "creating new"
    draftModel.value = null;
};

const handleCreateGeneralEntry = () => {
    editingIndex.value = -1;
    resetNewForm();
    draftModel.value = {text: "", location: undefined};
};

const handleEditEntry = (index: number) => {
    const entry = entries.value[index];
    if (!entry || entry.reviewerSource?.isAutomatic) return;

    editingIndex.value = index;
    draftModel.value = {
        text: entry.originalText,
        location: entry.locationRange
    };
};

const handleSaveEntry = (index: number, data: Partial<LocalReviewEntry>) => {
    const entry = entries.value[index];
    if (!entry) return;

    entries.value[index] = {
        ...entry,
        ...data
    };
    editingIndex.value = -1;
    draftModel.value = null;
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

const handleNewEntrySubmit = () => {
    if (!newEntryForm.value.message.trim()) return;

    const entryData = buildLocalEntry({
        message: newEntryForm.value.message,
        suggestion: newEntryForm.value.suggestion,
        severity: newEntryForm.value.severity,
        category: newEntryForm.value.category,
        location: draft.value?.location,
        text: draft.value?.text
    });

    entries.value.unshift(entryData);
    draftModel.value = null;
    resetNewForm();
};

const handleRemoveEntry = (index: number) => {
    const removedEntry = entries.value[index] ?? null;
    entries.value.splice(index, 1);
    if (removedEntry && removedEntry === selectedEntry.value) {
        selectEntry(null);
    }
    if (editingIndex.value === index) {
        editingIndex.value = -1;
    }
};

const handleSelectEntry = (entry: LocalReviewEntry) => {
    toggleEntrySelection(entry);
};

const handleSubmitReview = async (verdict: any) => {
    await submitReview(verdict);
    if (job.value && task.value) {
        clearDraft();
    }
};
</script>

<template>
    <div class="h-full flex flex-col border border-neutral-200 dark:border-neutral-800 rounded-xl">
        <div class="px-5 py-4 flex items-center justify-between">
            <div class="flex flex-col gap-0.5">
                <h2 class="text-sm font-semibold text-neutral-900 dark:text-white tracking-tight">
                    {{ t("views.adminfaced.review.title") }}
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

                <div v-if="currentDraftExists" class="flex items-center gap-1.5 text-xs text-neutral-500 dark:text-neutral-400">
                    <div class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"/>
                    <span>{{ t('views.adminfaced.review.draftSavedLocal') }}</span>
                    <UPopover v-model:open="showDiscardConfirm">
                        <UButton
                                color="error"
                                size="xs"
                                variant="ghost">
                            {{ t('common.discard') }}
                        </UButton>

                        <template #content>
                            <div class="p-3 space-y-3 min-w-56">
                                <p class="text-sm font-medium text-neutral-900 dark:text-white">
                                    {{ t('views.adminfaced.review.discardDraftTitle') }}
                                </p>
                                <p class="text-xs text-neutral-600 dark:text-neutral-400">
                                    {{ t('views.adminfaced.review.discardDraftConfirm') }}
                                </p>
                                <div class="flex justify-end gap-2">
                                    <UButton
                                            color="neutral"
                                            size="xs"
                                            variant="ghost"
                                            @click="showDiscardConfirm = false">
                                        {{ t("common.cancel") }}
                                    </UButton>
                                    <UButton
                                            color="error"
                                            size="xs"
                                            @click="confirmDiscardDraft">
                                        {{ t("common.discard") }}
                                    </UButton>
                                </div>
                            </div>
                        </template>
                    </UPopover>
                </div>
            </div>

            <div class="flex items-center gap-1">
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

        <div class="flex-1 p-5 space-y-6 overflow-y-auto">
            <ReviewEntryForm
                    v-if="isCreatingNew"
                    :context-text="draft?.text"
                    :model-value="newEntryForm"
                    :submit-label="t('views.adminfaced.review.reviewEntryAdd')"
                    :title="t('views.adminfaced.review.newFeedback')"
                    class="animate-in fade-in slide-in-from-right-4 duration-300"
                    @cancel="handleCancelNew"
                    @submit="handleNewEntrySubmit"
                    @update:model-value="newEntryForm = $event"
            />

            <ReviewFeedbackEntries
                    :editing-index="editingIndex"
                    :entries="entries"
                    :selected-entry="selectedEntry"
                    @cancel="handleCancelEdit"
                    @edit="handleEditEntry"
                    @locate="handleSelectEntry"
                    @remove="handleRemoveEntry"
                    @save="handleSaveEntry"
                    @select="handleSelectEntry"
            />

            <div class="pt-6 border-t border-neutral-100 dark:border-neutral-800">
                <ReviewActionPanel
                        :disabled="disabled || submitting"
                        :loading="submitting"
                        :summary="summaryRef"
                        @submit="handleSubmitReview"
                        @update:summary="val => summaryRef = val"
                />
            </div>
        </div>

    </div>
</template>
