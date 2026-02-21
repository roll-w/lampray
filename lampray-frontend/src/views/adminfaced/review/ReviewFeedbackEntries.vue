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
import type {ReviewFeedbackEntry} from "@/services/content/review.type";
import {useI18n} from "vue-i18n";
import {computed} from "vue";
import type {LocalReviewEntry} from "@/views/adminfaced/review/reviewQueueContext.ts";
import ReviewFeedbackEntryItem from "@/views/adminfaced/review/ReviewFeedbackEntryItem.vue";

const props = defineProps<{
    entries: LocalReviewEntry[];
    selectedEntry?: LocalReviewEntry | null;
    editingIndex?: number;
}>();

const emit = defineEmits<{
    (e: "remove", index: number): void;
    (e: "locate", entry: ReviewFeedbackEntry): void;
    (e: "select", entry: ReviewFeedbackEntry): void;
    (e: "edit", index: number): void;
    (e: "save", index: number, data: Partial<LocalReviewEntry>): void;
    (e: "cancel", index: number): void;
}>();

const {t} = useI18n();

const count = computed(() => props.entries.length);

const isEntrySelected = (entry: LocalReviewEntry) => {
    return props.selectedEntry === entry;
};

const isEntryEditing = (index: number) => {
    return props.editingIndex === index;
};

const handleSelect = (entry: LocalReviewEntry) => {
    emit("select", entry);
};

const handleLocate = (entry: LocalReviewEntry) => {
    emit("locate", entry);
};

const handleRemove = (index: number) => {
    emit("remove", index);
};

const handleEdit = (index: number) => {
    emit("edit", index);
};

const handleSave = (index: number, data: Partial<LocalReviewEntry>) => {
    emit("save", index, data);
};

const handleCancel = (index: number) => {
    emit("cancel", index);
};
</script>

<template>
    <div class="space-y-3">
        <div class="flex items-center justify-between">
            <h3 class="text-xs font-semibold uppercase tracking-widest">
                {{ t("views.adminfaced.review.reviewEntries") }}
            </h3>
            <UBadge class="rounded-full" color="neutral" size="sm" variant="subtle">
                {{ count }}
            </UBadge>
        </div>

        <slot name="default"/>

        <UEmpty v-if="count === 0"
                :description="t('views.adminfaced.review.reviewEntriesEmpty')"
                :ui="{
                    root: 'border border-dashed border-neutral-200 dark:border-neutral-800 rounded-lg py-12 flex flex-col items-center justify-center gap-2 bg-neutral-50/50 dark:bg-neutral-900/20',
                }"
                icon="i-lucide-file"
                variant="naked"
        />

        <div v-else class="flex flex-col gap-1.5">
            <ReviewFeedbackEntryItem
                    v-for="(entry, index) in entries"
                    :key="index"
                    :entry="entry"
                    :index="index"
                    :is-selected="isEntrySelected(entry)"
                    :is-editing="isEntryEditing(index)"
                    @select="handleSelect"
                    @locate="handleLocate"
                    @remove="handleRemove"
                    @edit="handleEdit"
                    @save="handleSave"
                    @cancel="handleCancel"
            />
        </div>
    </div>
</template>
