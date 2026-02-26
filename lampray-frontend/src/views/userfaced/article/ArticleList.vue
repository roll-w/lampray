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
import {computed, onMounted, ref} from "vue";
import {useI18n} from "vue-i18n";
import type {ArticleInfoView} from "@/services/content/article.type.ts";
import {articleService} from "@/services/content/article.service.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {RouteName} from "@/router/routeName.ts";

const BATCH_SIZE = 10;

const {t} = useI18n();
const axios = useAxios();

const loading = ref(false);
const loadError = ref(false);
const articles = ref<ArticleInfoView[]>([]);
const visibleCount = ref(BATCH_SIZE);

const visibleArticles = computed(() => {
    return articles.value.slice(0, visibleCount.value);
});

const canShowMore = computed(() => visibleCount.value < articles.value.length);

function showMore(): void {
    visibleCount.value += BATCH_SIZE;
}

async function loadArticles(): Promise<void> {
    loading.value = true;
    loadError.value = false;
    try {
        const response = await articleService(axios).getArticles();
        articles.value = response.data.data ?? [];
        visibleCount.value = BATCH_SIZE;
    } catch {
        loadError.value = true;
        articles.value = [];
    } finally {
        loading.value = false;
    }
}

function formatTime(time: string): string {
    return new Date(time).toLocaleString();
}

onMounted(() => {
    loadArticles();
});
</script>

<template>
    <div class="max-w-5xl mx-auto px-4 sm:px-6 py-8 space-y-5">
        <header class="rounded-2xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 px-5 sm:px-8 py-6 space-y-2">
            <h1 class="text-3xl sm:text-4xl font-semibold tracking-tight text-neutral-950 dark:text-neutral-50">{{ t("article.list.title") }}</h1>
            <p class="text-xs uppercase tracking-[0.16em] text-neutral-500 dark:text-neutral-400">
                {{ t("article.list.showing", { visible: visibleArticles.length, total: articles.length }) }}
            </p>
        </header>

        <div v-if="loading" class="py-8 text-sm text-neutral-500 dark:text-neutral-400">{{ t("common.loading") }}</div>

        <div v-else-if="loadError" class="space-y-2">
            <p class="text-sm text-rose-600 dark:text-rose-400">{{ t("article.list.loadError") }}</p>
            <UButton size="sm" color="neutral" variant="outline" @click="loadArticles">{{ t("article.list.retry") }}</UButton>
        </div>

        <div v-else-if="articles.length === 0" class="text-sm text-neutral-500 dark:text-neutral-400">
            {{ t("article.list.empty") }}
        </div>

        <div v-else class="space-y-3">
            <article
                    v-for="article in visibleArticles"
                    :key="article.id"
                    class="rounded-xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 px-5 py-4 flex items-center justify-between gap-4"
            >
                <RouterLink
                        class="text-base sm:text-lg font-medium text-neutral-900 dark:text-neutral-100 hover:underline"
                        :to="{ name: RouteName.ARTICLE_DETAIL, params: { id: article.id } }"
                >
                    {{ article.title }}
                </RouterLink>

                <div class="text-xs text-neutral-500 dark:text-neutral-400 whitespace-nowrap">
                    {{ formatTime(article.createTime) }}
                </div>
            </article>

            <UButton v-if="canShowMore" variant="outline" color="neutral" @click="showMore">
                {{ t("article.list.showMore") }}
            </UButton>
        </div>
    </div>
</template>
