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
import type {StructuralText} from "@/components/structuraltext/types.ts";
import ArticleCommentForm from "@/views/userfaced/article/components/ArticleCommentForm.vue";

defineOptions({
    name: "ArticleCommentItem",
});

interface Props {
    node: CommentThreadNode;
    canReply: boolean;
    activeReplyId: number | null;
    submitting: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
    requestReply: [id: number];
    submitReply: [payload: { parentId: number; message: string }];
    cancelReply: [];
}>();

const {t} = useI18n();

const isReplying = computed(() => props.activeReplyId === props.node.id);

function toPlainText(node: StructuralText): string {
    const chunks: string[] = [];

    const walk = (current: StructuralText): void => {
        if (current.content) {
            chunks.push(current.content);
        }
        if (!current.children.length) {
            return;
        }
        for (const child of current.children) {
            walk(child);
        }
    };

    walk(node);
    return chunks.join(" ").trim();
}

const plainText = computed(() => toPlainText(props.node.content));

function formatTime(time: string): string {
    return new Date(time).toLocaleString();
}

function submitReply(message: string): void {
    emit("submitReply", {
        parentId: props.node.id,
        message,
    });
}
</script>

<template>
    <article class="rounded-xl border border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/40 p-4 space-y-3">
        <header class="flex items-center justify-between text-[11px] uppercase tracking-wide text-neutral-500 dark:text-neutral-400">
            <span>#{{ node.userId }}</span>
            <span>{{ formatTime(node.createTime) }}</span>
        </header>

        <p class="text-sm leading-6 text-neutral-800 dark:text-neutral-100 whitespace-pre-wrap break-words">{{ plainText }}</p>

        <div class="flex items-center gap-2">
            <UBadge v-if="node.pending" color="warning" variant="soft" size="xs">pending</UBadge>
            <UBadge v-if="node.failed" color="error" variant="soft" size="xs">failed</UBadge>

            <UButton
                    v-if="canReply"
                    variant="ghost"
                    color="neutral"
                    size="xs"
                    :disabled="submitting"
                    @click="emit('requestReply', node.id)"
            >
                {{ t("article.detail.reply") }}
            </UButton>
        </div>

        <div v-if="isReplying" class="rounded-lg border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 p-3 space-y-2">
            <p class="text-xs text-neutral-500 dark:text-neutral-400">{{ t("article.detail.replyingTo") }} #{{ node.userId }}</p>
            <ArticleCommentForm
                    :show-cancel="true"
                    :submitting="submitting"
                    @submit="submitReply"
                    @cancel="emit('cancelReply')"
            />
        </div>

        <div v-if="node.replies.length > 0" class="pl-4 border-l border-neutral-200 dark:border-neutral-800 space-y-3">
            <ArticleCommentItem
                    v-for="reply in node.replies"
                    :key="reply.id"
                    :node="reply"
                    :can-reply="canReply"
                    :active-reply-id="activeReplyId"
                    :submitting="submitting"
                    @request-reply="(id) => emit('requestReply', id)"
                    @submit-reply="(payload) => emit('submitReply', payload)"
                    @cancel-reply="emit('cancelReply')"
            />
        </div>
    </article>
</template>
