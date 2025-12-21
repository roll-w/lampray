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
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {getContentTypeI18nKey} from "@/services/content/content.type.ts";

const axios = useAxios();
const reviewApi = reviewService(axios);
const toast = useToast();
const {t} = useI18n();

const loading = ref(true);
const currentJobView = ref<ReviewJobContentView | null>(null);
const reviewQueue = ref<ReviewJobView[]>([]);
const reviewReason = ref("");
const isSubmitting = ref(false);
const loadingContent = ref(false);

const currentIndex = ref(-1);
const hasCurrentJob = computed(() => currentJob.value !== null);

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

/**
 * Fetch review jobs from server
 */
const fetchReviewJobs = async () => {
    loading.value = true;
    reviewQueue.value = [];
    currentIndex.value = -1;
    currentJobView.value = null;

    try {
        const response = await reviewApi.getReviewJobs(ReviewStatuses.UNFINISHED);
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
        return;
    }

    const job = reviewQueue.value[index];
    if (!job) return;

    loadingContent.value = true;
    currentIndex.value = index;
    try {
        const response = await reviewApi.getReviewContent(job.id);
        currentJobView.value = response.data?.data || null;
        reviewReason.value = "";

    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        loadingContent.value = false;
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
            reason: reviewReason.value.trim()
        });

        toast.add(newSuccessToast(
                pass
                        ? t("views.adminfaced.review.approveSuccess")
                        : t("views.adminfaced.review.rejectSuccess")
        ));

        // Remove current job from queue
        reviewQueue.value.splice(currentIndex.value, 1);

        // Load next job or complete
        if (reviewQueue.value.length === 0) {
            currentIndex.value = -1;
            currentJobView.value = null;
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

const handleApprove = () => submitReview(true);
const handleReject = () => submitReview(false);

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
                <template #right>
                    <UButton
                            icon="i-lucide-refresh-cw"
                            color="primary"
                            variant="outline"
                            :loading="loading"
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
                    <UIcon name="i-lucide-loader-2"
                           class="w-8 h-8 animate-spin text-primary mb-2"/>
                    <p class="text-gray-500 dark:text-gray-400">
                        {{ t("views.adminfaced.review.loading") }}
                    </p>
                </div>
            </div>

            <div v-else-if="!hasCurrentJob && !loading"
                 class="flex flex-col items-center justify-center py-12">
                <div class="flex flex-col items-center gap-2 py-4">
                    <div class="w-14 h-14 rounded-full bg-gradient-to-br flex items-center justify-center">
                        <UIcon name="i-lucide-check-circle" class="w-7 h-7 text-success"/>
                    </div>
                    <h1 class="text-xl font-bold text-gray-900 dark:text-white">
                        {{ t("views.adminfaced.review.noPendingReviews") }}</h1>
                    <p class="text-normal text-gray-600 dark:text-gray-400">
                        {{ t("views.adminfaced.review.allCompleted") }}
                    </p>
                    <UButton @click="refreshQueue" color="primary" class="mt-5">
                        {{ t("views.adminfaced.review.checkForNew") }}
                    </UButton>
                </div>

            </div>

            <div v-else-if="hasCurrentJob" class="grid grid-cols-1 lg:grid-cols-4 gap-4 px-4">
                <div class="lg:col-span-3 space-y-4">

                    <UPageCard :title="t('views.adminfaced.review.contentToReview')">
                        <div v-if="loadingContent" class="flex items-center justify-center py-8">
                            <UIcon name="i-lucide-loader-2"
                                   class="w-6 h-6 animate-spin text-primary"/>
                        </div>
                        <div v-else-if="currentJobView">
                            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 pb-8">
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
                                    {{ t("views.adminfaced.review.contentType") }}
                                </span>
                                    <p class="font-medium text-gray-900 dark:text-white">
                                        {{ contentTypeDisplay }}
                                    </p>
                                </div>
                                <div>
                                <span class="text-sm text-gray-500 dark:text-gray-400">
                                    {{ t("views.adminfaced.review.contentId") }}
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
                            <div v-if="currentJobView.title" class="mb-4">
                                <h4 class="text-xl font-bold text-gray-900 dark:text-white">
                                    {{ currentJobView.title }}
                                </h4>
                            </div>

                            <div class="overflow-hidden border-t border-gray-200 dark:border-gray-700">
                                <StructuralTextEditor
                                        :model-value="currentJobView!.content"
                                        :editable="false"
                                        :show-toolbar="false"
                                        :show-outline="false">
                                </StructuralTextEditor>
                            </div>

                            <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                                <div class="grid grid-cols-2 gap-4 text-sm">
                                    <div>
                                        <span class="text-gray-500 dark:text-gray-400">User ID:</span>
                                        <span class="ml-2 text-gray-900 dark:text-white">{{
                                                currentJobView.userId
                                            }}</span>
                                    </div>
                                    <div>
                                        <span class="text-gray-500 dark:text-gray-400">Created:</span>
                                        <span class="ml-2 text-gray-900 dark:text-white">
                                            {{ new Date(currentJobView.createTime).toLocaleString() }}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </UPageCard>
                </div>

                <div class="lg:col-span-1 space-y-4">
                    <UPageCard :title="t('views.adminfaced.review.navigation')">
                        <div class="flex w-full justify-between gap-2">
                            <UButton
                                    block
                                    color="neutral"
                                    variant="outline"
                                    class="flex-1"
                                    icon="i-lucide-arrow-left"
                                    :disabled="isSubmitting || currentIndex === 0 || loadingContent"
                                    @click="prevJob"
                            >
                                {{ t("views.adminfaced.review.prev") }}
                            </UButton>
                            <UButton
                                    block
                                    color="neutral"
                                    variant="outline"
                                    class="flex-1"
                                    icon="i-lucide-arrow-right"
                                    :disabled="isSubmitting || currentIndex >= reviewQueue.length - 1 || loadingContent"
                                    @click="nextJob"
                            >
                                {{ t("views.adminfaced.review.next") }}
                            </UButton>
                        </div>
                    </UPageCard>

                    <UPageCard :title="t('views.adminfaced.review.reviewDecision')">
                        <div class="space-y-4 w-full">
                            <UFormField
                                    :label="t('views.adminfaced.review.reviewComments')"
                                    name="reviewReason"
                            >
                                <UTextarea
                                        v-model="reviewReason"
                                        class="w-full"
                                        :placeholder="t('views.adminfaced.review.reviewCommentsPlaceholder')"
                                        :rows="4"
                                        :disabled="isSubmitting || loadingContent"
                                />
                            </UFormField>

                            <div class="flex w-full justify-between gap-2">
                                <UButton block
                                         color="success"
                                         variant="subtle"
                                         class="flex-1"
                                         icon="i-lucide-check-circle"
                                         :loading="isSubmitting"
                                         :disabled="loadingContent"
                                         @click="handleApprove"
                                >{{ t("views.adminfaced.review.approve") }}
                                </UButton>
                                <UButton block
                                         color="error"
                                         variant="subtle"
                                         class="flex-1"
                                         icon="i-lucide-x-circle"
                                         :loading="isSubmitting"
                                         :disabled="loadingContent"
                                         @click="handleReject">
                                    {{ t("views.adminfaced.review.reject") }}
                                </UButton>
                            </div>
                        </div>
                    </UPageCard>
                </div>
            </div>
        </template>
    </DashboardPanel>
</template>

<style scoped>
</style>