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

const props = defineProps<{
    entries: ReviewFeedbackEntry[];
}>();

const emit = defineEmits<{
    (e: 'remove', index: number): void;
    (e: 'locate', entry: ReviewFeedbackEntry): void;
}>();

const {t} = useI18n();

const getSeverityColor = (severity: string) => {
    switch (severity) {
        case 'CRITICAL':
            return 'error';
        case 'MAJOR':
            return 'warning';
        case 'MINOR':
            return 'primary';
        default:
            return 'neutral';
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
        <div class="flex items-center justify-between px-2">
            <h3 class="text-sm font-semibold text-neutral-900 dark:text-neutral-100 uppercase tracking-widest">
                {{ t('views.adminfaced.review.reviewEntries') }}
            </h3>
            <UBadge class="rounded-full px-2" color="neutral" size="sm" variant="subtle">
                {{ entries.length }}
            </UBadge>
        </div>

        <slot name="default"/>

        <div v-if="entries.length === 0"
             class="border border-dashed border-neutral-200 dark:border-neutral-800 rounded-lg py-12 flex flex-col items-center justify-center gap-2">
            <UIcon class="size-6 text-neutral-300" name="i-lucide-message-square-off"/>
            <span class="text-xs text-neutral-400 font-medium italic">
                {{ t('views.adminfaced.review.reviewEntriesEmpty') }}
            </span>
        </div>

        <UAccordion
                v-else
                :items="items"
                :ui="{
                root: 'space-y-3',
                item: 'border border-neutral-200 dark:border-neutral-800 rounded-xl bg-white dark:bg-neutral-900 overflow-hidden transition-all duration-200 shadow-none',
                header: 'px-4 py-3.5 hover:bg-neutral-50 dark:hover:bg-neutral-800/50 cursor-pointer select-none',
                trigger: 'flex items-center gap-3 w-full text-left',
                content: 'px-4 pb-4 border-t border-neutral-100 dark:border-neutral-800 pt-4',
                label: 'text-sm font-semibold text-neutral-800 dark:text-neutral-200 truncate pr-4'
            }"
                multiple
        >
            <template #leading="{ item }">
                <UBadge
                        :color="getSeverityColor(item.entry.severity)"
                        class="rounded-full size-2 p-0 min-w-0"
                        size="xs"
                        variant="solid"
                />
            </template>
            <template v-for="(entry, index) in entries" :key="index" #[`entry-${index}`]>
                <div class="space-y-4">
                    <div class="flex flex-wrap gap-2">
                        <UBadge class="font-mono" color="neutral" size="xs" variant="subtle">
                            {{ entry.category }}
                        </UBadge>
                        <UBadge v-if="entry.reviewerSource.isAutomatic" color="info" size="xs" variant="subtle">
                            {{ t('views.adminfaced.review.auto') }}
                        </UBadge>
                    </div>

                    <p class="text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed wrap-break-word whitespace-pre-wrap">
                        {{ entry.message }}
                    </p>

                    <div v-if="entry.suggestion"
                         class="bg-primary-50 dark:bg-primary-900/10 p-3 rounded border-l-2 border-primary-500">
                        <div class="text-[10px] uppercase font-bold text-primary-600 dark:text-primary-400 mb-1 tracking-tighter">
                            {{ t('views.adminfaced.review.suggestion') }}
                        </div>
                        <p class="text-sm text-neutral-700 dark:text-neutral-300 italic">
                            {{ entry.suggestion }}
                        </p>
                    </div>

                    <div class="flex items-center justify-between pt-2 border-t border-neutral-50 dark:border-neutral-800/50">
                        <span class="text-[10px] text-neutral-400 font-mono uppercase">
                            LOC: {{ entry.locationRange?.startInNode ?? 'N/A' }}
                        </span>
                        <div class="flex items-center gap-1">
                            <UButton
                                    color="neutral"
                                    icon="i-lucide-locate"
                                    size="xs"
                                    variant="ghost"
                                    @click="emit('locate', entry)"
                            />
                            <UButton
                                    v-if="!entry.reviewerSource.isAutomatic"
                                    color="error"
                                    icon="i-lucide-trash-2"
                                    size="xs"
                                    variant="ghost"
                                    @click="emit('remove', index)"
                            />
                        </div>
                    </div>
                </div>
            </template>
        </UAccordion>
    </div>
</template>
