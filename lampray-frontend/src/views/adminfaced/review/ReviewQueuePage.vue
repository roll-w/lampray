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

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {reviewService} from "@/services/content/review.service";
import {type ReviewJobContentView, type ReviewJobView, ReviewStatuses} from "@/services/content/review.type";
import {useAxios} from "@/composables/useAxios.ts";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";

const axios = useAxios();
const reviewApi = reviewService(axios);
const toast = useToast();
const {t} = useI18n();

const loading = ref(false);
const currentJobView = ref<ReviewJobContentView | null>(null);
const reviewQueue = ref<ReviewJobView[]>([]);
const reviewReason = ref("");
const isSubmitting = ref(false);

const hasCurrentJob = computed(() => currentJob.value !== null);
const currentIndex = ref(0);
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

/**
 * Fetch review jobs
 */
const fetchReviewJobs = async () => {
    loading.value = true;
    try {
        const response = await reviewApi.getReviewJobs(ReviewStatuses.UNFINISHED);
        reviewQueue.value = response.data.data || [];
        currentIndex.value = 0;
        // Auto load first job
        if (reviewQueue.value.length > 0 && !currentJobView.value) {
            await loadNextJob();
        } else if (reviewQueue.value.length === 0) {
            currentIndex.value = -1;
        }
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loading.value = false;
    }
};

/**
 * Load next job content
 */
const loadNextJob = async (index: number = 0) => {
    if (reviewQueue.value.length === 0) {
        currentIndex.value = -1;
        return;
    }

    const nextJob = reviewQueue.value[index];
    if (!nextJob) {
        currentIndex.value = -1;
        toast.add(newSuccessToast(t("views.adminfaced.review.queueComplete")));
        return;
    }

    loading.value = true;
    try {
        const response = await reviewApi.getReviewContent(nextJob.id);
        currentJobView.value = response.data!.data!;
        reviewReason.value = "";
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loading.value = false;
    }
};

/**
 * Submit review decision
 */
const submitReview = async (pass: boolean) => {
    if (!currentJob.value) return;

    if (!pass && !reviewReason.value.trim()) {
        toast.add(newErrorToastFromError(
            new Error(t("views.adminfaced.review.validation.reasonRequired")),
            t("views.adminfaced.review.validation.title")
        ));
        return;
    }

    isSubmitting.value = true;
    try {
        await reviewApi.makeReview(currentJob.value.id, {
            pass,
            result: reviewReason.value.trim()
        });

        toast.add(newSuccessToast(
            pass
                ? t("views.adminfaced.review.approveSuccess")
                : t("views.adminfaced.review.rejectSuccess")
        ));

        nextJob()
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        isSubmitting.value = false;
    }
};

const handleApprove = () => submitReview(true);

const handleReject = () => submitReview(false);

const nextJob = () => {
    currentIndex.value += 1;
    loadNextJob(currentIndex.value);
};

const prevJob = () => {
    if (currentIndex.value > 0) {
        currentIndex.value -= 1;
        loadNextJob(currentIndex.value);
    }
};

/**
 * Refresh queue
 */
const refreshQueue = () => {
    currentIndex.value = -1;
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
                <template #right>
                    <UButton
                        icon="i-lucide-refresh"
                        color="primary"
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
                        {{ t("views.adminfaced.review.progress", {
                            current: currentIndex + 1,
                            total: reviewQueue.length
                        }) }}
                    </span>
                    <span class="text-sm text-gray-500 dark:text-gray-400">
                        {{ Math.round(queueProgress) }}%
                    </span>
                </div>
                <UProgress :model-value="queueProgress"/>
            </div>

            <div v-if="loading && !currentJob" class="flex items-center justify-center py-12">
                <div class="text-center">
                    <UIcon name="i-lucide-loader"
                           class="w-8 h-8 animate-spin text-primary mb-2"/>
                    <p class="text-gray-500 dark:text-gray-400">
                        {{ t("views.adminfaced.review.loading") }}
                    </p>
                </div>
            </div>

            <div v-else-if="!hasCurrentJob && !loading" class="flex flex-col items-center justify-center py-12">
                <UIcon name="i-heroicons-check-circle" class="w-16 h-16 text-green-500 mb-4"/>
                <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                    {{ t("views.adminfaced.review.noPendingReviews") }}
                </h3>
                <p class="text-gray-500 dark:text-gray-400 mb-4">
                    {{ t("views.adminfaced.review.allCompleted") }}
                </p>
                <UButton @click="refreshQueue" color="primary">
                    {{ t("views.adminfaced.review.checkForNew") }}
                </UButton>
            </div>

            <div v-else-if="hasCurrentJob" class="space-y-3 px-4">
                <UCard>
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div>
                            <span class="text-sm text-gray-500 dark:text-gray-400">
                                {{ t("views.adminfaced.review.jobId") }}
                            </span>
                            <p class="font-medium text-gray-900 dark:text-white">
                                #{{ currentJob!.id }}
                            </p>
                        </div>
                        <div>
                            <span class="text-sm text-gray-500 dark:text-gray-400">
                                {{ t("views.adminfaced.review.resourceType") }}
                            </span>
                            <p class="font-medium text-gray-900 dark:text-white">
                                {{ currentJob!.contentType }}
                            </p>
                        </div>
                        <div>
                            <span class="text-sm text-gray-500 dark:text-gray-400">
                                {{ t("views.adminfaced.review.resourceId") }}
                            </span>
                            <p class="font-medium text-gray-900 dark:text-white">
                                {{ currentJob!.contentId }}
                            </p>
                        </div>
                        <div>
                            <span class="text-sm text-gray-500 dark:text-gray-400">
                                {{ t("views.adminfaced.review.created") }}
                            </span>
                            <p class="font-medium text-gray-900 dark:text-white">
                                {{ new Date(currentJob!.assignedTime).toLocaleString() }}
                            </p>
                        </div>
                    </div>
                </UCard>
                <UCard>
                    <template #header>
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                            {{ t("views.adminfaced.review.contentToReview") }}
                        </h3>
                    </template>
                    <div class="prose dark:prose-invert max-w-none">
                        <pre class="whitespace-pre-wrap break-words bg-gray-50 dark:bg-gray-900 p-4 rounded">{{
                            JSON.stringify(currentJobView)
                        }}</pre>
                    </div>
                </UCard>

                <UCard>
                    <template #header>
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                            {{ t("views.adminfaced.review.reviewDecision") }}
                        </h3>
                    </template>

                    <div class="space-y-4">
                        <UFormField
                            :label="t('views.adminfaced.review.reviewComments')"
                            name="reviewReason"
                        >
                            <UTextarea
                                v-model="reviewReason"
                                class="w-full"
                                :placeholder="t('views.adminfaced.review.reviewCommentsPlaceholder')"
                                :rows="4"
                            />
                        </UFormField>

                        <div class="flex items-center gap-3">
                            <UButton
                                color="success"
                                variant="subtle"
                                icon="i-lucide-check-circle"
                                :loading="isSubmitting"
                                @click="handleApprove"
                            >
                                {{ t("views.adminfaced.review.approve") }}
                            </UButton>
                            <UButton
                                color="error"
                                variant="subtle"
                                icon="i-lucide-x-circle"
                                :loading="isSubmitting"
                                @click="handleReject"
                            >
                                {{ t("views.adminfaced.review.reject") }}
                            </UButton>
                            <UButton
                                    color="neutral"
                                    variant="outline"
                                    icon="i-lucide-arrow-left"
                                    :disabled="isSubmitting || currentIndex === 0"
                                    @click="prevJob"
                            >
                                {{ t("views.adminfaced.review.previous") }}
                            </UButton>
                            <UButton
                                color="neutral"
                                variant="outline"
                                icon="i-lucide-arrow-right"
                                :disabled="isSubmitting"
                                @click="nextJob"
                            >
                                {{ t("views.adminfaced.review.skip") }}
                            </UButton>

                        </div>
                    </div>
                </UCard>
            </div>
        </template>
    </DashboardPanel>
</template>

<style scoped>
</style>