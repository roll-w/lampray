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
import {computed, inject} from "vue";
import {useI18n} from "vue-i18n";
import ArticleCommentForm from "@/views/userfaced/article/components/ArticleCommentForm.vue";
import {articleCommentContextKey} from "@/views/userfaced/article/components/commentContext.ts";
import {extractPlainText} from "@/views/userfaced/article/utils/structuralText.ts";
import {RouteName} from "@/router/routeName.ts";
import type {CommentThreadNode} from "@/views/userfaced/article/types/commentThread.ts";

defineOptions({
    name: "ArticleCommentItem",
});

interface Props {
    node: CommentThreadNode;
}

const props = defineProps<Props>();

const emit = defineEmits<{
    "reply:request": [id: number];
    "reply:submit": [payload: { parentId: number; message: string }];
    "reply:cancel": [];
}>();

const {t} = useI18n();
const commentContext = inject(articleCommentContextKey);

if (!commentContext) {
    throw new Error("ArticleCommentItem requires article comment context.");
}

const isReplying = computed(() => commentContext.activeReplyId.value === props.node.id);
const isArticleAuthor = computed(() => props.node.userId === commentContext.articleAuthorId.value);
const author = computed(() => commentContext.profiles.value.get(props.node.userId) ?? null);
const canReply = computed(() => commentContext.canReply.value);
const isSubmitting = computed(() => commentContext.submitting.value);
const authorProfileRoute = computed(() => {
    return {
        name: RouteName.USER_SPACE,
        params: {
            id: props.node.userId,
        },
    };
});

const authorName = computed(() => {
    if (author.value?.nickname) {
        return author.value.nickname;
    }
    if (author.value?.username) {
        return author.value.username;
    }
    return `#${props.node.userId}`;
});
const authorSubline = computed(() => {
    if (author.value?.username) {
        return `@${author.value.username}`;
    }
    return t("article.detail.unknownAuthor");
});
const authorInitial = computed(() => {
    const value = authorName.value;
    if (value.length === 0) {
        return "?";
    }
    return value[0]!;
});

const plainText = computed(() => extractPlainText(props.node.content));

function formatTime(time: string): string {
    return new Date(time).toLocaleString();
}

function submitReply(message: string): void {
    emit("reply:submit", {
        parentId: props.node.id,
        message,
    });
}
</script>

<template>
    <article class="rounded-xl bg-white/80 dark:bg-neutral-950/35 p-4 space-y-3">
        <header class="flex items-start justify-between gap-4">
            <div class="flex items-center gap-3 min-w-0">
                <RouterLink
                        :to="authorProfileRoute"
                        :aria-label="t('article.detail.viewAuthorSpace', { name: authorName })"
                        class="flex items-center gap-3 min-w-0 cursor-pointer"
                >
                    <UAvatar
                            :src="author?.avatar || undefined"
                            :text="authorInitial"
                            size="sm"
                            class="ring-1 ring-neutral-200 dark:ring-neutral-800"
                    />
                    <div class="min-w-0">
                        <p class="text-sm font-semibold text-neutral-900 dark:text-neutral-100 truncate hover:text-primary-600 dark:hover:text-primary-400 transition-colors">
                            {{ authorName }}
                        </p>
                        <p class="text-xs text-neutral-500 dark:text-neutral-400 truncate">{{ authorSubline }}</p>
                    </div>
                </RouterLink>
                <UBadge v-if="isArticleAuthor" color="primary" variant="soft" size="xs">{{ t("article.detail.authorBadge") }}</UBadge>
            </div>

            <span class="text-xs text-neutral-500 dark:text-neutral-400 whitespace-nowrap">{{ formatTime(node.createTime) }}</span>
        </header>

        <p class="text-sm leading-6 text-neutral-800 dark:text-neutral-100 whitespace-pre-wrap break-words">{{ plainText }}</p>

        <div class="flex items-center gap-2">
            <UBadge v-if="node.pending" color="warning" variant="soft" size="xs">pending</UBadge>
            <UBadge v-if="node.failed" color="error" variant="soft" size="xs">failed</UBadge>

            <UButton
                    v-if="canReply"
                    variant="ghost"
                    color="primary"
                    size="xs"
                    :disabled="isSubmitting"
                    @click="emit('reply:request', node.id)"
            >
                {{ t("article.detail.reply") }}
            </UButton>
        </div>

        <div v-if="isReplying" class="rounded-lg bg-neutral-100/80 dark:bg-neutral-900/55 p-3 space-y-2">
            <p class="text-xs text-neutral-500 dark:text-neutral-400">{{ t("article.detail.replyingTo") }} {{ authorName }}</p>
            <ArticleCommentForm
                    :show-cancel="true"
                    :submitting="isSubmitting"
                    @comment:submit="submitReply"
                    @comment:cancel="emit('reply:cancel')"
            />
        </div>

        <div v-if="node.replies.length > 0" class="pl-3 sm:pl-5 border-l border-neutral-200/70 dark:border-neutral-700/60 space-y-2">
            <ArticleCommentItem
                    v-for="reply in node.replies"
                    :key="reply.id"
                    :node="reply"
                    @reply:request="emit('reply:request', $event)"
                    @reply:submit="emit('reply:submit', $event)"
                    @reply:cancel="emit('reply:cancel')"
            />
        </div>
    </article>
</template>
