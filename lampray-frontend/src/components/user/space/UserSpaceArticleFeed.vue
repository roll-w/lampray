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

<script setup lang="ts">
import {computed, ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {UrlContentType} from "@/services/content/content.type.ts";
import {contentService} from "@/services/content/content.service.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {RouteName} from "@/router/routeName.ts";
import {newErrorToastFromError} from "@/utils/toasts.ts";
import {extractPlainText} from "@/views/userfaced/article/utils/structuralText.ts";
import type {ArticleListItemView} from "@/views/userfaced/article/types/articleView.ts";

interface Props {
    userId: number;
}

interface AlertState {
    title: string;
    description: string;
    color: "warning" | "info";
}

const props = defineProps<Props>();

const {t} = useI18n();
const axios = useAxios();
const toast = useToast();

const BATCH_SIZE = 8;
const loading = ref(false);
const unavailable = ref(false);
const expectedAlert = ref<AlertState | null>(null);
const articles = ref<ArticleListItemView[]>([]);
const visibleCount = ref(BATCH_SIZE);

const visibleArticles = computed(() => articles.value.slice(0, visibleCount.value));
const canShowMore = computed(() => visibleCount.value < articles.value.length);
const skeletonRows = [0, 1, 2, 3] as const;

function articleDetailRoute(articleId: number) {
    return {
        name: RouteName.ARTICLE_DETAIL,
        params: {
            userId: props.userId,
            articleId,
        },
    };
}

function formatTime(time: string): string {
    return new Date(time).toLocaleString();
}

function getExcerpt(article: ArticleListItemView): string {
    const plainText = extractPlainText(article.content);
    if (plainText.length <= 220) {
        return plainText;
    }
    return `${plainText.slice(0, 220)}...`;
}

function showMore(): void {
    visibleCount.value += BATCH_SIZE;
}

function isExpectedStatus(status: number | null): boolean {
    if (status === null) {
        return false;
    }
    return status === 401 || status === 403 || status === 404;
}

function toExpectedAlert(status: number): AlertState {
    if (status === 404) {
        return {
            title: t("article.list.userNotFoundTitle"),
            description: t("article.list.userNotFoundDescription"),
            color: "info",
        };
    }
    return {
        title: t("article.list.restrictedTitle"),
        description: t("article.list.restrictedDescription"),
        color: "warning",
    };
}

async function loadArticles(): Promise<void> {
    loading.value = true;
    unavailable.value = false;
    expectedAlert.value = null;

    try {
        const response = await contentService(axios).getUserContents<ArticleListItemView>(props.userId, UrlContentType.ARTICLE);
        articles.value = response.data.data ?? [];
        visibleCount.value = BATCH_SIZE;
    } catch (error: any) {
        const status = error?.response?.status ?? null;
        if (isExpectedStatus(status)) {
            expectedAlert.value = toExpectedAlert(status);
        } else {
            unavailable.value = true;
            toast.add(newErrorToastFromError(error, t("request.error.title")));
        }
        articles.value = [];
    } finally {
        loading.value = false;
    }
}

watch(() => props.userId, () => {
    void loadArticles();
}, {immediate: true});
</script>

<template>
    <section id="articles" class="space-y-4">
        <div v-if="loading" class="space-y-2">
            <div
                    v-for="row in skeletonRows"
                    :key="row"
                    class="rounded-xl px-5 py-4 bg-white/85 dark:bg-neutral-950/60 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70"
            >
                <div class="flex items-start justify-between gap-4">
                    <USkeleton class="h-6 w-2/3" />
                    <USkeleton class="h-4 w-24" />
                </div>
                <div class="space-y-2 mt-3">
                    <USkeleton class="h-4 w-full" />
                    <USkeleton class="h-4 w-4/5" />
                </div>
            </div>
        </div>

        <div v-else-if="expectedAlert" class="min-h-64 flex items-center">
            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                <UAlert
                        :title="expectedAlert.title"
                        :description="expectedAlert.description"
                        :color="expectedAlert.color"
                        :icon="expectedAlert.color === 'info' ? 'i-lucide-info' : 'i-lucide-lock'"
                        variant="subtle"
                        class="w-full"
                />
            </div>
        </div>

        <div v-else-if="unavailable" class="min-h-64 flex items-center">
            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                <UAlert
                        :title="t('request.error.title')"
                        :description="t('article.list.unavailable')"
                        color="neutral"
                        icon="i-lucide-cloud-off"
                        variant="subtle"
                        class="w-full"
                />
            </div>
        </div>

        <div v-else-if="articles.length === 0" class="min-h-64 flex items-center">
            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                <UAlert
                        :title="t('article.list.empty')"
                        :description="t('views.userfaced.space.sections.articles.description')"
                        color="info"
                        icon="i-lucide-inbox"
                        variant="subtle"
                        class="w-full"
                />
            </div>
        </div>

        <div v-else class="space-y-2">
            <RouterLink
                    v-for="article in visibleArticles"
                    :key="article.id"
                    :to="articleDetailRoute(article.id)"
                    class="group block rounded-xl px-5 py-4 bg-white/85 dark:bg-neutral-950/60 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 hover:ring-primary-300/70 dark:hover:ring-primary-700/70 hover:bg-primary-50/50 dark:hover:bg-primary-900/15 transition-colors duration-200"
            >
                <div class="flex items-start justify-between gap-4">
                    <div class="space-y-1 min-w-0">
                        <UBadge size="xs" variant="subtle" color="neutral">{{ t("content.type.article") }}</UBadge>
                        <h3 class="text-base sm:text-lg font-semibold leading-snug text-neutral-900 dark:text-neutral-100 group-hover:text-primary-700 dark:group-hover:text-primary-300 transition-colors">
                            {{ article.title }}
                        </h3>
                    </div>
                    <span class="text-xs uppercase tracking-[0.12em] text-neutral-500 dark:text-neutral-400 whitespace-nowrap pt-1">
                        {{ formatTime(article.createTime) }}
                    </span>
                </div>

                <p class="mt-2 text-sm leading-6 text-neutral-600 dark:text-neutral-300">{{ getExcerpt(article) }}</p>
            </RouterLink>

            <UButton v-if="canShowMore" variant="soft" color="primary" @click="showMore">
                {{ t("article.list.showMore") }}
            </UButton>
        </div>
    </section>
</template>
