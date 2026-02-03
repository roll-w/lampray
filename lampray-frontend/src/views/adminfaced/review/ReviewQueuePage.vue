<!--
  - Copyright (C) 2023-2025 RollW
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
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import {autoUpdate, offset, shift, useFloating} from "@floating-ui/vue";
import {reviewService} from "@/services/content/review.service";
import {
    ReviewCategory,
    type ReviewFeedbackEntry,
    type ReviewJobContentView,
    type ReviewJobDetailsView,
    type ReviewJobView,
    ReviewSeverity,
    ReviewStatus,
    type ReviewTaskView,
    ReviewVerdict
} from "@/services/content/review.type";
import {useAxios} from "@/composables/useAxios.ts";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {getContentTypeI18nKey} from "@/services/content/content.type.ts";
import type {StructuralText} from "@/components/structuraltext/types.ts";
import {useUserStore} from "@/stores/user.ts";

type ReviewEntryForm = {
    category: ReviewCategory;
    severity: ReviewSeverity;
    message: string;
    suggestion: string;
}

type TextSegment = {
    path: string;
    length: number;
}

const axios = useAxios();
const reviewApi = reviewService(axios);
const toast = useToast();
const {t} = useI18n();
const userStore = useUserStore();

const loading = ref(true);
const currentJobView = ref<ReviewJobContentView | null>(null);
const currentJobDetails = ref<ReviewJobDetailsView | null>(null);
const reviewQueue = ref<ReviewJobView[]>([]);
const reviewSummary = ref("");
const reviewEntries = ref<ReviewFeedbackEntry[]>([]);
const isSubmitting = ref(false);
const loadingContent = ref(false);

const contentRef = ref<HTMLElement | null>(null);
const reviewRootRef = ref<HTMLElement | null>(null);
const selectedText = ref("");
const selectionLocation = ref<{ start: number; end: number; path: string } | null>(null);
const showSelectionPopover = ref(false);
const textNodeMeta = ref<WeakMap<Text, { path: string; offsetBase: number }>>(new WeakMap());
const selectionEntryForm = ref<ReviewEntryForm>({
    category: ReviewCategory.CONTENT_QUALITY,
    severity: ReviewSeverity.MINOR,
    message: "",
    suggestion: ""
});

const entryForm = ref<ReviewEntryForm>({
    category: ReviewCategory.CONTENT_QUALITY,
    severity: ReviewSeverity.MINOR,
    message: "",
    suggestion: ""
});

const floatingReference = ref<{
    getBoundingClientRect: () => DOMRect
} | null>(null);
const floatingElement = ref<HTMLElement | null>(null);
const {floatingStyles, update: updateFloating} = useFloating(floatingReference, floatingElement, {
    placement: "bottom-start",
    middleware: [offset(10), shift({padding: 12})],
    whileElementsMounted: (referenceEl, floatingEl, update) => autoUpdate(referenceEl, floatingEl, update)
});

const currentIndex = ref(-1);
const hasCurrentJob = computed(() => currentJob.value !== null);
const currentUserId = computed(() => userStore.user?.id ?? null);

const currentJob = computed(() => {
    if (currentIndex.value < 0 || currentIndex.value >= reviewQueue.value.length) {
        return null;
    }
    return reviewQueue.value[currentIndex.value];
});

const queueProgress = computed(() => {
    if (reviewQueue.value.length === 0) return 0;
    return ((currentIndex.value + 1) / reviewQueue.value.length) * 100;
});

const contentTypeDisplay = computed(() => {
    if (!currentJob.value) return "";
    return t(getContentTypeI18nKey(currentJob.value.contentType));
});

const jobMarkDisplay = computed(() => {
    if (!currentJob.value) return "";
    if (!currentJob.value.reviewMark) return "";
    return t(`views.adminfaced.review.reviewMarks.${currentJob.value.reviewMark}`);
});

const reviewTask = computed<ReviewTaskView | null>(() => {
    if (!currentJobDetails.value) return null;
    const tasks = currentJobDetails.value.tasks || [];
    if (tasks.length === 0) return null;
    const userId = currentUserId.value;
    if (userId !== null) {
        const assignedTask = tasks.find(task => task.reviewerId === userId);
        if (assignedTask) return assignedTask;
    }
    const manualTask = tasks.find(task => !task.feedback?.entries?.some(entry => entry.reviewerSource.isAutomatic));
    return manualTask ?? tasks[0] ?? null;
});

const verdictOptions = computed(() => [
    {label: t("views.adminfaced.review.reviewVerdicts.APPROVED"), value: ReviewVerdict.APPROVED},
    {label: t("views.adminfaced.review.reviewVerdicts.NEEDS_REVISION"), value: ReviewVerdict.NEEDS_REVISION},
    {label: t("views.adminfaced.review.reviewVerdicts.REJECTED"), value: ReviewVerdict.REJECTED}
]);

const categoryOptions = computed(() => [
    {label: t("views.adminfaced.review.reviewCategories.CONTENT_QUALITY"), value: ReviewCategory.CONTENT_QUALITY},
    {label: t("views.adminfaced.review.reviewCategories.GRAMMAR"), value: ReviewCategory.GRAMMAR},
    {label: t("views.adminfaced.review.reviewCategories.FORMAT"), value: ReviewCategory.FORMAT},
    {label: t("views.adminfaced.review.reviewCategories.POLICY_VIOLATION"), value: ReviewCategory.POLICY_VIOLATION},
    {label: t("views.adminfaced.review.reviewCategories.SENSITIVE_CONTENT"), value: ReviewCategory.SENSITIVE_CONTENT},
    {label: t("views.adminfaced.review.reviewCategories.COPYRIGHT"), value: ReviewCategory.COPYRIGHT},
    {label: t("views.adminfaced.review.reviewCategories.TECHNICAL"), value: ReviewCategory.TECHNICAL},
    {label: t("views.adminfaced.review.reviewCategories.OTHER"), value: ReviewCategory.OTHER}
]);

const severityOptions = computed(() => [
    {label: t("views.adminfaced.review.reviewSeverities.CRITICAL"), value: ReviewSeverity.CRITICAL},
    {label: t("views.adminfaced.review.reviewSeverities.MAJOR"), value: ReviewSeverity.MAJOR},
    {label: t("views.adminfaced.review.reviewSeverities.MINOR"), value: ReviewSeverity.MINOR},
    {label: t("views.adminfaced.review.reviewSeverities.INFO"), value: ReviewSeverity.INFO}
]);

const verdict = ref<ReviewVerdict>(ReviewVerdict.APPROVED);

const sortedEntries = computed(() => {
    const entries = reviewEntries.value.map((entry, index) => ({entry, index}));
    return entries.sort((a, b) => {
        if (a.entry.reviewerSource.isAutomatic !== b.entry.reviewerSource.isAutomatic) {
            return a.entry.reviewerSource.isAutomatic ? 1 : -1;
        }
        return 0;
    });
});

const entriesCount = computed(() => reviewEntries.value.length);

const reviewActionDisabled = computed(() => {
    if (loadingContent.value || isSubmitting.value) return true;
    if (verdict.value === ReviewVerdict.APPROVED) return false;
    return reviewEntries.value.length === 0 && reviewSummary.value.trim().length === 0;
});

const entrySourceLabel = (entry: ReviewFeedbackEntry) => {
    return entry.reviewerSource.isAutomatic
            ? t("views.adminfaced.review.reviewEntrySourceAuto")
            : t("views.adminfaced.review.reviewEntrySourceManual");
};

const getEntryCategoryLabel = (entry: ReviewFeedbackEntry) => {
    return t(`views.adminfaced.review.reviewCategories.${entry.category}`);
};

const getEntrySeverityLabel = (entry: ReviewFeedbackEntry) => {
    return t(`views.adminfaced.review.reviewSeverities.${entry.severity}`);
};

const resetEntryForm = () => {
    entryForm.value = {
        category: ReviewCategory.CONTENT_QUALITY,
        severity: ReviewSeverity.MINOR,
        message: "",
        suggestion: ""
    };
};

const resetSelectionForm = () => {
    selectionEntryForm.value = {
        category: ReviewCategory.CONTENT_QUALITY,
        severity: ReviewSeverity.MINOR,
        message: "",
        suggestion: ""
    };
};

const clearSelectionState = () => {
    selectedText.value = "";
    selectionLocation.value = null;
    showSelectionPopover.value = false;
};

const findTextNode = (node: Node | null, preferEnd: boolean): Text | null => {
    if (!node) return null;
    if (node.nodeType === Node.TEXT_NODE) return node as Text;
    const children = Array.from(node.childNodes);
    if (preferEnd) {
        children.reverse();
    }
    for (const child of children) {
        const textNode = findTextNode(child, preferEnd);
        if (textNode) return textNode;
    }
    return null;
};

const getTextMeta = (node: Node, offset: number, preferEnd: boolean) => {
    const textNode = node.nodeType === Node.TEXT_NODE ? (node as Text) : findTextNode(node, preferEnd);
    if (!textNode) return null;
    const meta = textNodeMeta.value.get(textNode);
    if (!meta) return null;
    const localOffset = node.nodeType === Node.TEXT_NODE
            ? offset
            : (preferEnd ? textNode.textContent?.length ?? 0 : 0);
    return {meta, localOffset};
};

const extractSelectionLocation = (range: Range, container: HTMLElement) => {
    if (!container.contains(range.commonAncestorContainer)) return null;
    const startMeta = getTextMeta(range.startContainer, range.startOffset, false);
    const endMeta = getTextMeta(range.endContainer, range.endOffset, true);
    if (!startMeta || !endMeta) return null;
    if (startMeta.meta.path !== endMeta.meta.path) return null;
    const start = startMeta.meta.offsetBase + startMeta.localOffset;
    const end = endMeta.meta.offsetBase + endMeta.localOffset;
    if (start === end) return null;
    return {start, end, path: startMeta.meta.path};
};

const updateSelectionPopover = async () => {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) {
        clearSelectionState();
        return;
    }
    const range = selection.getRangeAt(0);
    if (range.collapsed || !contentRef.value) {
        clearSelectionState();
        return;
    }
    if (!contentRef.value.contains(range.commonAncestorContainer)) {
        clearSelectionState();
        return;
    }
    const text = selection.toString().trim();
    if (!text) {
        clearSelectionState();
        return;
    }
    const location = extractSelectionLocation(range, contentRef.value);
    if (!location) {
        clearSelectionState();
        return;
    }
    selectedText.value = text;
    selectionLocation.value = location;
    floatingReference.value = {
        getBoundingClientRect: () => range.getBoundingClientRect()
    };
    showSelectionPopover.value = true;
    await nextTick();
    updateFloating();
};

const handleSelection = async () => {
    await updateSelectionPopover();
};

const addEntryFromSelection = () => {
    if (!selectionLocation.value || !selectedText.value) {
        toast.add(newErrorToastFromError(
                new Error(t("views.adminfaced.review.reviewEntryAddDisabled")),
                t("views.adminfaced.review.validation.title")
        ));
        return;
    }

    const newEntry: ReviewFeedbackEntry = {
        category: selectionEntryForm.value.category,
        severity: selectionEntryForm.value.severity,
        message: selectionEntryForm.value.message.trim() || selectedText.value,
        suggestion: selectionEntryForm.value.suggestion.trim() || undefined,
        locationRange: {
            startInNode: selectionLocation.value.start,
            endInNode: selectionLocation.value.end,
            startPath: selectionLocation.value.path,
            endPath: selectionLocation.value.path
        },
        reviewerSource: {
            isAutomatic: false,
            reviewerName: "manual"
        }
    };

    reviewEntries.value.unshift(newEntry);
    clearSelectionState();
    resetSelectionForm();
    window.getSelection()?.removeAllRanges();
};

const addManualEntry = () => {
    if (!entryForm.value.message.trim()) {
        toast.add(newErrorToastFromError(
                new Error(t("views.adminfaced.review.reviewEntryMessageRequired")),
                t("views.adminfaced.review.validation.title")
        ));
        return;
    }

    const newEntry: ReviewFeedbackEntry = {
        category: entryForm.value.category,
        severity: entryForm.value.severity,
        message: entryForm.value.message.trim(),
        suggestion: entryForm.value.suggestion.trim() || undefined,
        reviewerSource: {
            isAutomatic: false,
            reviewerName: "manual"
        }
    };
    reviewEntries.value.unshift(newEntry);
    resetEntryForm();
};

const removeEntryAtIndex = (index: number) => {
    reviewEntries.value.splice(index, 1);
};

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

const loadExistingEntries = () => {
    if (!currentJobDetails.value) {
        reviewEntries.value = [];
        return;
    }
    const entries: ReviewFeedbackEntry[] = [];
    currentJobDetails.value.tasks.forEach(task => {
        if (task.feedback?.entries?.length) {
            entries.push(...task.feedback.entries);
        }
    });
    reviewEntries.value = entries.map(entry => ({
        ...entry,
        reviewerSource: entry.reviewerSource || {isAutomatic: false, reviewerName: "manual"}
    }));
};

const normalizeEditorPaths = () => {
    const container = contentRef.value;
    if (!container || !currentJobView.value) return;
    container.querySelectorAll("[data-review-path]").forEach(node => {
        node.removeAttribute("data-review-path");
    });
    const editorRoot = container.querySelector(".ProseMirror");
    if (!editorRoot) return;
    const segments: TextSegment[] = [];
    buildTextSegments(currentJobView.value.content, "$.content", segments);
    const mergedSegments = mergeTextSegments(segments);
    if (mergedSegments.length === 0) return;
    const walker = document.createTreeWalker(editorRoot, NodeFilter.SHOW_TEXT, {
        acceptNode: node => {
            if (!node.textContent || node.textContent.length === 0) return NodeFilter.FILTER_REJECT;
            return NodeFilter.FILTER_ACCEPT;
        }
    });
    textNodeMeta.value = new WeakMap();
    let segmentIndex = 0;
    let remaining = mergedSegments[segmentIndex]?.length ?? 0;
    let offsetBase = 0;
    let currentNode: Node | null = walker.nextNode();
    while (currentNode && segmentIndex < mergedSegments.length) {
        const textNode = currentNode as Text;
        const textLength = textNode.textContent?.length ?? 0;
        while (remaining <= 0 && segmentIndex < mergedSegments.length - 1) {
            segmentIndex += 1;
            remaining = mergedSegments[segmentIndex]?.length ?? 0;
            offsetBase = 0;
        }
        const segment = mergedSegments[segmentIndex];
        if (!segment) break;
        const element = textNode.parentElement;
        if (element && !element.hasAttribute("data-review-path")) {
            element.setAttribute("data-review-path", segment.path);
        }
        textNodeMeta.value.set(textNode, {path: segment.path, offsetBase});
        remaining -= textLength;
        offsetBase += textLength;
        if (remaining <= 0 && segmentIndex < mergedSegments.length - 1) {
            segmentIndex += 1;
            remaining = mergedSegments[segmentIndex]?.length ?? 0;
            offsetBase = 0;
        }
        currentNode = walker.nextNode();
    }
};

const escapeAttributeValue = (value: string) => {
    let escaped = "";
    for (const char of value) {
        if (char === "\"" || char === "\\") {
            escaped += "\\";
        }
        escaped += char;
    }
    return escaped;
};

const locateNodeForPath = (path: string) => {
    const container = reviewRootRef.value || contentRef.value;
    if (!container) return null;
    const safePath = typeof CSS !== "undefined" && CSS.escape
            ? CSS.escape(path)
            : escapeAttributeValue(path);
    const target = container.querySelector(`[data-review-path="${safePath}"]`) as HTMLElement | null;
    return target;
};

const scrollToEntry = (entry: ReviewFeedbackEntry) => {
    const location = entry.locationRange;
    if (!location?.startPath) return;
    const target = locateNodeForPath(location.startPath);
    if (!target) return;
    target.scrollIntoView({behavior: "smooth", block: "center"});
    target.classList.add("review-highlight");
    setTimeout(() => {
        target.classList.remove("review-highlight");
    }, 1600);
};

const getLocationLabel = (entry: ReviewFeedbackEntry) => {
    const location = entry.locationRange;
    if (!location) return t("views.adminfaced.review.reviewEntryNoLocation");
    return t("views.adminfaced.review.reviewEntryRangeLabel", {
        start: location.startInNode,
        end: location.endInNode
    });
};

/**
 * Fetch review jobs from server
 */
const fetchReviewJobs = async () => {
    loading.value = true;
    reviewQueue.value = [];
    currentIndex.value = -1;
    currentJobView.value = null;
    currentJobDetails.value = null;
    reviewEntries.value = [];
    reviewSummary.value = "";

    try {
        const response = await reviewApi.getReviewJobs([ReviewStatus.PENDING]);
        reviewQueue.value = response.data.data || [];

        if (reviewQueue.value.length > 0) {
            currentIndex.value = 0;
            await loadJobContent(0);
        }
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loading.value = false;
    }
};

/**
 * Load job content by index
 */
const loadJobContent = async (index: number) => {
    if (index < 0 || index >= reviewQueue.value.length) {
        currentIndex.value = -1;
        currentJobView.value = null;
        currentJobDetails.value = null;
        return;
    }

    const job = reviewQueue.value[index];
    if (!job) return;

    loadingContent.value = true;
    currentIndex.value = index;
    try {
        const [contentResponse, detailResponse] = await Promise.all([
            reviewApi.getReviewContent(job.id),
            reviewApi.getReviewJob(job.id)
        ]);
        currentJobView.value = contentResponse.data?.data || null;
        currentJobDetails.value = detailResponse.data.data || null;
        loadExistingEntries();
        reviewSummary.value = "";
        if (reviewEntries.value.some(entry => entry.reviewerSource.isAutomatic)) {
            verdict.value = ReviewVerdict.NEEDS_REVISION;
        } else {
            verdict.value = ReviewVerdict.APPROVED;
        }
        await nextTick();
        normalizeEditorPaths();
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loadingContent.value = false;
    }
};

/**
 * Submit review decision
 */
const submitReview = async () => {
    if (!currentJob.value || !reviewTask.value) return;

    if (reviewActionDisabled.value) {
        toast.add(newErrorToastFromError(
                new Error(t("views.adminfaced.review.validation.reasonRequired")),
                t("views.adminfaced.review.validation.title")
        ));
        return;
    }

    isSubmitting.value = true;
    try {
        await reviewApi.makeReview(currentJob.value.id, reviewTask.value.taskId, {
            verdict: verdict.value,
            entries: reviewEntries.value,
            summary: reviewSummary.value.trim() || undefined
        });

        toast.add(newSuccessToast(
                t("views.adminfaced.review.approveSuccess")
        ));

        // Remove current job from queue
        reviewQueue.value.splice(currentIndex.value, 1);

        // Load next job or complete
        if (reviewQueue.value.length === 0) {
            currentIndex.value = -1;
            currentJobView.value = null;
            currentJobDetails.value = null;
            toast.add(newSuccessToast(t("views.adminfaced.review.queueComplete")));
        } else {
            // Stay at same index (which now points to next job)
            const nextIndex = Math.min(currentIndex.value, reviewQueue.value.length - 1);
            await loadJobContent(nextIndex);
        }
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        isSubmitting.value = false;
    }
};

const nextJob = async () => {
    const nextIndex = currentIndex.value + 1;
    if (nextIndex < reviewQueue.value.length) {
        await loadJobContent(nextIndex);
    }
};

const prevJob = async () => {
    const prevIndex = currentIndex.value - 1;
    if (prevIndex >= 0) {
        await loadJobContent(prevIndex);
    }
};

/**
 * Refresh queue
 */
const refreshQueue = () => {
    fetchReviewJobs();
};

const handleEditorMouseUp = async () => {
    await handleSelection();
};

const handleEditorKeyUp = async () => {
    await handleSelection();
};

const handleEditorMouseDown = () => {
    clearSelectionState();
};

const handleSelectionCancel = () => {
    clearSelectionState();
    window.getSelection()?.removeAllRanges();
};

const handleDocumentClick = (event: MouseEvent) => {
    if (!showSelectionPopover.value) return;
    const target = event.target as Node | null;
    if (!floatingElement.value || !target) return;
    if (floatingElement.value.contains(target)) return;
    if (contentRef.value && contentRef.value.contains(target)) return;
    handleSelectionCancel();
};

const handleEditorScroll = () => {
    if (showSelectionPopover.value) {
        updateFloating();
    }
};

onMounted(() => {
    fetchReviewJobs();
    window.addEventListener("click", handleDocumentClick);
});

onBeforeUnmount(() => {
    window.removeEventListener("click", handleDocumentClick);
});
</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    <div class="flex flex-col">
                        <span class="text-lg font-medium">
                            {{ t("views.adminfaced.review.title") }}
                        </span>
                        <span class="text-sm text-neutral-500 font-normal mt-1">
                            {{ t("views.adminfaced.review.description") }}
                        </span>
                    </div>
                </template>
                <template #right>
                    <UButton
                            :loading="loading"
                            color="primary"
                            icon="i-lucide-refresh-cw"
                            variant="outline"
                            @click="refreshQueue"
                    >
                        {{ t("views.adminfaced.review.refresh") }}
                    </UButton>
                </template>
            </UDashboardNavbar>
        </template>

        <template #body>
            <div v-if="reviewQueue.length > 0" class="mb-6 px-4">
                <div class="flex items-center justify-between mb-2">
                    <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {{
                            t("views.adminfaced.review.progress", {
                                current: currentIndex + 1,
                                total: reviewQueue.length
                            })
                        }}
                    </span>
                    <span class="text-sm text-gray-500 dark:text-gray-400">
                        {{ Math.round(queueProgress) }}%
                    </span>
                </div>
                <UProgress :model-value="queueProgress"/>
            </div>

            <div v-if="loading" class="flex items-center justify-center py-12">
                <div class="text-center flex flex-col items-center">
                    <UIcon class="w-8 h-8 animate-spin text-primary mb-2"
                           name="i-lucide-loader-2"/>
                    <p class="text-gray-500 dark:text-gray-400">
                        {{ t("views.adminfaced.review.loading") }}
                    </p>
                </div>
            </div>

            <div v-else-if="!hasCurrentJob && !loading"
                 class="flex flex-col items-center justify-center py-12">
                <div class="flex flex-col items-center gap-2 py-4">
                    <div class="w-14 h-14 rounded-full bg-linear-to-br flex items-center justify-center">
                        <UIcon class="w-7 h-7 text-success" name="i-lucide-check-circle"/>
                    </div>
                    <h1 class="text-xl font-bold text-gray-900 dark:text-white">
                        {{ t("views.adminfaced.review.noPendingReviews") }}</h1>
                    <p class="text-normal text-gray-600 dark:text-gray-400">
                        {{ t("views.adminfaced.review.allCompleted") }}
                    </p>
                    <UButton class="mt-5" color="primary" @click="refreshQueue">
                        {{ t("views.adminfaced.review.checkForNew") }}
                    </UButton>
                </div>

            </div>

            <div v-else-if="hasCurrentJob" ref="reviewRootRef"
                 class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_340px] gap-6 px-6 pb-10">
                <div class="space-y-6">
                    <div class="rounded-[20px] border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950">
                        <div class="px-6 pt-6">
                            <div class="flex flex-col gap-2">
                                <span class="text-xs uppercase tracking-[0.2em] text-neutral-500">
                                    {{ t("views.adminfaced.review.contentToReview") }}
                                </span>
                                <div class="flex flex-wrap items-center gap-3 text-sm text-neutral-600 dark:text-neutral-300">
                                    <span class="font-medium text-neutral-900 dark:text-white">
                                        {{ currentJob!.id }}
                                    </span>
                                    <span>{{ contentTypeDisplay }}</span>
                                    <span>{{ t("views.adminfaced.review.reviewMark") }}: {{ jobMarkDisplay }}</span>
                                    <span>{{ t("views.adminfaced.review.contentId") }}: {{
                                            currentJob!.contentId
                                        }}</span>
                                </div>
                            </div>
                            <div v-if="currentJobView?.title" class="mt-4">
                                <h4 class="text-2xl font-semibold text-neutral-900 dark:text-white">
                                    {{ currentJobView.title }}
                                </h4>
                            </div>
                        </div>
                        <div class="mt-6 border-t border-neutral-200 dark:border-neutral-800">
                            <div v-if="loadingContent" class="flex items-center justify-center py-10">
                                <UIcon class="w-6 h-6 animate-spin text-neutral-500"
                                       name="i-lucide-loader-2"/>
                            </div>
                            <div v-else-if="currentJobView" class="relative">
                                <div ref="contentRef"
                                     class="review-content px-6 pb-8 max-h-[calc(100vh-260px)] overflow-y-auto"
                                     @keyup="handleEditorKeyUp"
                                     @mousedown="handleEditorMouseDown"
                                     @mouseup="handleEditorMouseUp"
                                     @scroll.passive="handleEditorScroll">
                                    <StructuralTextEditor
                                            :editable="false"
                                            :model-value="currentJobView!.content"
                                            :show-outline="false"
                                            :show-toolbar="false"
                                            :ui="{content: {root: 'w-full'}}">
                                    </StructuralTextEditor>
                                </div>
                                <div v-if="showSelectionPopover"
                                     ref="floatingElement"
                                     :style="floatingStyles"
                                     class="selection-popover">
                                    <div class="flex items-center justify-between gap-3">
                                        <div class="text-xs uppercase tracking-[0.16em] text-neutral-400">
                                            {{ t("views.adminfaced.review.selectionPopoverTitle") }}
                                        </div>
                                        <UButton
                                                color="neutral"
                                                size="xs"
                                                variant="ghost"
                                                @click="handleSelectionCancel">
                                            {{ t("views.adminfaced.review.selectionPopoverCancel") }}
                                        </UButton>
                                    </div>
                                    <p class="mt-2 text-sm text-neutral-700 dark:text-neutral-200 line-clamp-2">
                                        {{ selectedText || t("views.adminfaced.review.selectionEmpty") }}
                                    </p>
                                    <div class="mt-4 grid grid-cols-2 gap-2">
                                        <UFormField :label="t('views.adminfaced.review.reviewEntryCategory')">
                                            <USelectMenu
                                                    v-model="selectionEntryForm.category"
                                                    :items="categoryOptions"
                                                    class="w-full"
                                                    value-key="value"
                                            />
                                        </UFormField>
                                        <UFormField :label="t('views.adminfaced.review.reviewEntrySeverity')">
                                            <USelectMenu
                                                    v-model="selectionEntryForm.severity"
                                                    :items="severityOptions"
                                                    class="w-full"
                                                    value-key="value"
                                            />
                                        </UFormField>
                                    </div>
                                    <UFormField :label="t('views.adminfaced.review.reviewEntryMessage')" class="mt-3">
                                        <UTextarea
                                                v-model="selectionEntryForm.message"
                                                :placeholder="t('views.adminfaced.review.reviewEntryMessagePlaceholder')"
                                                :rows="2"
                                        />
                                    </UFormField>
                                    <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')"
                                                class="mt-3">
                                        <UTextarea
                                                v-model="selectionEntryForm.suggestion"
                                                :placeholder="t('views.adminfaced.review.reviewEntrySuggestionPlaceholder')"
                                                :rows="2"
                                        />
                                    </UFormField>
                                    <div class="mt-4 flex justify-end gap-2">
                                        <UButton
                                                color="primary"
                                                size="sm"
                                                @click="addEntryFromSelection">
                                            {{ t("views.adminfaced.review.selectionPopoverAdd") }}
                                        </UButton>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="px-6 py-4 border-t border-neutral-200 dark:border-neutral-800 text-sm text-neutral-500">
                            <div class="flex flex-wrap gap-4">
                                <span>User ID {{ currentJobView?.userId }}</span>
                                <span>{{
                                        t("views.adminfaced.review.created")
                                    }} {{ new Date(currentJobView!.createTime).toLocaleString() }}</span>
                            </div>
                        </div>
                    </div>


                </div>

                <div class="lg:col-span-1 space-y-4">
                    <div class="review-sticky">
                        <div class="rounded-[20px] border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 p-5">
                            <div class="flex w-full justify-between gap-2">
                                <UButton
                                        :disabled="isSubmitting || currentIndex === 0 || loadingContent"
                                        block
                                        class="flex-1"
                                        color="neutral"
                                        icon="i-lucide-arrow-left"
                                        variant="outline"
                                        @click="prevJob"
                                >
                                    {{ t("views.adminfaced.review.prev") }}
                                </UButton>
                                <UButton
                                        :disabled="isSubmitting || currentIndex >= reviewQueue.length - 1 || loadingContent"
                                        block
                                        class="flex-1"
                                        color="neutral"
                                        icon="i-lucide-arrow-right"
                                        variant="outline"
                                        @click="nextJob"
                                >
                                    {{ t("views.adminfaced.review.next") }}
                                </UButton>
                            </div>
                            <div class="mt-6 space-y-4">
                                <UFormField :label="t('views.adminfaced.review.reviewDecision')">
                                    <USelectMenu
                                            v-model="verdict"
                                            :items="verdictOptions"
                                            class="w-full"
                                            value-key="value"
                                    />
                                </UFormField>
                                <UFormField :label="t('views.adminfaced.review.reviewSummary')">
                                    <UTextarea
                                            v-model="reviewSummary"
                                            :disabled="isSubmitting || loadingContent"
                                            :placeholder="t('views.adminfaced.review.reviewSummaryPlaceholder')"
                                            :rows="4"
                                            class="w-full"
                                    />
                                </UFormField>
                                <div class="flex flex-col gap-2">
                                    <UButton
                                            :disabled="reviewActionDisabled"
                                            :loading="isSubmitting"
                                            block
                                            color="primary"
                                            icon="i-lucide-check-circle"
                                            variant="solid"
                                            @click="submitReview">
                                        {{ verdictOptions.find(option => option.value === verdict)?.label }}
                                    </UButton>
                                    <p class="text-xs text-neutral-500">
                                        {{ t("views.adminfaced.review.validation.reasonRequired") }}
                                    </p>
                                </div>
                            </div>

                            <div class="flex items-center justify-between">
                                <div>
                                    <h3 class="text-lg font-semibold text-neutral-900 dark:text-white">
                                        {{ t("views.adminfaced.review.reviewEntries") }}
                                    </h3>
                                    <p class="text-sm text-neutral-500 mt-1">
                                        {{ t("views.adminfaced.review.reviewEntriesHint") }}
                                    </p>
                                </div>
                                <div class="text-sm text-neutral-500">
                                    {{ entriesCount }}
                                </div>
                            </div>

                            <div class="mt-6 space-y-4">
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <UFormField :label="t('views.adminfaced.review.reviewEntryCategory')">
                                        <USelectMenu
                                                v-model="entryForm.category"
                                                :items="categoryOptions"
                                                class="w-full"
                                                value-key="value"
                                        />
                                    </UFormField>
                                    <UFormField :label="t('views.adminfaced.review.reviewEntrySeverity')">
                                        <USelectMenu
                                                v-model="entryForm.severity"
                                                :items="severityOptions"
                                                class="w-full"
                                                value-key="value"
                                        />
                                    </UFormField>
                                </div>
                                <UFormField :label="t('views.adminfaced.review.reviewEntryMessage')">
                                    <UTextarea
                                            v-model="entryForm.message"
                                            :placeholder="t('views.adminfaced.review.reviewEntryMessagePlaceholder')"
                                            :rows="2"
                                    />
                                </UFormField>
                                <UFormField :label="t('views.adminfaced.review.reviewEntrySuggestion')">
                                    <UTextarea
                                            v-model="entryForm.suggestion"
                                            :placeholder="t('views.adminfaced.review.reviewEntrySuggestionPlaceholder')"
                                            :rows="2"
                                    />
                                </UFormField>
                                <div class="flex justify-end">
                                    <UButton color="primary" variant="outline" @click="addManualEntry">
                                        {{ t("views.adminfaced.review.reviewEntryAdd") }}
                                    </UButton>
                                </div>
                            </div>

                            <div class="mt-6 border-t border-neutral-200 dark:border-neutral-800 pt-6">
                                <div v-if="sortedEntries.length === 0" class="text-sm text-neutral-500">
                                    {{ t("views.adminfaced.review.reviewEntriesEmpty") }}
                                </div>
                                <div v-else class="space-y-4">
                                    <div v-for="item in sortedEntries" :key="`${item.entry.message}-${item.index}`"
                                         class="border border-neutral-200 dark:border-neutral-800 rounded-xl px-4 py-4">
                                        <div class="flex items-center justify-between gap-3">
                                            <div class="flex items-center gap-2 text-xs uppercase tracking-[0.18em] text-neutral-400">
                                                <span>{{ entrySourceLabel(item.entry) }}</span>
                                                <span v-if="item.entry.reviewerSource.isAutomatic"
                                                      class="text-neutral-500">
                                                    {{ t("views.adminfaced.review.reviewEntryAutoImported") }}
                                                </span>
                                                <span>{{ getEntryCategoryLabel(item.entry) }}</span>
                                                <span>{{ getEntrySeverityLabel(item.entry) }}</span>
                                            </div>
                                            <div class="flex items-center gap-2 text-xs text-neutral-500">
                                                <button
                                                        class="text-neutral-500 hover:text-neutral-900 dark:hover:text-white"
                                                        @click="scrollToEntry(item.entry)">
                                                    {{ t("views.adminfaced.review.reviewEntryGoTo") }}
                                                </button>
                                                <button
                                                        v-if="!item.entry.reviewerSource.isAutomatic"
                                                        class="text-neutral-400 hover:text-red-500"
                                                        @click="removeEntryAtIndex(item.index)">
                                                    {{ t("views.adminfaced.review.reviewEntryRemove") }}
                                                </button>
                                            </div>
                                        </div>
                                        <div class="mt-3 text-sm text-neutral-700 dark:text-neutral-200">
                                            {{ item.entry.message }}
                                        </div>
                                        <div v-if="item.entry.suggestion" class="mt-2 text-sm text-neutral-500">
                                            {{ item.entry.suggestion }}
                                        </div>
                                        <div class="mt-3 text-xs text-neutral-500">
                                            {{ t("views.adminfaced.review.reviewEntryLocation") }}:
                                            {{ getLocationLabel(item.entry) }}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </template>
    </DashboardPanel>
</template>

<style scoped>

.selection-popover {
    width: 320px;
    border: 1px solid #e5e7eb;
    background: #ffffff;
    padding: 16px;
    border-radius: 16px;
    z-index: 30;
}

.review-sticky {
    position: sticky;
    top: calc(var(--ui-header-height) + 16px);
}

@media (max-width: 1024px) {
    .review-sticky {
        position: static;
    }
}

.dark .selection-popover {
    background: #0b0f1a;
    border-color: #1f2937;
}
</style>
