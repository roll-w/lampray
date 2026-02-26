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
    submit: [message: string];
    cancel: [];
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
    emit("submit", normalized);
    message.value = "";
}
</script>

<template>
    <div class="space-y-3 rounded-xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 p-3">
        <UTextarea
                v-model="message"
                :rows="3"
                :disabled="disabled || submitting"
                :placeholder="t('article.detail.commentPlaceholder')"
                :ui="{ base: 'border-neutral-200 dark:border-neutral-800 rounded-lg' }"
                autoresize
        />

        <div class="flex items-center gap-2">
            <UButton
                    :loading="submitting"
                    :disabled="!canSubmit"
                    color="neutral"
                    @click="onSubmit"
            >
                {{ t("article.detail.commentSubmit") }}
            </UButton>

            <UButton
                    v-if="showCancel"
                    variant="ghost"
                    color="neutral"
                    :disabled="submitting"
                    @click="emit('cancel')"
            >
                {{ t("article.detail.cancelReply") }}
            </UButton>
        </div>
    </div>
</template>
