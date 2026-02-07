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
import {computed, onMounted, ref} from "vue";
import {reviewService} from "@/services/content/review.service";
import {autoUpdate, shift, useFloating} from "@floating-ui/vue";
import {
    type ContentLocationRange,
    type ReviewFeedbackEntry,
    type ReviewJobContentView,
    type ReviewJobDetailsView,
    type ReviewJobView,
    ReviewStatus,
    type ReviewTaskView,
    ReviewVerdict
} from "@/services/content/review.type";
import {useAxios} from "@/composables/useAxios.ts";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useUserStore} from "@/stores/user.ts";
import ReviewContent from "./ReviewContent.vue";
import ReviewSidebar, {type ReviewEntryDraft} from "./ReviewSidebar.vue";

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
const loadingContent = ref(false);

// New State for Sidebar Form
const currentDraft = ref<ReviewEntryDraft | null>(null);
const currentEntries = ref<ReviewFeedbackEntry[]>([]);

const contentRef = ref<any>(null);

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

const verdict = ref<ReviewVerdict>(ReviewVerdict.APPROVED);

const reviewActionDisabled = computed(() => {
    return loadingContent.value;
});

const scrollToEntry = (entry: ReviewFeedbackEntry) => {
    const location = entry.locationRange;
    if (!location?.startPath) return;

    if (contentRef.value?.scrollToPath) {
        contentRef.value.scrollToPath(location.startPath);
    }
};

/**
 * Handle selection from ReviewContent
 */
const handleSelectionRange = (range: ContentLocationRange, text: string) => {
    currentDraft.value = {
        location: range,
        text: text
    };

    console.log("Selected range for new entry:", range, text);
};


const sidebarAnchor = ref(null);
const sidebarFloating = ref(null);

const {floatingStyles: sidebarFloatingStyles} = useFloating(sidebarAnchor, sidebarFloating, {
    strategy: "fixed",
    placement: "right-start",
    middleware: [
        shift({
            crossAxis: true,
            padding: {top: 70} // Add padding to account for header
        })
    ],
    whileElementsMounted: autoUpdate
});

/**
 * Fetch review jobs from server
 */
const fetchReviewJobs = async () => {
    loading.value = true;
    reviewQueue.value = [];
    currentIndex.value = -1;
    currentJobView.value = null;
    currentJobDetails.value = null;
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
        currentJobDetails.value = detailResponse.data.data || null;
        reviewSummary.value = "";

        // Restore default verdict logic
        const hasAutoEntries = currentJobDetails.value?.tasks?.some(t => t.feedback?.entries?.some(e => e.reviewerSource?.isAutomatic));
        if (hasAutoEntries) {
            verdict.value = ReviewVerdict.NEEDS_REVISION;
        } else {
            verdict.value = ReviewVerdict.APPROVED;
        }

        // Clean up
        currentDraft.value = null;
        currentEntries.value = [];
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loadingContent.value = false;
    }
};

const handleReviewSubmitSuccess = () => {
    // Remove current job from queue
    if (currentIndex.value >= 0 && currentIndex.value < reviewQueue.value.length) {
        reviewQueue.value.splice(currentIndex.value, 1);
    }

    // Load next job or complete
    if (reviewQueue.value.length === 0) {
        currentIndex.value = -1;
        currentJobView.value = null;
        currentJobDetails.value = null;
        toast.add(newSuccessToast(t("views.adminfaced.review.queueComplete")));
    } else {
        // Stay at same index (which now points to next job)
        const nextIndex = Math.min(currentIndex.value, reviewQueue.value.length - 1);
        loadJobContent(nextIndex);
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

onMounted(() => {
    fetchReviewJobs();
});
</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar class="z-20">
                <template #title>
                    <div class="flex flex-col">
                        <span class="text-base text-neutral-900 dark:text-white tracking-tight">
                            {{ t("views.adminfaced.review.title") }}
                        </span>
                    </div>
                </template>
                <template #right>
                    <UButton
                            :loading="loading"
                            color="neutral"
                            size="xs"
                            variant="ghost"
                            @click="refreshQueue"
                    >
                        <template #leading>
                            <UIcon class="size-4" name="i-lucide-refresh-cw"/>
                        </template>
                    </UButton>
                </template>
            </UDashboardNavbar>
        </template>

        <template #body>
            <div v-if="loading" class="flex items-center justify-center h-full">
                <div class="flex flex-col items-center gap-4 animate-pulse">
                    <div class="size-8 rounded-full border-2 border-neutral-200 border-t-neutral-800 animate-spin"/>
                    <p class="text-sm text-neutral-500 font-medium tracking-wide">
                        {{ t("views.adminfaced.review.loading") }}
                    </p>
                </div>
            </div>

            <div v-else-if="!hasCurrentJob" class="flex flex-col items-center justify-center h-full">
                <UEmpty
                        :description="t('views.adminfaced.review.allCompleted')"
                        :title="t('views.adminfaced.review.noPendingReviews')"
                        icon="i-lucide-check-circle"
                >
                    <template #actions>
                        <UButton color="black" size="md" variant="solid" @click="refreshQueue">
                            {{ t("views.adminfaced.review.checkForNew") }}
                        </UButton>
                    </template>
                </UEmpty>
            </div>

            <div v-else class="pb-32">
                <div class="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-0 relative">
                    <main ref="sidebarAnchor"
                          class="min-w-0 min-h-[calc(100vh-64px)] z-0">
                        <div class="w-full p-6 space-y-6">
                            <div class="flex items-center border border-neutral-200 dark:border-neutral-800 rounded-lg p-0.5">
                                <UButton
                                        :disabled="currentIndex === 0"
                                        color="neutral"
                                        icon="i-lucide-chevron-left"
                                        size="xs"
                                        variant="ghost"
                                        @click="prevJob"
                                />
                                <div class="w-px h-3 bg-neutral-200 dark:bg-neutral-800 mx-1"/>
                                <UButton
                                        :disabled="currentIndex >= reviewQueue.length - 1"
                                        color="neutral"
                                        icon="i-lucide-chevron-right"
                                        size="xs"
                                        variant="ghost"
                                        @click="nextJob"
                                />
                            </div>
                            <ReviewContent
                                    ref="contentRef"
                                    :job="currentJobView!"
                                    :entries="currentEntries"
                                    @select-range="handleSelectionRange"
                            />
                        </div>
                    </main>

                    <aside class="hidden lg:block relative">
                        <div ref="sidebarFloating"
                             :style="{ ...sidebarFloatingStyles }"
                             class="z-10"
                        >
                            <ReviewSidebar
                                    v-if="reviewTask"
                                    v-model:draft="currentDraft"
                                    v-model:summary="reviewSummary"
                                    v-model:entries="currentEntries"
                                    :disabled="reviewActionDisabled"
                                    :job="currentJob!"
                                    :progress="queueProgress"
                                    :task-id="reviewTask.taskId"
                                    @submit-success="handleReviewSubmitSuccess"
                                    @locate-entry="scrollToEntry"
                            />
                        </div>
                    </aside>

                    <div class="lg:hidden block border-t border-neutral-200 dark:border-neutral-800">
                        <ReviewSidebar
                                v-if="reviewTask"
                                v-model:draft="currentDraft"
                                v-model:summary="reviewSummary"
                                :disabled="reviewActionDisabled"
                                :job="currentJob!"
                                :progress="queueProgress"
                                :task-id="reviewTask.taskId"
                                @submit-success="handleReviewSubmitSuccess"
                                @locate-entry="scrollToEntry"
                        />
                    </div>
                </div>
            </div>
        </template>
    </DashboardPanel>
</template>
