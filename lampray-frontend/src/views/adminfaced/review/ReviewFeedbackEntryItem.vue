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
import type {LocalReviewEntry} from "@/views/adminfaced/review/reviewQueueContext.ts";
import {ReviewSeverity} from "@/services/content/review.type.ts";
import {ref, watch} from "vue";
import ReviewEntryForm, {type EntryFormData} from "@/views/adminfaced/review/ReviewEntryForm.vue";

const props = defineProps<{
    entry: LocalReviewEntry;
    index: number;
    isSelected: boolean;
    isEditing: boolean;
}>();

const emit = defineEmits<{
    (e: "select", entry: LocalReviewEntry): void;
    (e: "locate", entry: LocalReviewEntry): void;
    (e: "remove", index: number): void;
    (e: "edit", index: number): void;
    (e: "save", index: number, data: Partial<LocalReviewEntry>): void;
    (e: "cancel", index: number): void;
}>();

const {t} = useI18n();

const showRemoveConfirm = ref(false);
const editFormData = ref<EntryFormData>({
    message: "",
    suggestion: "",
    severity: ReviewSeverity.CRITICAL,
    category: props.entry.category
});

watch(() => props.isEditing, (isEditing) => {
    if (isEditing) {
        editFormData.value = {
            message: props.entry.message,
            suggestion: props.entry.suggestion || "",
            severity: props.entry.severity,
            category: props.entry.category
        };
    }
}, {immediate: true});

const getSeverityIcon = (severity: ReviewSeverity) => {
    switch (severity) {
        case ReviewSeverity.CRITICAL:
            return "i-lucide-shield-alert";
        case ReviewSeverity.MAJOR:
            return "i-lucide-alert-triangle";
        case ReviewSeverity.MINOR:
            return "i-lucide-alert-circle";
        case ReviewSeverity.INFO:
            return "i-lucide-info";
    }
};

const getSeverityClass = (severity: ReviewSeverity) => {
    switch (severity) {
        case ReviewSeverity.CRITICAL:
            return "text-error-500 dark:text-error-400";
        case ReviewSeverity.MAJOR:
            return "text-warning-500 dark:text-warning-400";
        case ReviewSeverity.MINOR:
            return "text-secondary-500 dark:text-secondary-400";
        case ReviewSeverity.INFO:
            return "text-info-500 dark:text-info-400";
    }
};

const getSeverityLabel = (severity: ReviewSeverity) => {
    return t(`views.adminfaced.review.severity.${severity}`);
};

const handleSelect = () => {
    if (props.isEditing) return;
    emit("select", props.entry);
};

const handleLocate = () => {
    emit("locate", props.entry);
};

const handleRemove = () => {
    showRemoveConfirm.value = false;
    emit("remove", props.index);
};

const handleEdit = () => {
    emit("edit", props.index);
};

const handleSave = () => {
    if (!editFormData.value.message.trim()) return;
    emit("save", props.index, {
        message: editFormData.value.message,
        suggestion: editFormData.value.suggestion,
        severity: editFormData.value.severity,
        category: editFormData.value.category
    });
};

const handleCancel = () => {
    emit("cancel", props.index);
};
</script>

<template>
    <ReviewEntryForm
            v-if="isEditing"
            :model-value="editFormData"
            :title="t('views.adminfaced.review.editFeedback')"
            :submit-label="t('views.adminfaced.review.reviewEntryUpdate')"
            :context-text="entry.originalText"
            class="border-primary-200 dark:border-primary-800 bg-neutral-100/40 dark:bg-neutral-800/40"
            @update:model-value="editFormData = $event"
            @submit="handleSave"
            @cancel="handleCancel"
    />

    <div v-else :class="[
        'group relative rounded-lg p-3 cursor-pointer transition-all duration-200',
        isSelected
            ? 'bg-primary-50/60 dark:bg-primary-900/15 ring-1 ring-primary-300 dark:ring-primary-700'
            : 'bg-neutral-100/40 dark:bg-neutral-800/40 hover:bg-neutral-100/80 dark:hover:bg-neutral-800/80'
    ]"
         @click="handleSelect">

        <div class="flex items-center justify-between mb-1.5">
            <div :class="`${getSeverityClass(entry.severity)}` "
                 class="flex items-center gap-2">
                <div class="flex items-center justify-center w-5 h-5">
                    <UIcon :name="getSeverityIcon(entry.severity)" class="w-4 h-4"/>
                </div>

                <span class="text-xs font-semibold">
                    {{ getSeverityLabel(entry.severity) }}
                </span>
            </div>

            <div :class="{ 'opacity-100': isSelected }"
                 class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                <UTooltip :text="t('views.adminfaced.review.reviewEntryEdit')">
                    <UButton
                            v-if="!entry.reviewerSource?.isAutomatic"
                            color="neutral"
                            icon="i-lucide-pencil"
                            size="xs"
                            variant="ghost"
                            class="hover:bg-neutral-300/40 dark:hover:bg-neutral-600/40"
                            @click.stop="handleEdit"
                    />
                </UTooltip>
                <UTooltip
                        :text="isSelected ? t('views.adminfaced.review.reviewEntrySelected') : t('views.adminfaced.review.reviewEntryGoTo')">
                    <UButton
                            :color="isSelected ? 'primary' : 'neutral'"
                            :variant="isSelected ? 'solid' : 'ghost'"
                            :class="!isSelected && 'hover:bg-neutral-300/40 dark:hover:bg-neutral-600/40'"
                            icon="i-lucide-locate-fixed"
                            size="xs"
                            @click.stop="handleLocate"
                    />
                </UTooltip>
                <UPopover v-if="!entry.reviewerSource?.isAutomatic" v-model:open="showRemoveConfirm">
                    <UTooltip :text="t('views.adminfaced.review.reviewEntryRemove')">
                        <UButton
                                color="error"
                                icon="i-lucide-trash-2"
                                size="xs"
                                variant="ghost"
                                class="hover:bg-neutral-300/40 dark:hover:bg-neutral-600/40"
                                @click.stop
                        />
                    </UTooltip>

                    <template #content>
                        <div class="p-3 space-y-3 min-w-48">
                            <p class="text-sm font-medium text-neutral-900 dark:text-white">
                                {{ t('views.adminfaced.review.reviewEntryRemoveConfirmTitle') }}
                            </p>
                            <p class="text-xs text-neutral-600 dark:text-neutral-400">
                                {{ t('views.adminfaced.review.reviewEntryRemoveConfirm') }}
                            </p>
                            <div class="flex justify-end gap-2">
                                <UButton
                                        color="neutral"
                                        size="xs"
                                        variant="ghost"
                                        @click="showRemoveConfirm = false">
                                    {{ t("common.cancel") }}
                                </UButton>
                                <UButton
                                        color="error"
                                        size="xs"
                                        @click="handleRemove">
                                    {{ t("common.remove") }}
                                </UButton>
                            </div>
                        </div>
                    </template>
                </UPopover>
            </div>
        </div>

        <div class="flex items-center gap-1.5 mb-2.5">
            <UBadge class="rounded font-mono text-[10px] px-1.5 py-0" color="info" size="xs" variant="soft">
                {{ t(`views.adminfaced.review.category.${entry.category}`) }}
            </UBadge>
            <UBadge v-if="entry.reviewerSource?.isAutomatic" class="rounded px-1.5 py-0" color="info" size="xs"
                    variant="soft">
                {{ t('views.adminfaced.review.reviewEntrySourceAuto') }}
            </UBadge>
        </div>

        <div class="mb-2.5">
            <p class="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed whitespace-pre-wrap wrap-break-word">
                {{ entry.message || t('views.adminfaced.review.entryUntitled') }}
            </p>
        </div>

        <div v-if="entry.suggestion"
             :class="['mb-2.5 p-2.5 rounded-md border', isSelected
             ? 'bg-primary-100/40 dark:bg-primary-900/30 border-primary-200 dark:border-primary-800'
             : 'bg-neutral-50 dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700']">
            <div class="flex items-center gap-1.5 mb-1">
                <UIcon class="w-3 h-3 text-neutral-500 dark:text-neutral-300" name="i-lucide-lightbulb"/>
                <span class="text-[10px] uppercase font-semibold text-neutral-500 dark:text-neutral-300 tracking-wider">
                    {{ t('views.adminfaced.review.reviewEntrySuggestion') }}
                </span>
            </div>
            <p class="text-xs text-neutral-700 dark:text-neutral-300 font-mono leading-relaxed whitespace-pre-wrap wrap-break-word">
                {{ entry.suggestion }}
            </p>
        </div>

        <div :class="{ 'border-primary-200 dark:border-primary-800/50': isSelected }"
             class="pt-2 border-t border-neutral-100 dark:border-neutral-800/80">
            <div class="flex items-center gap-1.5 text-neutral-400">
                <UIcon class="w-3 h-3 shrink-0" name="i-lucide-quote"/>
                <span :title="entry.originalText" class="text-[11px] italic font-mono truncate">
                    {{ entry.originalText }}
                </span>
            </div>
        </div>
    </div>
</template>
