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
import { nextTick, ref, onMounted, watch } from "vue";
import { autoUpdate, offset, shift, useFloating } from "@floating-ui/vue";
import type { ReviewJobContentView } from "@/services/content/review.type";
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import type { StructuralText } from "@/components/structuraltext/types.ts";
import ReviewEntryForm from "./ReviewEntryForm.vue";
import { useI18n } from "vue-i18n";

/**
 * @author RollW
 */

const props = defineProps<{
    job: ReviewJobContentView;
    loading?: boolean;
}>();

const emit = defineEmits<{
    (e: 'submit', entry: any): void;
}>();

const { t } = useI18n();
const contentRef = ref<HTMLElement | null>(null);
const floatingReference = ref<{ getBoundingClientRect: () => DOMRect } | null>(null);
const showSelectionPopover = ref(false);
const selectedText = ref("");
const currentLocation = ref<any>(null);

const { floatingStyles, update: updateFloating } = useFloating(floatingReference, ref(null), {
    placement: "top",
    middleware: [offset(8), shift({ padding: 12 })],
    whileElementsMounted: (referenceEl, floatingEl, update) => autoUpdate(referenceEl, floatingEl, update)
});

type TextSegment = {
    path: string;
    length: number;
}

const buildTextSegments = (node: StructuralText, currentPath: string, segments: TextSegment[]) => {
    if (node.content.length > 0) {
        segments.push({ path: `${currentPath}.content`, length: node.content.length });
    }
    if (node.children && node.children.length > 0) {
        node.children.forEach((child, index) => {
            buildTextSegments(child as StructuralText, `${currentPath}.children[${index}]`, segments);
        });
    }
};

const mergeTextSegments = (segments: TextSegment[]) => {
    const merged: TextSegment[] = [];
    segments.forEach(segment => {
        const last = merged[merged.length - 1];
        if (last && last.path === segment.path) {
            last.length += segment.length;
        } else {
            merged.push({ ...segment });
        }
    });
    return merged;
};

const normalizeEditorPaths = () => {
    const container = contentRef.value;
    if (!container || !props.job) return;

    const editorRoot = container.querySelector(".ProseMirror");
    if (!editorRoot) return;

    // Clear existing
    editorRoot.querySelectorAll("[data-review-path]").forEach((node: HTMLElement) => {
        node.removeAttribute("data-review-path");
    });

    const segments: TextSegment[] = [];
    buildTextSegments(props.job.content, "$.content", segments);
    const mergedSegments = mergeTextSegments(segments);
    if (mergedSegments.length === 0) return;

    const walker = document.createTreeWalker(editorRoot, NodeFilter.SHOW_TEXT, {
        acceptNode: node => {
            if (!node.textContent || node.textContent.length === 0) return NodeFilter.FILTER_REJECT;
            return NodeFilter.FILTER_ACCEPT;
        }
    });

    let segmentIndex = 0;
    let remaining = mergedSegments[segmentIndex]?.length ?? 0;
    let currentNode: Node | null = walker.nextNode();

    while (currentNode && segmentIndex < mergedSegments.length) {
        const textNode = currentNode as Text;
        const textLength = textNode.textContent?.length ?? 0;

        const segment = mergedSegments[segmentIndex];
        if (!segment) break;

        const element = textNode.parentElement;
        if (element && !element.hasAttribute("data-review-path")) {
            element.setAttribute("data-review-path", segment.path);
        }

        remaining -= textLength;
        if (remaining <= 0 && segmentIndex < mergedSegments.length - 1) {
            segmentIndex += 1;
            remaining = mergedSegments[segmentIndex]?.length ?? 0;
        }
        currentNode = walker.nextNode();
    }
};

const handleSelection = async () => {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
        showSelectionPopover.value = false;
        return;
    }

    const range = selection.getRangeAt(0);
    if (!contentRef.value?.contains(range.commonAncestorContainer)) {
        showSelectionPopover.value = false;
        return;
    }

    const text = selection.toString().trim();
    if (!text) {
        showSelectionPopover.value = false;
        return;
    }

    // Extract location info
    const startNode = range.startContainer;
    const endNode = range.endContainer;

    const startElement = (startNode.nodeType === Node.TEXT_NODE ? startNode.parentElement : startNode) as HTMLElement;
    const endElement = (endNode.nodeType === Node.TEXT_NODE ? endNode.parentElement : endNode) as HTMLElement;

    const startPath = startElement?.closest('[data-review-path]')?.getAttribute('data-review-path');
    const endPath = endElement?.closest('[data-review-path]')?.getAttribute('data-review-path');

    const location = {
        startPath,
        endPath,
        startInNode: range.startOffset,
        endInNode: range.endOffset,
        text
    };

    selectedText.value = text;
    floatingReference.value = { getBoundingClientRect: () => range.getBoundingClientRect() };
    showSelectionPopover.value = true;

    // Store current location for confirmSelection
    currentLocation.value = location;

    await nextTick();
    updateFloating();
};

const confirmSelection = () => {
    // Show entry form instead of just emitting select
    showEntryForm.value = true;
};

const showEntryForm = ref(false);

const handleEntrySubmit = (entry: any) => {
    emit('submit', entry);
    showEntryForm.value = false;
    showSelectionPopover.value = false;
    window.getSelection()?.removeAllRanges();
};

const handleCancel = () => {
    showEntryForm.value = false;
};

const scrollToPath = (path: string) => {
    if (!contentRef.value) return;
    const target = contentRef.value.querySelector(`[data-review-path="${path}"]`) as HTMLElement;
    if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "center" });
        target.classList.add("review-highlight");
        setTimeout(() => target.classList.remove("review-highlight"), 2000);
    }
};

defineExpose({ scrollToPath });

watch(() => props.job, async () => {
    await nextTick();
    normalizeEditorPaths();
}, { immediate: true });
</script>

<template>
    <div class="relative bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-lg overflow-hidden transition-colors duration-200">
        <div class="px-8 py-6 border-b border-neutral-100 dark:border-neutral-800">
            <div class="flex items-center gap-2 mb-4">
                <UBadge color="neutral" variant="subtle" size="sm" class="font-mono tracking-tight">
                    #{{ job.contentId }}
                </UBadge>
                <span class="text-xs text-neutral-400 font-medium uppercase tracking-widest">
                    {{ new Date(job.createTime).toLocaleDateString() }}
                </span>
            </div>
            <h1 v-if="job.title" class="text-3xl font-bold text-neutral-900 dark:text-white tracking-tight leading-tight">
                {{ job.title }}
            </h1>
        </div>

        <div v-if="loading" class="flex flex-col items-center justify-center py-20 gap-3">
            <UIcon name="i-lucide-loader-2" class="size-8 animate-spin text-neutral-300" />
            <span class="text-sm text-neutral-400 animate-pulse font-medium tracking-wide">
                {{ t('views.adminfaced.review.loadingContent') }}
            </span>
        </div>

        <div v-else ref="contentRef"
             class="px-8 py-10 min-h-[400px] selection:bg-primary-100 selection:text-primary-900 dark:selection:bg-primary-900/30 dark:selection:text-primary-100"
             @mouseup="handleSelection"
             @keyup="handleSelection">
            <StructuralTextEditor
                :editable="false"
                :model-value="job.content"
                :show-outline="false"
                :show-toolbar="false"
                :ui="{ content: { root: 'prose prose-neutral dark:prose-invert max-w-none' } }"
            />
        </div>

        <!-- Selection Bubble -->
        <div v-if="showSelectionPopover"
             :style="floatingStyles"
             class="z-50">
             <UPopover v-model:open="showEntryForm" :popper="{ placement: 'bottom', offset: 12 }">
                <div class="bg-neutral-900 dark:bg-white text-white dark:text-neutral-900 px-1 py-1 rounded-full shadow-xl flex items-center gap-1">
                    <UButton
                        icon="i-lucide-plus"
                        size="xs"
                        variant="ghost"
                        color="neutral"
                        class="rounded-full hover:bg-neutral-800 dark:hover:bg-neutral-100"
                        @click="confirmSelection"
                    >
                        {{ t('views.adminfaced.review.addFeedback') }}
                    </UButton>
                    <div class="w-px h-4 bg-neutral-700 dark:bg-neutral-200 mx-1" />
                    <UButton
                        icon="i-lucide-x"
                        size="xs"
                        variant="ghost"
                        color="neutral"
                        class="rounded-full hover:bg-neutral-800 dark:hover:bg-neutral-100"
                        @click="showSelectionPopover = false"
                    />
                </div>

                <template #panel>
                    <div class="w-80 shadow-2xl rounded-2xl border border-neutral-200 dark:border-neutral-800 overflow-hidden bg-white dark:bg-neutral-950">
                        <ReviewEntryForm
                            :initial-message="selectedText"
                            :location="currentLocation"
                            @submit="handleEntrySubmit"
                            @cancel="handleCancel"
                        />
                    </div>
                </template>
             </UPopover>
        </div>
    </div>
</template>

<style scoped>
:deep(.prose) {
    --tw-prose-body: var(--neutral-700);
    --tw-prose-headings: var(--neutral-900);
}

.dark :deep(.prose) {
    --tw-prose-body: var(--neutral-300);
    --tw-prose-headings: var(--neutral-50);
}
</style>
