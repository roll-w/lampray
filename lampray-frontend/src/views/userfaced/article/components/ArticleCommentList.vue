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
import {computed, provide, toRef} from "vue";
import {useI18n} from "vue-i18n";
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";
import ArticleCommentItem from "@/views/userfaced/article/components/ArticleCommentItem.vue";
import {articleCommentContextKey} from "@/views/userfaced/article/components/commentContext.ts";
import type {CommentThreadNode} from "@/views/userfaced/article/types/commentThread.ts";

interface Props {
    comments: CommentThreadNode[];
    visibleCount: number;
    profiles: Map<number, UserCommonDetailsVo | null>;
    articleAuthorId: number;
    canReply: boolean;
    activeReplyId: number | null;
    submitting: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
    "comments:show-more": [];
    "reply:request": [id: number];
    "reply:submit": [payload: { parentId: number; message: string }];
    "reply:cancel": [];
}>();

const {t} = useI18n();

provide(articleCommentContextKey, {
    profiles: toRef(props, "profiles"),
    articleAuthorId: toRef(props, "articleAuthorId"),
    canReply: toRef(props, "canReply"),
    activeReplyId: toRef(props, "activeReplyId"),
    submitting: toRef(props, "submitting"),
});

const visibleComments = computed(() => props.comments.slice(0, props.visibleCount));
const hasMore = computed(() => props.visibleCount < props.comments.length);
</script>

<template>
    <div class="space-y-2">
        <p v-if="comments.length === 0" class="text-sm text-neutral-500 dark:text-neutral-400 px-1">
            {{ t("article.detail.commentsEmpty") }}
        </p>

        <ArticleCommentItem
                v-for="comment in visibleComments"
                :key="comment.id"
                :node="comment"
                @reply:request="emit('reply:request', $event)"
                @reply:submit="emit('reply:submit', $event)"
                @reply:cancel="emit('reply:cancel')"
        />

        <UButton
                v-if="hasMore"
                variant="ghost"
                color="primary"
                :disabled="submitting"
                @click="emit('comments:show-more')"
        >
            {{ t("article.detail.showMoreComments") }}
        </UButton>
    </div>
</template>
