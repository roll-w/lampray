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
import {computed, onMounted, ref} from "vue";
import {reviewService} from "@/services/content/review.service";
import {
    type ReviewJobContentView,
    type ReviewJobDetailsView,
    type ReviewJobView,
    type ReviewFeedbackEntry,
    ReviewStatus,
    type ReviewTaskView, ReviewVerdict,
} from "@/services/content/review.type";
import {useAxios} from "@/composables/useAxios.ts";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useUserStore} from "@/stores/user.ts";
import ReviewContent from "./ReviewContent.vue";
import ReviewSidebar from "./ReviewSidebar.vue";
import {
    provideReviewQueueContext,
    type LocalReviewEntry,
    type ReviewEntryDraft,
    type ReviewSelection
} from "./reviewQueueContext.ts";

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

const currentDraft = ref<ReviewEntryDraft | null>(null);
const currentEntries = ref<LocalReviewEntry[]>([]);
const selectedEntry = ref<ReviewFeedbackEntry | null>(null);

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
    if (reviewQueue.value.length <= 1) return 100;
    return Math.round((currentIndex.value / (reviewQueue.value.length - 1)) * 100);
});

const isFirst = computed(() => currentIndex.value <= 0);
const isLast = computed(() => currentIndex.value >= reviewQueue.value.length - 1);

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

const reviewActionDisabled = computed(() => {
    return loadingContent.value;
});

const setDraftFromSelection = (selection: ReviewSelection) => {
    currentDraft.value = {
        location: selection.range,
        text: selection.text
    };
};

const clearDraft = () => {
    currentDraft.value = null;
};

const toggleEntrySelection = (entry: ReviewFeedbackEntry) => {
    selectedEntry.value = selectedEntry.value === entry ? null : entry;
};

const clearSelection = () => {
    selectedEntry.value = null;
};

const selectEntry = (entry: ReviewFeedbackEntry | null) => {
    selectedEntry.value = entry;
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
        reviewSummary.value = "";
        selectedEntry.value = null;

        // Load entries from current task
        if (currentJobDetails.value && reviewTask.value) {
            currentEntries.value = [...(reviewTask.value.feedback?.entries || [])].map(e => ({
                ...e,
                originalText: undefined // We don't have this from backend usually
            }));
        } else {
            currentEntries.value = [];
        }

    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loadingContent.value = false;
    }
};

const submittingReview = ref(false);

const handleReviewSubmit = async (verdict: ReviewVerdict) => {
    if (!currentJob.value || !reviewTask.value || submittingReview.value) return;

    if (verdict !== ReviewVerdict.APPROVED && currentEntries.value.length === 0 && !reviewSummary.value.trim()) {
        toast.add(newErrorToastFromError(
                new Error(t("views.adminfaced.review.validation.reasonRequired")),
                t("views.adminfaced.review.validation.title")
        ));
        return;
    }

    submittingReview.value = true;
    try {
        await reviewApi.makeReview(currentJob.value.id, reviewTask.value.taskId, {
            verdict: verdict,
            entries: currentEntries.value,
            summary: reviewSummary.value.trim() || undefined
        });
        toast.add(newSuccessToast(t("views.adminfaced.review.approveSuccess")));
        handleReviewSubmitSuccess();
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        submittingReview.value = false;
    }
};

const handleReviewSubmitSuccess = () => {
    if (currentIndex.value >= 0 && currentIndex.value < reviewQueue.value.length) {
        reviewQueue.value.splice(currentIndex.value, 1);
    }

    if (reviewQueue.value.length === 0) {
        currentIndex.value = -1;
        currentJobView.value = null;
        currentJobDetails.value = null;
        toast.add(newSuccessToast(t("views.adminfaced.review.queueComplete")));
    } else {
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

const refreshQueue = () => {
    fetchReviewJobs();
};

provideReviewQueueContext({
    state: {
        job: currentJob,
        jobContent: currentJobView,
        task: reviewTask,
        entries: currentEntries,
        draft: currentDraft,
        summary: reviewSummary,
        selectedEntry,
        progress: queueProgress,
        isFirst,
        isLast,
        disabled: reviewActionDisabled,
        submitting: submittingReview
    },
    actions: {
        setDraftFromSelection,
        clearDraft,
        selectEntry,
        toggleEntrySelection,
        clearSelection,
        submitReview: handleReviewSubmit,
        prevJob,
        nextJob
    }
});

onMounted(() => {
    fetchReviewJobs();
});
</script>

<template>
    <DashboardPanel>
        <template ref="dashboardHeader" #header>
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
                            size="md"
                            variant="soft"
                            @click="refreshQueue"
                    >
                        <template #leading>
                            <UIcon name="i-lucide-refresh-cw"/>
                        </template>
                    </UButton>
                </template>
            </UDashboardNavbar>
        </template>

        <template #body>
            <div v-if="loading" class="flex items-center justify-center h-full">
                <div class="flex flex-col items-center gap-4">
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
                        class="w-full h-full p-6"
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
                    <main class="min-w-0 min-h-[calc(100vh-64px)] z-0">
                        <div class="w-full p-6 lg:p-10 space-y-6">
                            <ReviewContent/>
                        </div>
                    </main>

                    <aside class="hidden lg:block sticky top-0 self-start w-[400px]">
                        <div class="scrollbar-hidden p-4 overflow-y-auto">
                            <ReviewSidebar v-if="reviewTask"/>
                        </div>
                    </aside>

                    <div class="lg:hidden block border-t border-neutral-200 dark:border-neutral-800">
                        <ReviewSidebar v-if="reviewTask"/>
                    </div>
                </div>
            </div>
        </template>
    </DashboardPanel>
</template>
