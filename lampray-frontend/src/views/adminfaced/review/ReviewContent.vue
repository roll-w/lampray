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
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {useI18n} from "vue-i18n";
import {getContentTypeI18nKey} from "@/services/content/content.type.ts";
import type {Editor} from "@tiptap/vue-3";
import type {ReviewFeedbackEntry} from "@/services/content/review.type";
import {useReviewQueueActions, useReviewQueueState} from "./reviewQueueContext.ts";
import type {ContentLocationRange} from "@/components/structuraltext/types.ts";

const {t} = useI18n();
const contentRef = ref<any>(null);
const showFloatingButton = ref(false); 
const selectedText = ref("");
const currentLocation = ref<ContentLocationRange | null>(null);

const {jobContent, entries, selectedEntry} = useReviewQueueState();
const {setDraftFromSelection, clearSelection} = useReviewQueueActions();

// Active entry to highlight
const activeEntry = ref<ReviewFeedbackEntry | null>(null);

watch(() => entries.value, (newEntries) => {
    if (!newEntries) return;
    if (selectedEntry.value && !newEntries.some(e => e === selectedEntry.value)) {
        activeEntry.value = null;
        clearSelection();
        return;
    }
    if (activeEntry.value && !newEntries.some(e => e === activeEntry.value)) {
        activeEntry.value = null;
    }
}, { deep: true });

watch(() => selectedEntry.value, (entry) => {
    if (!entry) {
        activeEntry.value = null;
        return;
    }
    scrollToEntry(entry);
});

const contentTypeDisplay = computed(() => {
    if (!jobContent.value) return "";
    return t(getContentTypeI18nKey(jobContent.value.contentType));
});

const handleSelection = (editor: Editor) => {
    const {state} = editor;
    const {selection} = state;
    
    if (selection.empty) {
        showFloatingButton.value = false;
        return;
    }

    const doc = state.doc;
    const text = doc.textBetween(selection.from, selection.to, " ");
    if (!text || !text.trim()) {
        showFloatingButton.value = false;
        return;
    }

    if (!contentRef.value) return;

    const location = contentRef.value.getLocationFromSelection(selection);

    if (location) {
        selectedText.value = text;
        currentLocation.value = location;
        showFloatingButton.value = true;
    } else {
        showFloatingButton.value = false;
    }
};

const confirmSelection = () => {
    if (currentLocation.value) {
        setDraftFromSelection({
            range: currentLocation.value,
            text: selectedText.value
        });
    }
};

const scrollToPath = (path: string) => {
    if (contentRef.value) {
        contentRef.value.scrollToLocation({
            startPath: path,
            startInNode: 0,
            endInNode: 0
        });
    }
};

const scrollToEntry = (entry: ReviewFeedbackEntry) => {
    if (contentRef.value && entry.locationRange) {
        activeEntry.value = entry;
        contentRef.value.scrollToLocation(entry.locationRange);
    }
};

const clearHighlight = () => {
    activeEntry.value = null;
    clearSelection();
};

const highlights = computed(() => {
    if (!activeEntry.value || !activeEntry.value.locationRange) return [];
    return [{
        location: activeEntry.value.locationRange,
        info: activeEntry.value.message,
        severity: "active"
    }];
});

defineExpose({scrollToPath, scrollToEntry, clearHighlight});

</script>

<template>
    <div class="relative bg-white dark:bg-neutral-900 rounded-none overflow-visible">
        <div class="mb-8 border-b border-neutral-100 dark:border-neutral-800 pb-6">
            <div class="flex items-center gap-3 mb-4">
                <UBadge class="font-mono tracking-tight" color="primary" size="md" variant="soft">
                    {{ contentTypeDisplay }}
                </UBadge>
                <div class="w-px h-3 bg-neutral-200 dark:bg-neutral-800"/>
                <span class="text-sm text-neutral-400 font-medium uppercase tracking-widest">
                    {{ jobContent ? new Date(jobContent.createTime).toLocaleDateString() : "" }}
                </span>
            </div>
            <h1 v-if="jobContent?.title"
                class="text-4xl text-neutral-900 dark:text-white tracking-tight font-bold">
                {{ jobContent.title }}
            </h1>
        </div>

        <div class="min-h-[400px]">
            <StructuralTextEditor
                    ref="contentRef"
                    :editable="false"
                    :model-value="jobContent?.content"
                    :show-outline="false"
                    :show-toolbar="false"
                    :highlights="highlights"
                    :ui="{ content: { root: 'prose prose-neutral dark:prose-invert max-w-none' } }"
                    @select-range="handleSelection"
            >
                <template #bubble-menu-end>
                    <UButton
                            color="neutral"
                            size="xs"
                            variant="ghost"
                            @mousedown.prevent
                            @click.stop="confirmSelection"
                    >
                        <template #leading>
                            <UIcon class="size-3" name="i-lucide-message-square-plus"/>
                        </template>
                        {{ t("views.adminfaced.review.reviewEntryAdd") }}
                    </UButton>
                </template>
            </StructuralTextEditor>
        </div>
    </div>
</template>

<style scoped>
</style>
