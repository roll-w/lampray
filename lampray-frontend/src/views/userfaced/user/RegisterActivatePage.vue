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

const router = useRouter();
const route = router.currentRoute;
const axios = useAxios();

const activateCode = ref<string | null>(null);
const activateStatus = ref<"pending" | "loading" | "success" | "failed" | "invalid">("pending");
const message = ref<string | null>(null);


watch(() => route.value.params.code, (newCode) => {
    activateCode.value = typeof newCode === "string" ? newCode : null;
}, {immediate: true});

const requestActivate = async () => {
    activateStatus.value = "loading";
    try {
        const response = await loginRegisterService(axios).activateUser(activateCode.value!);
        activateStatus.value = "success";
    } catch (error: any) {
        activateStatus.value = "failed";
    }
};

const showResendModal = ref(false);

const resendActivateEmailSchema = z.object({
    email: z.email("Valid email required").refine(val => val !== null && val.trim() !== "", {
        message: "Email required"
    }),
    username: z.string().min(1, "Username required").refine(val => val !== null && val.trim() !== "", {
        message: "Username required"
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
                        <h1 class="text-xl font-bold text-gray-900 dark:text-white">账户激活</h1>
                        <p class="text-normal text-gray-600 dark:text-gray-400">请确认账户激活信息并完成激活。</p>
                    </div>
                </template>

                <div class="space-y-6">
                    <div class="flex flex-col gap-4">
                        <div class="text-md text-gray-700 dark:text-gray-300">
                            您的激活信息如下：
                        </div>
                        <div class="p-4 bg-neutral-200/50 dark:bg-neutral-800/50 rounded-md
                                    text-sm text-neutral-800 dark:text-neutral-200 ">
                            <p class="font-mono">{{ activateCode || "无效激活码" }}</p>
                            <p class="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
                                （激活码通常为注册时发送到您邮箱的链接中的一部分）
                            </p>
                        </div>
                        <UButton color="primary" block size="lg"
                                 :loading="activateStatus === 'loading'"
                                 :disabled="!activateCode || activateStatus !== 'pending'"
                                 @click="requestActivate()">
                            确认激活
                        </UButton>
                    </div>

                    <UAlert v-if="activateStatus === 'success'"
                            title="激活成功"
                            description="您的账户已成功激活，现在可以登录并使用所有功能。"
                            color="success"
                            variant="soft"
                            icon="i-lucide-check-circle"
                            class="mt-2"
                    />
                    <UAlert v-else-if="activateStatus === 'failed'"
                            title="激活失败"
                            description="激活码无效或已过期，请重试或重新发送激活邮件。"
                            color="error"
                            variant="soft"
                            icon="i-lucide-x-circle"
                            class="mt-2"
                    >
                        <template #actions>
                            <div class="flex flex-col sm:flex-row gap-2">
                                <UModal v-model:open="showResendModal" :close-on-escape="true" :close-on-click-away="true"
                                        title="重新发送激活邮件"
                                        size="md">
                                    <template #body>
                                        <div class="space-y-4">
                                            <UForm :schema="resendActivateEmailSchema" :state="resendActivateEmailForm"
                                                   class="space-y-4 w-full">
                                                <UFormField label="用户名" name="username" required>
                                                    <UInput v-model="resendActivateEmailForm.username"
                                                            placeholder="输入用户名" type="text"
                                                            autocomplete="username"
                                                            name="username" class="w-full"/>
                                                </UFormField>
                                                <UFormField label="电子邮件" name="email" required>
                                                    <UInput v-model="resendActivateEmailForm.email"
                                                            placeholder="输入电子邮件" type="email"
                                                            autocomplete="email"
                                                            name="email" class="w-full"/>
                                                </UFormField>
                                            </UForm>
                                        </div>
                                    </template>

                                    <template #footer>
                                        <div class="flex justify-end w-full gap-2">
                                            <UButton variant="outline" color="secondary" @click="showResendModal = false">取消</UButton>
                                            <UButton color="primary" @click="() => {
                                                    requestResendActivateEmail();
                                                showResendModal = false;
                                            }">发送</UButton>
                                        </div>
                                    </template>
                                    <UButton color="error" variant="solid" size="md" block
                                             @click="showResendModal = true">
                                        重新发送激活邮件
                                    </UButton>
                                </UModal>
                            </div>
                        </template>
                    </UAlert>
                    <UAlert v-else-if="activateStatus === 'invalid'"
                            title="无效的激活码"
                            description="未检测到有效激活码，请检查链接或重新注册。"
                            color="warning"
                            variant="soft"
                            icon="i-lucide-alert-triangle"
                            class="mt-2"
                    />
                </div>

                <template #footer>
                    <div class="text-center py-2">
                        <p class="text-xs text-gray-500 dark:text-gray-400">
                            如遇问题请
                            <UButton variant="link" size="xs" color="primary" class="p-0 h-auto">
                                联系客服
                            </UButton>
                            获取帮助。
                        </p>
                    </div>
                </template>
            </UCard>
        </div>
    </div>


</template>

<style scoped>
</style>