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
import {computed} from "vue";
import {useI18n} from "vue-i18n";
import type {CommentThreadNode} from "@/services/content/comment.type.ts";
import ArticleCommentItem from "@/views/userfaced/article/components/ArticleCommentItem.vue";

interface Props {
    comments: CommentThreadNode[];
    visibleCount: number;
    canReply: boolean;
    activeReplyId: number | null;
    submitting: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
    showMore: [];
    requestReply: [id: number];
    submitReply: [payload: { parentId: number; message: string }];
    cancelReply: [];
}>();

const {t} = useI18n();

const visibleComments = computed(() => {
    return props.comments.slice(0, props.visibleCount);
});

const hasMore = computed(() => props.visibleCount < props.comments.length);
</script>

<template>
    <div class="space-y-3">
        <p v-if="comments.length === 0" class="text-sm text-neutral-500 dark:text-neutral-400">
            {{ t("article.detail.commentsEmpty") }}
        </p>

        <ArticleCommentItem
                v-for="comment in visibleComments"
                :key="comment.id"
                :node="comment"
                :can-reply="canReply"
                :active-reply-id="activeReplyId"
                :submitting="submitting"
                @request-reply="(id) => emit('requestReply', id)"
                @submit-reply="(payload) => emit('submitReply', payload)"
                @cancel-reply="emit('cancelReply')"
        />

        <UButton
                v-if="hasMore"
                variant="outline"
                color="neutral"
                @click="emit('showMore')"
        >
            {{ t("article.detail.showMoreComments") }}
        </UButton>
    </div>
</template>
