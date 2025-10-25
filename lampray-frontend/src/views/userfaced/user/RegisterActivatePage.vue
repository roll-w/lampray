<!--
  - Copyright (C) 2023-2025 RollW
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
import {ref, watch} from "vue";
import {useRouter} from "vue-router";
import {loginRegisterService} from "@/services/user/user.service.ts";
import {useAxios} from "@/composables/useAxios.ts";
import * as z from "zod";
import {useI18n} from "vue-i18n";

const router = useRouter();
const route = router.currentRoute;
const axios = useAxios();
const {t} = useI18n()

const activateCode = ref<string | null>(null);
const activateStatus = ref<"pending" | "loading" | "success" | "failed" | "invalid">("pending");
const message = ref<string | null>(null);


watch(() => route.value.params.code, (newCode) => {
    activateCode.value = typeof newCode === "string" ? newCode : null;
}, {immediate: true});

const requestActivate = async () => {
    activateStatus.value = "loading";
    try {
        await loginRegisterService(axios).activateUser(activateCode.value!);
        activateStatus.value = "success";
    } catch (error: any) {
        activateStatus.value = "failed";
    }
};

const showResendModal = ref(false);

// handler to resend and close modal
const handleResendConfirm = async () => {
    await requestResendActivateEmail();
    showResendModal.value = false;
};

const resendActivateEmailSchema = z.object({
    email: z.email(t("views.common.user.emailPlaceholder")).refine(val => val !== null && val.trim() !== "", {
        message: t("views.common.user.emailPlaceholder")
    }),
    username: z.string().min(1, t("views.common.user.usernamePlaceholder")).refine(val => val !== null && val.trim() !== "", {
        message: t("views.common.user.usernamePlaceholder")
    }),
});

type ResendActivateEmailSchema = z.output<typeof resendActivateEmailSchema>;

const resendActivateEmailForm = ref<Partial<ResendActivateEmailSchema>>({
    email: '',
    username: ''
});

const requestResendActivateEmail = async () => {
    try {
        await loginRegisterService(axios).resendActivationEmail(
                resendActivateEmailSchema.parse(resendActivateEmailForm.value)
        );
    } catch (error: any) {
    }
};

</script>

<template>
    <div class="min-h-[80vh] flex items-center justify-center p-4">
        <div class="max-w-md mx-auto w-full">
            <UCard variant="outline"
                   class="rounded-lg bg-linear-to-br from-blue-50 via-default to-amber-50 dark:from-blue-950/50 dark:via-default dark:to-amber-950/50">
                <template #header>
                    <div class="flex flex-col items-center gap-2 py-4">
                        <div class="w-14 h-14 rounded-full bg-gradient-to-br flex items-center justify-center">
                            <UIcon name="i-lucide-shield-check" class="w-7 h-7 text-primary"/>
                        </div>
                        <h1 class="text-xl font-bold text-gray-900 dark:text-white">
                            {{ t('views.userfaced.user.registerActivate.title') }}</h1>
                        <p class="text-normal text-gray-600 dark:text-gray-400">
                            {{ t('views.userfaced.user.registerActivate.subtitle') }}</p>
                    </div>
                </template>

                <div class="space-y-6">
                    <div class="flex flex-col gap-4">
                        <div class="text-md text-gray-700 dark:text-gray-300">
                            {{ t('views.userfaced.user.registerActivate.infoIntro') }}
                        </div>
                        <div class="p-4 bg-neutral-200/50 dark:bg-neutral-800/50 rounded-md
                                     text-sm text-neutral-800 dark:text-neutral-200 ">
                            <p class="font-mono">
                                {{ activateCode || t('views.userfaced.user.registerActivate.invalidCode') }}</p>
                            <p class="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
                                {{ t('views.userfaced.user.registerActivate.codeNote') }}
                            </p>
                        </div>
                        <UButton color="primary" block size="lg"
                                 :loading="activateStatus === 'loading'"
                                 :disabled="!activateCode || activateStatus !== 'pending'"
                                 @click="requestActivate()">
                            {{ t('views.userfaced.user.registerActivate.confirmButton') }}
                        </UButton>
                    </div>

                    <UAlert v-if="activateStatus === 'success'"
                            :title="t('views.userfaced.user.registerActivate.successTitle')"
                            :description="t('views.userfaced.user.registerActivate.successDescription')"
                            color="success"
                            variant="soft"
                            icon="i-lucide-check-circle"
                            class="mt-2"
                    />
                    <UAlert v-else-if="activateStatus === 'failed'"
                            :title="t('views.userfaced.user.registerActivate.failedTitle')"
                            :description="t('views.userfaced.user.registerActivate.failedDescription')"
                            color="error"
                            variant="soft"
                            icon="i-lucide-x-circle"
                            class="mt-2"
                    >
                        <template #actions>
                            <div class="flex flex-col sm:flex-row gap-2">
                                <UModal v-model:open="showResendModal" :close-on-escape="true"
                                        :close-on-click-away="true"
                                        :title="t('views.userfaced.user.registerActivate.resendModalTitle')"
                                        size="md">
                                    <template #body>
                                        <div class="space-y-4">
                                            <UForm :schema="resendActivateEmailSchema" :state="resendActivateEmailForm"
                                                   class="space-y-4 w-full">
                                                <UFormField :label="t('views.common.user.username')" name="username"
                                                            required>
                                                    <UInput v-model="resendActivateEmailForm.username"
                                                            :placeholder="t('views.common.user.usernamePlaceholder')"
                                                            type="text"
                                                            autocomplete="username"
                                                            name="username" class="w-full"/>
                                                </UFormField>
                                                <UFormField :label="t('views.common.user.email')" name="email" required>
                                                    <UInput v-model="resendActivateEmailForm.email"
                                                            :placeholder="t('views.common.user.emailPlaceholder')"
                                                            type="email"
                                                            autocomplete="email"
                                                            name="email" class="w-full"/>
                                                </UFormField>
                                            </UForm>
                                        </div>
                                    </template>

                                    <template #footer>
                                        <div class="flex justify-end w-full gap-2">
                                            <UButton variant="outline" color="secondary"
                                                     @click="showResendModal = false">{{ t('common.cancel') }}
                                            </UButton>
                                            <UButton color="primary" @click="handleResendConfirm">{{
                                                    t('common.submit')
                                                }}
                                            </UButton>
                                        </div>
                                    </template>
                                    <UButton color="error" variant="solid" size="md" block
                                             @click="showResendModal = true">
                                        {{ t('views.userfaced.user.registerActivate.resendButton') }}
                                    </UButton>
                                </UModal>
                            </div>
                        </template>
                    </UAlert>
                    <UAlert v-else-if="activateStatus === 'invalid'"
                            :title="t('views.userfaced.user.registerActivate.invalidTitle')"
                            :description="t('views.userfaced.user.registerActivate.invalidDescription')"
                            color="warning"
                            variant="soft"
                            icon="i-lucide-alert-triangle"
                            class="mt-2"
                    />
                </div>

                <template #footer>
                    <div class="text-center py-2">
                        <p class="text-xs text-gray-500 dark:text-gray-400">
                            {{ t('views.common.support.contactSupportPrefix') }}
                            <UButton variant="link" size="xs" color="primary" class="p-0 h-auto">
                                {{ t('views.common.support.contactSupport') }}
                            </UButton>
                            {{ t('views.common.support.contactSupportSuffix') }}
                        </p>
                    </div>
                </template>
            </UCard>
        </div>
    </div>
</template>
