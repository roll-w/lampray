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
import {computed, nextTick, ref, watch} from "vue";
import type {ContentLocationRange, ReviewJobContentView} from "@/services/content/review.type";
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import type {StructuralText} from "@/components/structuraltext/types.ts";
import {useI18n} from "vue-i18n";
import {getContentTypeI18nKey} from "@/services/content/content.type.ts";
import type {Editor} from "@tiptap/vue-3";

const props = defineProps<{
    job: ReviewJobContentView;
}>();

const emit = defineEmits<{
    (e: "select-range", range: ContentLocationRange, text: string): void;
}>();

const {t} = useI18n();
const contentRef = ref<HTMLElement | null>(null);
const showFloatingButton = ref(false); // Can keep for v-if if needed, or rely on slot behavior? StructuralTextEditor slot visibility is usually handled by parent or editor state.
const selectedText = ref("");
const currentLocation = ref<ContentLocationRange | null>(null);

type TextSegment = {
    path: string;
    length: number;
}

const buildTextSegments = (node: StructuralText, currentPath: string, segments: TextSegment[]) => {
    if (node.content.length > 0) {
        segments.push({path: `${currentPath}.content`, length: node.content.length});
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
            merged.push({...segment});
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

const contentTypeDisplay = computed(() => {
    if (!props.job) return "";
    return t(getContentTypeI18nKey(props.job.contentType));
});

const handleSelection = (editor: Editor) => {
    const {state, view} = editor;
    const {selection} = state;
    if (selection.empty) {
        showFloatingButton.value = false;
        return;
    }

    const text = state.doc.textBetween(selection.from, selection.to, " ");
    if (!text || !text.trim()) {
        showFloatingButton.value = false;
        return;
    }

    const domSelection = window.getSelection();
    if (!domSelection || domSelection.rangeCount === 0) return;

    const range = domSelection.getRangeAt(0);
    const startNode = range.startContainer;
    const endNode = range.endContainer;

    const startElement = (startNode.nodeType === Node.TEXT_NODE ? startNode.parentElement : startNode) as HTMLElement;
    const endElement = (endNode.nodeType === Node.TEXT_NODE ? endNode.parentElement : endNode) as HTMLElement;

    const startPath = startElement?.closest('[data-review-path]')?.getAttribute('data-review-path');
    const endPath = endElement?.closest('[data-review-path]')?.getAttribute('data-review-path');

    const location: ContentLocationRange = {
        startInNode: range.startOffset,
        endInNode: range.endOffset,
        startPath: startPath || undefined,
        endPath: endPath || undefined
    };

    selectedText.value = text;
    currentLocation.value = location;
};

const confirmSelection = () => {
    console.log("Confirming selection:", currentLocation.value, selectedText.value);
    if (currentLocation.value) {
        emit("select-range", currentLocation.value, selectedText.value);
        // Do NOT clear selection here, so the user can see what they selected while filling the form.
        // The form in sidebar will show the context.
    }
};

const scrollToPath = (path: string) => {
    if (!contentRef.value) {
        console.warn("ReviewContent: contentRef is null");
        return;
    }
    const target = contentRef.value.querySelector(`[data-review-path="${path}"]`) as HTMLElement;
    if (target) {
        target.scrollIntoView({behavior: "smooth", block: "center"});
        target.classList.add("review-highlight");
        setTimeout(() => target.classList.remove("review-highlight"), 2000);
    } else {
        console.warn(`ReviewContent: Path not found: ${path}`);
    }
};

defineExpose({scrollToPath});

watch(() => props.job, async () => {
    await nextTick();
    normalizeEditorPaths();
}, {immediate: true});
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

        <div ref="contentRef" class="min-h-[400px]">
            <StructuralTextEditor
                    :editable="false"
                    :model-value="job.content"
                    :show-outline="false"
                    :show-toolbar="false"
                    :ui="{ content: { root: 'prose prose-neutral dark:prose-invert max-w-none' } }"
                    @selection-range="handleSelection"
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
