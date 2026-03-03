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
import {computed, ref, watch} from "vue";
import {useRoute} from "vue-router";
import {useI18n} from "vue-i18n";
import {useAxios} from "@/composables/useAxios.ts";
import {commentService} from "@/services/content/comment.service.ts";
import type {CommentRequest} from "@/services/content/comment.type.ts";
import {ContentType, UrlContentType} from "@/services/content/content.type.ts";
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {type StructuralText, StructuralTextType} from "@/components/structuraltext/types.ts";
import {RouteName} from "@/router/routeName.ts";
import {useUserStore} from "@/stores/user.ts";
import ArticleCommentForm from "@/views/userfaced/article/components/ArticleCommentForm.vue";
import ArticleCommentList from "@/views/userfaced/article/components/ArticleCommentList.vue";
import {newErrorToastFromError} from "@/utils/toasts.ts";
import {contentService} from "@/services/content/content.service.ts";
import {useUserProfileMap} from "@/composables/useUserProfileMap.ts";
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";
import type {ArticleDetailsView} from "@/views/userfaced/article/types/articleView.ts";
import {normalizeCommentThread, type UiCommentView} from "@/views/userfaced/article/types/commentThread.ts";
import {extractPlainText} from "@/views/userfaced/article/utils/structuralText.ts";

const DEFAULT_VISIBLE_COMMENT_COUNT = 10;
const COMMENTS_SECTION_ID = "article-comments-section";

interface AlertState {
    title: string;
    description: string;
    color: "warning" | "info";
}

const route = useRoute();
const {t} = useI18n();
const axios = useAxios();
const userStore = useUserStore();
const toast = useToast();
const {resolveProfiles} = useUserProfileMap();

const article = ref<ArticleDetailsView | null>(null);
const articleLoading = ref(false);
const articleExpectedAlert = ref<AlertState | null>(null);
const articleUnavailable = ref(false);

const commentsRaw = ref<UiCommentView[]>([]);
const commentsLoading = ref(false);
const commentsExpectedAlert = ref<AlertState | null>(null);
const commentsUnavailable = ref(false);
const visibleCommentCount = ref(DEFAULT_VISIBLE_COMMENT_COUNT);
const submittingComment = ref(false);
const activeReplyId = ref<number | null>(null);
const profileMap = ref<Map<number, UserCommonDetailsVo | null>>(new Map());

const targetUserId = computed<number>(() => {
    const value = Number(route.params.userId);
    return Number.isFinite(value) && value > 0 ? value : 0;
});

const articleId = computed<number>(() => {
    const value = Number(route.params.articleId);
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
const articlePlainText = computed(() => extractPlainText(article.value?.content));
const articleCharacterCount = computed(() => articlePlainText.value.length);
const estimatedReadMinutes = computed(() => {
    if (articleCharacterCount.value <= 0) {
        return 1;
    }
    return Math.max(1, Math.ceil(articleCharacterCount.value / 800));
});
const commentCount = computed(() => commentsRaw.value.length);
const commentCountText = computed(() => commentCount.value.toLocaleString());

const articleAuthor = computed<UserCommonDetailsVo | null>(() => {
    if (!article.value) {
        return null;
    }
    return profileMap.value.get(article.value.userId) ?? null;
});

const articleAuthorName = computed(() => {
    if (articleAuthor.value?.nickname) {
        return articleAuthor.value.nickname;
    }
    if (articleAuthor.value?.username) {
        return articleAuthor.value.username;
    }
    if (article.value) {
        return `#${article.value.userId}`;
    }
    return t("article.detail.unknownAuthor");
});

const articleAuthorSubline = computed(() => {
    if (articleAuthor.value?.username) {
        return `@${articleAuthor.value.username}`;
    }
    if (article.value) {
        return `#${article.value.userId}`;
    }
    return t("article.detail.unknownAuthor");
});

const articleAuthorInitial = computed(() => {
    const value = articleAuthorName.value;
    if (value.length === 0) {
        return "?";
    }
    return value[0]!;
});

const resolvedAuthorId = computed<number>(() => {
    return article.value?.userId ?? targetUserId.value;
});

const articleAuthorHomeRoute = computed(() => {
    return {
        name: RouteName.USER_SPACE,
        params: {
            id: resolvedAuthorId.value,
        },
    };
});

const commentSkeletonRows = [0, 1, 2] as const;

function isExpectedStatus(status: number | null): boolean {
    if (status === null) {
        return false;
    }
    return status === 401 || status === 403 || status === 404;
}

function toArticleExpectedAlert(status: number): AlertState {
    if (status === 404) {
        return {
            title: t("article.detail.notFoundTitle"),
            description: t("article.detail.notFoundDescription"),
            color: "info",
        };
    }
    return {
        title: t("article.detail.restricted"),
        description: t("article.detail.restrictedHint"),
        color: "warning",
    };
}

function toCommentExpectedAlert(status: number): AlertState {
    if (status === 404) {
        return {
            title: t("article.detail.commentsUnavailableTitle"),
            description: t("article.detail.commentsUnavailableDescription"),
            color: "info",
        };
    }
    return {
        title: t("article.detail.commentsRestrictedTitle"),
        description: t("article.detail.commentsRestrictedDescription"),
        color: "warning",
    };
}

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

function createPendingComment(message: string, parentId: number): UiCommentView {
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

function loadMoreComments(): void {
    visibleCommentCount.value += DEFAULT_VISIBLE_COMMENT_COUNT;
}

function scrollToComments(): void {
    if (typeof window === "undefined") {
        return;
    }

    const target = document.getElementById(COMMENTS_SECTION_ID);
    target?.scrollIntoView({
        behavior: "smooth",
        block: "start",
    });
}

function scrollToTop(): void {
    if (typeof window === "undefined") {
        return;
    }

    window.scrollTo({
        top: 0,
        behavior: "smooth",
    });
}

function selectReplyTarget(commentId: number): void {
    activeReplyId.value = commentId;
}

function clearReplyTarget(): void {
    activeReplyId.value = null;
}

async function loadArticle(): Promise<void> {
    articleLoading.value = true;
    articleExpectedAlert.value = null;
    articleUnavailable.value = false;
    article.value = null;
    profileMap.value = new Map();

    try {
        if (targetUserId.value <= 0 || articleId.value <= 0) {
            articleExpectedAlert.value = {
                title: t("article.detail.notFoundTitle"),
                description: t("article.detail.notFoundDescription"),
                color: "info",
            };
            return;
        }

        const response = await contentService(axios).getUserContent<ArticleDetailsView>(
                targetUserId.value,
                UrlContentType.ARTICLE,
                articleId.value
        );
        article.value = response.data.data ?? null;

        if (!article.value) {
            articleExpectedAlert.value = {
                title: t("article.detail.notFoundTitle"),
                description: t("article.detail.notFoundDescription"),
                color: "info",
            };
            return;
        }

        profileMap.value = await resolveProfiles([article.value.userId]);
    } catch (error: any) {
        const status = error?.response?.status;
        if (isExpectedStatus(status)) {
            articleExpectedAlert.value = toArticleExpectedAlert(status);
        } else {
            articleUnavailable.value = true;
            toast.add(newErrorToastFromError(error, t("request.error.title")));
        }
    } finally {
        articleLoading.value = false;
    }
}

async function loadComments(): Promise<void> {
    commentsLoading.value = true;
    commentsExpectedAlert.value = null;
    commentsUnavailable.value = false;
    commentsRaw.value = [];
    visibleCommentCount.value = DEFAULT_VISIBLE_COMMENT_COUNT;

    try {
        const response = await commentService(axios).getArticleComments(articleId.value);
        commentsRaw.value = response.data.data ?? [];

        const profileIds = commentsRaw.value.map(comment => comment.userId);
        if (article.value?.userId) {
            profileIds.push(article.value.userId);
        }
        profileMap.value = await resolveProfiles(profileIds);
    } catch (error: any) {
        const status = error?.response?.status ?? null;
        if (isExpectedStatus(status)) {
            commentsExpectedAlert.value = toCommentExpectedAlert(status);
        } else {
            commentsUnavailable.value = true;
            toast.add(newErrorToastFromError(error, t("request.error.title")));
        }
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

            const profileIds = commentsRaw.value.map(comment => comment.userId);
            if (article.value?.userId) {
                profileIds.push(article.value.userId);
            }
            profileMap.value = await resolveProfiles(profileIds);
        }
        activeReplyId.value = null;
    } catch (error: any) {
        commentsRaw.value = snapshot;
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    } finally {
        submittingComment.value = false;
    }
}

async function submitRootComment(message: string): Promise<void> {
    await submitComment(message, 0);
}

async function submitChildComment(payload: { parentId: number; message: string }): Promise<void> {
    await submitComment(payload.message, payload.parentId);
}

async function loadPageData(): Promise<void> {
    await loadArticle();
    if (!article.value) {
        commentsRaw.value = [];
        return;
    }
    await loadComments();
}

watch(() => [route.params.userId, route.params.articleId], () => {
    void loadPageData();
}, {immediate: true});
</script>

<template>
    <div class="w-full mx-auto max-w-screen-2xl px-4 sm:px-6 lg:px-10 py-6 sm:py-8 space-y-6">
        <div v-if="articleLoading" class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_20rem] items-start">
            <div class="space-y-5">
                <div class="rounded-2xl bg-neutral-100/80 dark:bg-neutral-900/45 px-5 sm:px-8 py-6 space-y-4">
                    <USkeleton class="h-4 w-24"/>
                    <USkeleton class="h-10 w-3/4"/>
                    <div class="flex items-center gap-3">
                        <USkeleton class="h-9 w-9 rounded-full"/>
                        <div class="space-y-2">
                            <USkeleton class="h-4 w-28"/>
                            <USkeleton class="h-3 w-20"/>
                        </div>
                    </div>
                    <div class="space-y-2 pt-2">
                        <USkeleton class="h-4 w-full"/>
                        <USkeleton class="h-4 w-11/12"/>
                        <USkeleton class="h-4 w-10/12"/>
                    </div>
                </div>

                <div class="rounded-2xl bg-neutral-100/80 dark:bg-neutral-900/45 px-5 sm:px-8 py-6 space-y-4">
                    <USkeleton class="h-6 w-36"/>
                    <div class="space-y-3">
                        <div
                                v-for="row in commentSkeletonRows"
                                :key="row"
                                class="rounded-xl bg-white/85 dark:bg-neutral-950/45 p-4 space-y-3"
                        >
                            <div class="flex items-center gap-3">
                                <USkeleton class="h-8 w-8 rounded-full"/>
                                <div class="space-y-2">
                                    <USkeleton class="h-3 w-24"/>
                                    <USkeleton class="h-3 w-16"/>
                                </div>
                            </div>
                            <USkeleton class="h-4 w-full"/>
                            <USkeleton class="h-4 w-4/5"/>
                        </div>
                    </div>
                </div>
            </div>

            <aside class="space-y-4">
                <div class="rounded-2xl bg-neutral-100/80 dark:bg-neutral-900/45 p-5 space-y-4">
                    <USkeleton class="h-4 w-28"/>
                    <div class="flex items-center gap-3">
                        <USkeleton class="h-10 w-10 rounded-full"/>
                        <div class="space-y-2">
                            <USkeleton class="h-4 w-24"/>
                            <USkeleton class="h-3 w-16"/>
                        </div>
                    </div>
                    <USkeleton class="h-9 w-full"/>
                    <USkeleton class="h-9 w-full"/>
                    <USkeleton class="h-20 w-full"/>
                </div>
            </aside>
        </div>

        <div v-else-if="articleExpectedAlert" class="min-h-[34vh] sm:min-h-[42vh] flex items-center">
            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                <UAlert
                        :color="articleExpectedAlert.color"
                        :description="articleExpectedAlert.description"
                        :icon="articleExpectedAlert.color === 'info' ? 'i-lucide-info' : 'i-lucide-shield-alert'"
                        :title="articleExpectedAlert.title"
                        class="w-full"
                        variant="subtle"
                />
            </div>
        </div>

        <div v-else-if="articleUnavailable" class="min-h-[34vh] sm:min-h-[42vh] flex items-center">
            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                <UAlert
                        :description="t('article.detail.unavailableDescription')"
                        :title="t('request.error.title')"
                        color="neutral"
                        icon="i-lucide-cloud-off"
                        class="w-full"
                        variant="subtle"
                />
            </div>
        </div>

        <template v-else>
            <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem] items-start">
                <div class="space-y-5">
                    <article class="rounded-2xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 overflow-hidden">
                        <header class="px-5 sm:px-8 py-6 border-b border-neutral-200/70 dark:border-neutral-700/50 space-y-4">
                            <div class="flex flex-wrap items-center gap-2">
                                <UBadge color="primary" variant="soft">{{ t("content.type.article") }}</UBadge>
                                <UBadge color="neutral" variant="subtle">{{ articleCreatedAt }}</UBadge>
                                <UBadge color="neutral" variant="subtle">{{ t("article.detail.commentsTitle") }} · {{ commentCountText }}</UBadge>
                                <UBadge color="neutral" variant="subtle">{{ t("article.detail.estimatedRead", { minutes: estimatedReadMinutes }) }}</UBadge>
                            </div>

                            <h1 class="text-3xl sm:text-4xl font-semibold tracking-tight text-neutral-950 dark:text-neutral-50 leading-tight">
                                {{ article.title }}
                            </h1>

                            <div class="flex flex-wrap items-center justify-between gap-3">
                                <RouterLink
                                        v-if="resolvedAuthorId > 0"
                                        :to="articleAuthorHomeRoute"
                                        :aria-label="t('article.detail.viewAuthorSpace', { name: articleAuthorName })"
                                        class="flex items-center gap-3 w-fit rounded-lg px-2 py-1.5 hover:bg-neutral-100/80 dark:hover:bg-neutral-900/70 transition-colors"
                                >
                                    <UAvatar
                                            :src="articleAuthor?.avatar || undefined"
                                            :text="articleAuthorInitial"
                                            class="ring-1 ring-neutral-200 dark:ring-neutral-800"
                                            size="sm"
                                    />
                                    <div class="leading-tight min-w-0">
                                        <p class="text-sm font-medium text-neutral-800 dark:text-neutral-100 truncate">
                                            {{ articleAuthorName }}
                                        </p>
                                        <p class="text-xs text-neutral-500 dark:text-neutral-400 truncate">
                                            {{ articleAuthorSubline }}
                                        </p>
                                    </div>
                                </RouterLink>

                                <div class="flex items-center gap-2">
                                    <UButton color="primary" variant="soft" size="sm" icon="i-lucide-message-circle" @click="scrollToComments">
                                        {{ t("article.detail.commentsTitle") }}
                                    </UButton>
                                    <UButton color="neutral" variant="ghost" size="sm" icon="i-lucide-arrow-up" @click="scrollToTop">
                                        {{ t("article.detail.jumpToTop") }}
                                    </UButton>
                                </div>
                            </div>
                        </header>

                        <div class="px-5 sm:px-8 py-6">
                            <StructuralTextEditor
                                    :editable="false"
                                    :model-value="article.content"
                                    :show-outline="false"
                                    :show-toolbar="false"
                                    :ui="{ content: { root: 'w-full' } }"
                            />
                        </div>
                    </article>

                    <section
                            :id="COMMENTS_SECTION_ID"
                            class="rounded-2xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 px-5 sm:px-8 py-6 space-y-4"
                    >
                        <div class="flex items-center justify-between gap-3">
                            <h2 class="text-xl font-semibold text-neutral-950 dark:text-neutral-50">
                                {{ t("article.detail.commentsTitle") }}
                            </h2>
                            <UBadge color="primary" variant="soft">{{ commentCountText }}</UBadge>
                        </div>

                        <div v-if="commentsLoading" class="space-y-3">
                            <div
                                    v-for="row in commentSkeletonRows"
                                    :key="row"
                                    class="rounded-xl bg-neutral-100/70 dark:bg-neutral-900/45 p-4 space-y-3"
                            >
                                <div class="flex items-center gap-3">
                                    <USkeleton class="h-8 w-8 rounded-full"/>
                                    <div class="space-y-2">
                                        <USkeleton class="h-3 w-24"/>
                                        <USkeleton class="h-3 w-16"/>
                                    </div>
                                </div>
                                <USkeleton class="h-4 w-full"/>
                                <USkeleton class="h-4 w-4/5"/>
                            </div>
                        </div>

                        <div v-else-if="commentsExpectedAlert" class="min-h-[34vh] sm:min-h-[42vh] flex items-center">
                            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                                <UAlert
                                        :color="commentsExpectedAlert.color"
                                        :description="commentsExpectedAlert.description"
                                        :icon="commentsExpectedAlert.color === 'info' ? 'i-lucide-info' : 'i-lucide-shield-alert'"
                                        :title="commentsExpectedAlert.title"
                                        class="w-full"
                                        variant="subtle"
                                />
                            </div>
                        </div>

                        <div v-else-if="commentsUnavailable" class="min-h-[34vh] sm:min-h-[42vh] flex items-center">
                            <div class="w-full rounded-2xl border border-neutral-200/70 dark:border-neutral-800/70 bg-white/85 dark:bg-neutral-950/65 p-4 sm:p-5">
                                <UAlert
                                        :description="t('article.detail.commentsUnavailableDescription')"
                                        :title="t('request.error.title')"
                                        color="neutral"
                                        icon="i-lucide-message-circle-off"
                                        class="w-full"
                                        variant="subtle"
                                />
                            </div>
                        </div>

                        <div v-else class="space-y-3">
                            <div v-if="userStore.isLogin" class="rounded-xl bg-neutral-100/70 dark:bg-neutral-900/45 p-1">
                                <ArticleCommentForm
                                        :submitting="submittingComment"
                                        @comment:submit="submitRootComment"
                                />
                            </div>

                            <UAlert
                                    v-else
                                    :title="t('article.detail.loginRequired')"
                                    color="primary"
                                    variant="soft"
                            >
                                <template #description>
                                    <RouterLink :to="loginRoute" class="underline">
                                        {{ t("views.common.user.login") }}
                                    </RouterLink>
                                </template>
                            </UAlert>

                            <ArticleCommentList
                                    :active-reply-id="activeReplyId"
                                    :article-author-id="article.userId"
                                    :can-reply="userStore.isLogin"
                                    :comments="commentThread"
                                    :profiles="profileMap"
                                    :submitting="submittingComment"
                                    :visible-count="visibleCommentCount"
                                    @comments:show-more="loadMoreComments"
                                    @reply:request="selectReplyTarget"
                                    @reply:submit="submitChildComment"
                                    @reply:cancel="clearReplyTarget"
                            />
                        </div>
                    </section>
                </div>

                <aside class="xl:sticky xl:top-[calc(var(--ui-header-height)+1rem)] space-y-4">
                    <section class="rounded-2xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 p-5 space-y-4">
                        <p class="text-xs uppercase tracking-[0.14em] text-neutral-500 dark:text-neutral-400">
                            {{ t("article.detail.metaPanelTitle") }}
                        </p>

                        <RouterLink
                                :to="articleAuthorHomeRoute"
                                :aria-label="t('article.detail.viewAuthorSpace', { name: articleAuthorName })"
                                class="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-neutral-100/60 dark:hover:bg-neutral-900/60 transition-colors"
                        >
                            <UAvatar
                                    :src="articleAuthor?.avatar || undefined"
                                    :text="articleAuthorInitial"
                                    class="ring-1 ring-neutral-200 dark:ring-neutral-800"
                                    size="sm"
                            />
                            <div class="leading-tight min-w-0">
                                <p class="text-sm font-medium text-neutral-800 dark:text-neutral-100 truncate">
                                    {{ articleAuthorName }}
                                </p>
                                <p class="text-xs text-neutral-500 dark:text-neutral-400 truncate">
                                    {{ articleAuthorSubline }}
                                </p>
                            </div>
                        </RouterLink>

                        <div class="grid grid-cols-2 gap-2">
                            <div class="rounded-lg bg-neutral-100/70 dark:bg-neutral-900/45 p-3 space-y-1">
                                <p class="text-xs text-neutral-500 dark:text-neutral-400">{{ t("article.detail.createdAt") }}</p>
                                <p class="text-sm font-medium text-neutral-900 dark:text-neutral-100">{{ articleCreatedAt }}</p>
                            </div>
                            <div class="rounded-lg bg-neutral-100/70 dark:bg-neutral-900/45 p-3 space-y-1">
                                <p class="text-xs text-neutral-500 dark:text-neutral-400">{{ t("article.detail.commentsTitle") }}</p>
                                <p class="text-sm font-medium text-neutral-900 dark:text-neutral-100">{{ commentCountText }}</p>
                            </div>
                        </div>

                        <div class="grid grid-cols-2 gap-2">
                            <UButton color="primary" variant="soft" size="sm" class="justify-center" icon="i-lucide-message-circle" @click="scrollToComments">
                                {{ t("article.detail.openDiscussion") }}
                            </UButton>
                            <UButton color="neutral" variant="ghost" size="sm" class="justify-center" icon="i-lucide-arrow-up" @click="scrollToTop">
                                {{ t("article.detail.jumpToTop") }}
                            </UButton>
                        </div>
                    </section>

                    <section class="rounded-2xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 p-4 space-y-2">
                        <p class="text-sm font-medium text-neutral-800 dark:text-neutral-100">
                            {{ t("article.detail.commentsTitle") }}
                        </p>
                        <p class="text-xs text-neutral-500 dark:text-neutral-400">
                            {{ commentCount > 0 ? t("article.detail.showMoreComments") : t("article.detail.commentsEmpty") }}
                        </p>
                    </section>
                </aside>
            </div>
        </template>
    </div>
</template>
