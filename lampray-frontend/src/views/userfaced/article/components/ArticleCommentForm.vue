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
import {computed, ref} from "vue";
import {useI18n} from "vue-i18n";

interface Props {
    submitting?: boolean;
    disabled?: boolean;
    showCancel?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    submitting: false,
    disabled: false,
    showCancel: false,
});

const emit = defineEmits<{
    "comment:submit": [message: string];
    "comment:cancel": [];
}>();

const {t} = useI18n();
const message = ref("");

const canSubmit = computed(() => {
    return !props.disabled && !props.submitting && message.value.trim().length > 0;
});

function onSubmit(): void {
    const normalized = message.value.trim();
    if (!normalized) {
        return;
    }
    emit("comment:submit", normalized);
    message.value = "";
}
</script>

<template>
    <div class="space-y-3 rounded-xl bg-neutral-100/70 dark:bg-neutral-900/45 p-3">
        <UTextarea
                v-model="message"
                :disabled="disabled || submitting"
                :placeholder="t('article.detail.commentPlaceholder')"
                :rows="3"
                :ui="{ base: 'border-neutral-200/80 dark:border-neutral-700/70 rounded-lg bg-white/80 dark:bg-neutral-950/60' }"
                autoresize
                class="w-full"
        />

        <div class="flex items-center gap-2">
            <UButton
                    :disabled="!canSubmit"
                    :loading="submitting"
                    color="primary"
                    @click="onSubmit"
            >
                {{ t("article.detail.commentSubmit") }}
            </UButton>

            <UButton
                    v-if="showCancel"
                    :disabled="submitting"
                    color="neutral"
                    variant="ghost"
                    @click="emit('comment:cancel')"
            >
                {{ t("article.detail.cancelReply") }}
            </UButton>
        </div>
    </div>
</template>
