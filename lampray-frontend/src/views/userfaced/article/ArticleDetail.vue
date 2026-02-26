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
import {useRoute} from "vue-router";
import {useI18n} from "vue-i18n";
import {useAxios} from "@/composables/useAxios.ts";
import {articleService} from "@/services/content/article.service.ts";
import {commentService} from "@/services/content/comment.service.ts";
import type {ArticleDetailsView} from "@/services/content/article.type.ts";
import {
    normalizeCommentThread,
    type CommentRequest,
    type CommentView
} from "@/services/content/comment.type.ts";
import {ContentType} from "@/services/content/content.type.ts";
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {StructuralTextType, type StructuralText} from "@/components/structuraltext/types.ts";
import {RouteName} from "@/router/routeName.ts";
import {useUserStore} from "@/stores/user.ts";
import ArticleCommentForm from "@/views/userfaced/article/components/ArticleCommentForm.vue";
import ArticleCommentList from "@/views/userfaced/article/components/ArticleCommentList.vue";
import {newErrorToast} from "@/utils/toasts.ts";

const DEFAULT_VISIBLE_COMMENT_COUNT = 10;

const route = useRoute();
const {t} = useI18n();
const axios = useAxios();
const userStore = useUserStore();
const toast = useToast();

const article = ref<ArticleDetailsView | null>(null);
const articleLoading = ref(false);
const articleLoadError = ref(false);
const articleRestricted = ref(false);

const commentsRaw = ref<CommentView[]>([]);
const commentsLoading = ref(false);
const commentsLoadError = ref(false);
const visibleCommentCount = ref(DEFAULT_VISIBLE_COMMENT_COUNT);
const submittingComment = ref(false);
const activeReplyId = ref<number | null>(null);

const articleId = computed<number>(() => {
    const value = Number(route.params.id);
    return Number.isFinite(value) && value > 0 ? value : 0;
});

const loginRoute = computed(() => {
    return {
        name: RouteName.LOGIN,
        query: {
            source: route.fullPath,
        },
    };
});

const commentThread = computed(() => normalizeCommentThread(commentsRaw.value));
const articleCreatedAt = computed(() => {
    if (!article.value) {
        return "";
    }
    return new Date(article.value.createTime).toLocaleString();
});

function makeStructuralText(value: string): StructuralText {
    return {
        type: StructuralTextType.DOCUMENT,
        content: "",
        children: [
            {
                type: StructuralTextType.PARAGRAPH,
                content: "",
                children: [
                    {
                        type: StructuralTextType.TEXT,
                        content: value,
                        children: [],
                    },
                ],
            },
        ],
    };
}

function createPendingComment(message: string, parentId: number): CommentView {
    const now = new Date().toISOString();
    return {
        id: -Date.now() - Math.floor(Math.random() * 1000),
        userId: userStore.user?.id ?? 0,
        parent: parentId,
        content: makeStructuralText(message),
        contentId: articleId.value,
        contentType: ContentType.ARTICLE,
        createTime: now,
        updateTime: now,
        pending: true,
    };
}

function showMoreComments(): void {
    visibleCommentCount.value += DEFAULT_VISIBLE_COMMENT_COUNT;
}

function requestReply(commentId: number): void {
    activeReplyId.value = commentId;
}

function cancelReply(): void {
    activeReplyId.value = null;
}

async function loadArticle(): Promise<void> {
    articleLoading.value = true;
    articleLoadError.value = false;
    articleRestricted.value = false;
    article.value = null;

    try {
        const response = await articleService(axios).getArticle(articleId.value);
        article.value = response.data.data ?? null;
    } catch (error: any) {
        const status = error?.response?.status;
        if (!userStore.isLogin && (status === 401 || status === 403 || status === 404)) {
            articleRestricted.value = true;
        } else {
            articleLoadError.value = true;
        }
    } finally {
        articleLoading.value = false;
    }
}

async function loadComments(): Promise<void> {
    commentsLoading.value = true;
    commentsLoadError.value = false;
    commentsRaw.value = [];
    visibleCommentCount.value = DEFAULT_VISIBLE_COMMENT_COUNT;

    try {
        const response = await commentService(axios).getArticleComments(articleId.value);
        commentsRaw.value = response.data.data ?? [];
    } catch {
        commentsLoadError.value = true;
    } finally {
        commentsLoading.value = false;
    }
}

async function submitComment(message: string, parentId: number): Promise<void> {
    if (!userStore.isLogin) {
        return;
    }

    const payload: CommentRequest = {
        content: makeStructuralText(message),
        parent: parentId > 0 ? parentId : null,
    };

    const snapshot = [...commentsRaw.value];
    const pending = createPendingComment(message, parentId);
    commentsRaw.value = [...commentsRaw.value, pending];
    submittingComment.value = true;

    try {
        const response = await commentService(axios).createArticleComment(articleId.value, payload);
        const savedComment = response.data.data;
        if (savedComment) {
            commentsRaw.value = commentsRaw.value.map(comment => {
                if (comment.id === pending.id) {
                    return savedComment;
                }
                return comment;
            });
        }
        activeReplyId.value = null;
    } catch {
        commentsRaw.value = snapshot;
        toast.add(newErrorToast(t("request.error.title"), t("article.detail.postFailed")));
    } finally {
        submittingComment.value = false;
    }
}

async function submitTopLevel(message: string): Promise<void> {
    await submitComment(message, 0);
}

async function submitReply(payload: { parentId: number; message: string }): Promise<void> {
    await submitComment(payload.message, payload.parentId);
}

async function retryAll(): Promise<void> {
    await loadArticle();
    if (!article.value) {
        commentsRaw.value = [];
        return;
    }
    await loadComments();
}

watch(() => route.params.id, () => {
    void retryAll();
}, {immediate: true});
</script>

<template>
    <div class="max-w-5xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        <div v-if="articleLoading" class="text-sm text-neutral-500 dark:text-neutral-400">{{ t("common.loading") }}</div>

        <div v-else-if="articleRestricted" class="space-y-2">
            <UAlert color="warning" variant="soft" :title="t('article.detail.restricted')" :description="t('article.detail.restrictedHint')" />
            <RouterLink :to="loginRoute">
                <UButton color="neutral">{{ t("views.common.user.login") }}</UButton>
            </RouterLink>
        </div>

        <div v-else-if="articleLoadError || !article" class="space-y-2">
            <p class="text-sm text-rose-600 dark:text-rose-400">{{ t("article.detail.loadError") }}</p>
            <UButton size="sm" color="neutral" variant="outline" @click="retryAll">{{ t("article.detail.retry") }}</UButton>
        </div>

        <template v-else>
            <article class="rounded-2xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 overflow-hidden">
                <header class="px-5 sm:px-8 py-6 border-b border-neutral-200 dark:border-neutral-800 space-y-3">
                    <h1 class="text-3xl sm:text-4xl font-semibold tracking-tight text-neutral-950 dark:text-neutral-50">{{ article.title }}</h1>
                    <p class="text-xs uppercase tracking-[0.16em] text-neutral-500 dark:text-neutral-400">{{ articleCreatedAt }}</p>
                </header>

                <div class="px-5 sm:px-8 py-6">
                    <StructuralTextEditor
                            :model-value="article.content"
                            :editable="false"
                            :show-toolbar="false"
                            :show-outline="false"
                            :ui="{ content: { root: 'w-full' } }"
                    />
                </div>
            </article>

            <section class="rounded-2xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 px-5 sm:px-8 py-6 space-y-4">
                <div class="flex items-center justify-between gap-3">
                    <h2 class="text-xl font-semibold text-neutral-950 dark:text-neutral-50">{{ t("article.detail.commentsTitle") }}</h2>
                    <UBadge variant="soft" color="neutral">{{ commentThread.length }}</UBadge>
                </div>

                <div v-if="commentsLoading" class="text-sm text-neutral-500 dark:text-neutral-400">{{ t("common.loading") }}</div>

                <div v-else-if="commentsLoadError" class="space-y-2">
                    <p class="text-sm text-rose-600 dark:text-rose-400">{{ t("article.detail.loadError") }}</p>
                    <UButton size="sm" color="neutral" variant="outline" @click="loadComments">{{ t("article.detail.retry") }}</UButton>
                </div>

                <div v-else class="space-y-3">
                    <ArticleCommentForm
                            v-if="userStore.isLogin"
                            :submitting="submittingComment"
                            @submit="submitTopLevel"
                    />

                    <div v-else class="text-sm text-neutral-600 dark:text-neutral-300">
                        {{ t("article.detail.loginRequired") }}
                        <RouterLink class="underline" :to="loginRoute">{{ t("views.common.user.login") }}</RouterLink>
                    </div>

                    <ArticleCommentList
                            :comments="commentThread"
                            :visible-count="visibleCommentCount"
                            :can-reply="userStore.isLogin"
                            :active-reply-id="activeReplyId"
                            :submitting="submittingComment"
                            @show-more="showMoreComments"
                            @request-reply="requestReply"
                            @submit-reply="submitReply"
                            @cancel-reply="cancelReply"
                    />
                </div>
            </section>
        </template>
    </div>
</template>
