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
import { ReviewVerdict } from "@/services/content/review.type";
import { useI18n } from "vue-i18n";

/**
 * @author RollW
 */

const props = defineProps<{
    summary: string;
    loading?: boolean;
    disabled?: boolean;
}>();

const emit = defineEmits<{
    (e: 'update:summary', value: string): void;
    (e: 'submit', verdict: ReviewVerdict): void;
}>();

const { t } = useI18n();
</script>

<template>
    <div class="space-y-6">
        <UFormField :label="t('views.adminfaced.review.reviewSummary')">
            <UTextarea
                :model-value="summary"
                @update:model-value="val => emit('update:summary', val)"
                :disabled="loading"
                :rows="4"
                :placeholder="t('views.adminfaced.review.reviewSummaryPlaceholder')"
                class="w-full"
                :ui="{ rounded: 'rounded-xl', base: 'bg-neutral-50 dark:bg-neutral-900 border-neutral-200 dark:border-neutral-800 focus:ring-primary-500' }"
            />
        </UFormField>

        <div class="grid grid-cols-2 gap-4">
            <UButton
                :loading="loading"
                :disabled="disabled"
                block
                size="lg"
                color="primary"
                class="rounded-xl font-bold shadow-none"
                @click="emit('submit', ReviewVerdict.APPROVED)"
            >
                <template #leading>
                    <UIcon name="i-lucide-check-circle-2" />
                </template>
                {{ t('views.adminfaced.review.reviewVerdicts.APPROVED') }}
            </UButton>
            
            <UButton
                :loading="loading"
                :disabled="disabled"
                block
                size="lg"
                color="error"
                variant="soft"
                class="rounded-xl font-bold shadow-none"
                @click="emit('submit', ReviewVerdict.REJECTED)"
            >
                <template #leading>
                    <UIcon name="i-lucide-x-circle" />
                </template>
                {{ t('views.adminfaced.review.reviewVerdicts.REJECTED') }}
            </UButton>
        </div>
    </div>
</template>
