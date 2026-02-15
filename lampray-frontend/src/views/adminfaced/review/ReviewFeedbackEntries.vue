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

const props = defineProps<{
    entries: LocalReviewEntry[];
    selectedEntry?: LocalReviewEntry | null;
}>();

const emit = defineEmits<{
    (e: "remove", index: number): void;
    (e: "locate", entry: ReviewFeedbackEntry): void;
    (e: "select", entry: ReviewFeedbackEntry): void;
}>();

const {t} = useI18n();

const getSeverityColor = (severity: string) => {
    switch (severity) {
        case "CRITICAL":
            return "error";
        case "MAJOR":
            return "warning";
        case "MINOR":
            return "primary";
        default:
            return "neutral";
    }
};

const items = computed(() => props.entries.map((entry, index) => ({
    label: entry.message,
    slot: `entry-${index}`,
    entry,
    index
})));
</script>

<template>
    <div class="space-y-4">
        <div class="flex items-center justify-between">
            <h3 class="text-xs font-semibold uppercase tracking-widest">
                {{ t("views.adminfaced.review.reviewEntries") }}
            </h3>
            <UBadge class="rounded-full" color="neutral" size="sm" variant="subtle">
                {{ entries.length }}
            </UBadge>
        </div>

        <slot name="default"/>
        <UEmpty v-if="entries.length === 0"
                :description="t('views.adminfaced.review.reviewEntriesEmpty')"
                :ui="{
                    root: 'border border-dashed border-neutral-200 dark:border-neutral-800 rounded-lg py-12 flex flex-col items-center justify-center gap-2 bg-neutral-50/50 dark:bg-neutral-900/20',
                }"
                icon="i-lucide-file"
                variant="naked"
        />
        <UAccordion
                v-else
                :items="items"
                :ui="{
                    root: 'space-y-2',
                    item: 'border border-neutral-200 dark:border-neutral-800 rounded-lg overflow-hidden shadow-none transition-colors',
                    header: 'px-3 py-2.5 hover:bg-neutral-50 dark:hover:bg-neutral-800/50 cursor-pointer select-none flex items-center gap-2',
                    trigger: 'flex items-center gap-3 w-full text-left',
                    default: 'text-sm font-medium text-neutral-900 dark:text-neutral-100 truncate flex-1',
                    content: 'px-3 pb-3 border-t border-neutral-100 dark:border-neutral-800 pt-3'
                }"
                multiple
        >
            <template #leading="{ item }">
                <div :class="`bg-${getSeverityColor(item.entry.severity)}-500`" class="size-2 rounded-full shrink-0"/>
            </template>

            <template #default="{ item }">
                <div
                    class="text-sm font-medium text-neutral-900 dark:text-neutral-100 truncate flex-1 pr-4"
                    @click="emit('select', item.entry)"
                >
                    {{ item.label || t("views.adminfaced.review.entryUntitled") }}
                </div>
            </template>

            <template v-for="(entry, index) in entries" :key="index" #[`entry-${index}`]>
                <div class="space-y-3">
                    <div class="flex flex-wrap gap-2">
                        <UBadge class="font-mono rounded-md" color="neutral" size="xs" variant="soft">
                            {{ entry.category }}
                        </UBadge>
                        <UBadge v-if="entry.reviewerSource.isAutomatic" class="rounded-md" color="info" size="xs"
                                variant="soft">
                            {{ t("views.adminfaced.review.reviewEntrySourceAuto") }}
                        </UBadge>
                    </div>

                    <p class="text-xs text-neutral-600 dark:text-neutral-400 leading-relaxed wrap-break-word whitespace-pre-wrap font-sans">
                        {{ entry.message }}
                    </p>

                    <div v-if="entry.suggestion"
                         class="bg-neutral-50 dark:bg-neutral-800/50 p-3 rounded-md border border-neutral-200 dark:border-neutral-700/50">
                        <div class="text-[10px] uppercase font-bold text-neutral-500 mb-1 tracking-wider">
                            {{ t("views.adminfaced.review.reviewEntrySuggestion") }}
                        </div>
                        <p class="text-xs text-neutral-800 dark:text-neutral-200 font-mono">
                            {{ entry.suggestion }}
                        </p>
                    </div>

                    <div class="flex items-center justify-between pt-2 border-t border-neutral-100 dark:border-neutral-800">
                        <div class="min-w-0 flex-1">
                            <div class="text-[10px] text-neutral-400 uppercase font-mono mb-0.5">
                                {{ t("views.adminfaced.review.reviewEntryContextLabel") }}
                            </div>
                            <div class="text-[11px] text-neutral-500 italic font-mono truncate" :title="entry.originalText">
                                "{{ entry.originalText }}"
                            </div>
                        </div>

                        <div class="flex items-center gap-1">
                            <UTooltip :text="entry === props.selectedEntry ? t('views.adminfaced.review.reviewEntrySelected') : t('views.adminfaced.review.reviewEntryGoTo')">
                                <UButton
                                        :ui="{ rounded: 'rounded-md' }"
                                        :color="entry === props.selectedEntry ? 'primary' : 'neutral'"
                                        :variant="entry === props.selectedEntry ? 'solid' : 'ghost'"
                                        icon="i-lucide-locate-fixed"
                                        size="xs"
                                        @click="emit('locate', entry)"
                                />
                            </UTooltip>
                            <UTooltip :text="t('views.adminfaced.review.reviewEntryRemove')">
                                <UButton
                                        v-if="!entry.reviewerSource.isAutomatic"
                                        :ui="{ rounded: 'rounded-md' }"
                                        color="error"
                                        icon="i-lucide-trash-2"
                                        size="xs"
                                        variant="ghost"
                                        @click="emit('remove', index)"
                                />
                            </UTooltip>
                        </div>
                    </div>
                </div>
            </template>
        </UAccordion>
    </div>
</template>
