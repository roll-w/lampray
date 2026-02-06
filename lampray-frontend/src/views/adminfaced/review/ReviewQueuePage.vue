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
import {
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
import ReviewFeedbackEntries from "./ReviewFeedbackEntries.vue";
import ReviewActionPanel from "./ReviewActionPanel.vue";
import ReviewEntryForm from "./ReviewEntryForm.vue";

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
const showEntryForm = ref(false);

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
    if (loadingContent.value || isSubmitting.value) return true;
    if (verdict.value === ReviewVerdict.APPROVED) return false;
    return reviewEntries.value.length === 0 && reviewSummary.value.trim().length === 0;
});

const resetEntryForm = () => {
    showEntryForm.value = false;
};

const handleEntrySubmit = (entry: ReviewFeedbackEntry) => {
    reviewEntries.value.unshift(entry);
    resetEntryForm();
};

const handleReviewSubmit = async (verdictValue: ReviewVerdict) => {
    verdict.value = verdictValue;
    await submitReview();
};

const scrollToEntry = (entry: ReviewFeedbackEntry) => {
    const location = entry.locationRange;
    if (!location?.startPath) return;

    if (contentRef.value?.scrollToPath) {
        contentRef.value.scrollToPath(location.startPath);
    }
};

const removeEntryAtIndex = (index: number) => {
    reviewEntries.value.splice(index, 1);
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

onMounted(() => {
    fetchReviewJobs();
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
                <template #default>
                    <!--TODO: fix width-->
                    <div class="w-full flex items-center gap-4">
                        <div class="w-full flex items-center gap-2">
                            <div class="w-full h-1.5 bg-neutral-200 dark:bg-neutral-800 rounded-full overflow-hidden">
                                <div :style="{ width: `${queueProgress}%` }"
                                     class="h-full bg-primary-500 transition-all duration-500"/>
                            </div>
                            <span class="text-[10px] font-mono text-neutral-500">{{ Math.round(queueProgress) }}%</span>
                        </div>
                        <div class="flex items-center gap-1">
                            <UButton
                                    :disabled="currentIndex === 0"
                                    color="neutral"
                                    icon="i-lucide-chevron-left"
                                    size="xs"
                                    variant="ghost"
                                    @click="prevJob"
                            />
                            <UButton
                                    :disabled="currentIndex >= reviewQueue.length - 1"
                                    color="neutral"
                                    icon="i-lucide-chevron-right"
                                    size="xs"
                                    variant="ghost"
                                    @click="nextJob"
                            />
                        </div>
                    </div>
                </template>
                <template #right>
                    <UButton
                            :loading="loading"
                            color="neutral"
                            variant="soft"
                            @click="refreshQueue"
                    >
                        {{ t("views.adminfaced.review.refresh") }}
                        <template #leading>
                            <UIcon name="i-lucide-refresh-cw"/>
                        </template>
                    </UButton>
                </template>
            </UDashboardNavbar>
        </template>

        <template #body>
            <div v-if="loading" class="flex items-center justify-center py-24">
                <div class="flex flex-col items-center gap-4">
                    <UIcon class="w-10 h-10 animate-spin text-primary-500" name="i-lucide-loader-2"/>
                    <p class="text-neutral-500 ">
                        {{ t("views.adminfaced.review.loading") }}
                    </p>
                </div>
            </div>

            <div v-else-if="!hasCurrentJob" class="flex flex-col items-center justify-center py-24">
                <UEmpty
                        :description="t('views.adminfaced.review.allCompleted')"
                        :title="t('views.adminfaced.review.noPendingReviews')"
                        icon="i-lucide-check-circle"
                >
                    <template #actions>
                        <UButton color="primary" size="lg" variant="solid" @click="refreshQueue">
                            {{ t("views.adminfaced.review.checkForNew") }}
                        </UButton>
                    </template>
                </UEmpty>
            </div>

            <div v-else class="h-[calc(100vh-var(--ui-header-height))] overflow-hidden">
                <div class="h-full grid grid-cols-1 xl:grid-cols-[1fr_400px]">
                    <!-- Left: Content Area -->
                    <main class="h-full">
                        <div class="mx-auto space-y-8">
                            <span class="text-[10px] uppercase tracking-[0.2em] text-neutral-400 font-bold px-6">
                                {{ currentJob?.id }}
                            </span>
                            <div class="w-full p-6">
                                <ReviewContent
                                        ref="contentRef"
                                        :job="currentJobView!"
                                        :loading="loadingContent"
                                        @submit="handleEntrySubmit"
                                />
                            </div>
                        </div>
                    </main>

                    <!-- Right: Sidebar -->
                    <aside class="flex flex-col border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 rounded-xl">
                        <div class="flex-1 p-6 space-y-8">
                            <!-- Action Panel -->
                            <section>
                                <ReviewActionPanel
                                        v-model:summary="reviewSummary"
                                        :disabled="reviewActionDisabled"
                                        :loading="isSubmitting"
                                        @submit="handleReviewSubmit"
                                />
                            </section>

                            <!-- Feedback Entries -->
                            <section class="border-t border-neutral-100 dark:border-neutral-800 pt-8">
                                <ReviewFeedbackEntries
                                        :entries="reviewEntries"
                                        @locate="scrollToEntry"
                                        @remove="removeEntryAtIndex">
                                    <template v-if="showEntryForm" #default>
                                        <ReviewEntryForm
                                                @cancel="showEntryForm = false"
                                                @submit="e => { handleEntrySubmit(e); showEntryForm = false; }"
                                        />
                                    </template>
                                </ReviewFeedbackEntries>
                                <div class="mt-6">
                                    <UButton
                                            block
                                            class="rounded-xl py-3"
                                            color="neutral"
                                            icon="i-lucide-plus"
                                            variant="soft"
                                            @click="showEntryForm = true"
                                    >
                                        {{ t('views.adminfaced.review.reviewEntryAdd') }}
                                    </UButton>
                                </div>
                            </section>
                        </div>
                    </aside>
                </div>
            </div>
        </template>
    </DashboardPanel>
</template>

<style scoped>
</style>
