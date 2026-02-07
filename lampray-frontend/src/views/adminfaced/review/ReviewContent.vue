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
import {computed, ref} from "vue";
import type {ContentLocationRange, ReviewJobContentView} from "@/services/content/review.type";
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {useI18n} from "vue-i18n";
import {getContentTypeI18nKey} from "@/services/content/content.type.ts";
import type {Editor} from "@tiptap/vue-3";
import type {ReviewFeedbackEntry} from "@/services/content/review.type";
import {ReviewSeverity} from "@/services/content/review.type";

const props = defineProps<{
    job: ReviewJobContentView;
    entries?: ReviewFeedbackEntry[];
}>();

const emit = defineEmits<{
    (e: "select-range", range: ContentLocationRange, text: string): void;
}>();

const {t} = useI18n();
const contentRef = ref<any>(null);
const showFloatingButton = ref(false); 
const selectedText = ref("");
const currentLocation = ref<ContentLocationRange | null>(null);

const contentTypeDisplay = computed(() => {
    if (!props.job) return "";
    return t(getContentTypeI18nKey(props.job.contentType));
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
    console.log("Confirming selection:", currentLocation.value, selectedText.value);
    if (currentLocation.value) {
        emit("select-range", currentLocation.value, selectedText.value);
    }
};

const scrollToPath = (path: string) => {
    if (contentRef.value) {
        contentRef.value.scrollToLocation({
            startPath: path,
            startInNode: 0,
            endInNode: 0 // Placeholder
        });
    }
};

const severityToHighlightSeverity = (severity: ReviewSeverity) => {
    switch (severity) {
        case ReviewSeverity.CRITICAL: return 'critical';
        case ReviewSeverity.MAJOR: return 'major';
        case ReviewSeverity.MINOR: return 'minor';
        case ReviewSeverity.INFO: return 'info';
        default: return 'info';
    }
};

const highlights = computed(() => {
    if (!props.entries) return [];
    return props.entries
        .filter(e => !!e.locationRange)
        .map(entry => ({
            location: entry.locationRange!,
            info: entry.message,
            severity: severityToHighlightSeverity(entry.severity)
        }));
});

defineExpose({scrollToPath});

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
                    {{ new Date(job.createTime).toLocaleDateString() }}
                </span>
            </div>
            <h1 v-if="job.title"
                class="text-4xl text-neutral-900 dark:text-white tracking-tight font-bold">
                {{ job.title }}
            </h1>
        </div>

        <div class="min-h-[400px]">
            <StructuralTextEditor
                    ref="contentRef"
                    :editable="false"
                    :model-value="job.content"
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
                        {{ t('views.adminfaced.review.reviewEntryAdd') }}
                    </UButton>
                </template>
            </StructuralTextEditor>
        </div>
    </div>
</template>
