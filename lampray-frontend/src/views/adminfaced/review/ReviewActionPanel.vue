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
import {ReviewVerdict} from "@/services/content/review.type";
import {useI18n} from "vue-i18n";

const props = defineProps<{
    summary: string;
    loading?: boolean;
    disabled?: boolean;
}>();

const emit = defineEmits<{
    (e: 'update:summary', value: string): void;
    (e: 'submit', verdict: ReviewVerdict): void;
}>();

const {t} = useI18n();
</script>

<template>
    <div class="space-y-6">
        <div>
            <h3 class="text-xs font-semibold uppercase tracking-widest pb-4">
                {{ t('views.adminfaced.review.reviewSummary') }}
            </h3>
            <UTextarea
                    :disabled="loading"
                    :model-value="summary"
                    :placeholder="t('views.adminfaced.review.reviewSummaryPlaceholder')"
                    :rows="4"
                    :ui="{ rounded: 'rounded-md', base: 'border-neutral-200 dark:border-neutral-800 focus:ring-primary-500 shadow-none' }"
                    class="w-full"
                    @update:model-value="val => emit('update:summary', val)"
            />
        </div>

        <div class="grid grid-cols-2 gap-2">
            <UButton
                    :disabled="disabled"
                    :loading="loading"
                    block
                    class="rounded-lg font-bold shadow-none"
                    color="primary"
                    size="lg"
                    @click="emit('submit', ReviewVerdict.APPROVED)"
            >
                <template #leading>
                    <UIcon name="i-lucide-check-circle-2"/>
                </template>
                {{ t('views.adminfaced.review.reviewVerdicts.APPROVED') }}
            </UButton>

            <UButton
                    :disabled="disabled"
                    :loading="loading"
                    block
                    class="rounded-lg font-bold shadow-none"
                    color="error"
                    size="lg"
                    variant="soft"
                    @click="emit('submit', ReviewVerdict.REJECTED)"
            >
                <template #leading>
                    <UIcon name="i-lucide-x-circle"/>
                </template>
                {{ t('views.adminfaced.review.reviewVerdicts.REJECTED') }}
            </UButton>
        </div>
    </div>
</template>
