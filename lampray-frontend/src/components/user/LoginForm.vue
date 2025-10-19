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
import {reactive, ref} from "vue";
import * as z from "zod";
import {loginRegisterService} from "@/services/user/user.service.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {RouteName} from "@/router/routeName.ts";
import {useRouter} from "vue-router";
import {useUserStore} from "@/stores/user.ts";

const router = useRouter();
const userStore = useUserStore();
const axios = useAxios();
const toast = useToast();

const loginSchema = z.object({
    identity: z.string().min(1, "Username or email required").refine(val => val !== null && val.trim() !== "", {
        message: "Username or email required"
    }),
    token: z.string().min(1, "Password required").refine(val => val !== null && val.trim() !== "", {
        message: "Password required"
    }),
    rememberMe: z.boolean().default(false)
});
type LoginSchema = z.output<typeof loginSchema>;
const loginForm = reactive<Partial<LoginSchema>>({
    identity: '',
    token: '',
    rememberMe: false
});

const showPassword = ref(false);

const jumpTo = () => {
    const source = router.currentRoute.value.query.source;
    if (source) {
        const url = decodeURIComponent(source.toString());
        window.location.replace(url);
    } else {
        router.push({name: RouteName.USER_HOME});
    }
};

const onLoginClick = async () => {
    const result = loginSchema.safeParse(loginForm);
    if (!result.success) {
        const firstError = result.error.message;
        toast.add({
            title: 'Request Error',
            orientation: 'horizontal',
            color: 'error',
            description: firstError,
            progress: false
        });
        return;
    }
    try {
        const response = await loginRegisterService(axios).loginByPassword({
            identity: loginForm.identity!,
            token: loginForm.token!,
        });
        const body = response.data;
        const data = body.data!;
        userStore.loginUser(data.user, {
            accessToken: data.accessToken,
            refreshToken: data.refreshToken,
            prefix: "Bearer ",
            accessTokenExpiry: new Date(data.accessTokenExpiry),
            refreshTokenExpiry: new Date(data.refreshTokenExpiry),
        }, loginForm.rememberMe!, false);
        toast.add({
            title: 'Login Success',
            orientation: 'vertical',
            color: 'success',
            progress: false
        });
        jumpTo();
    } catch (error: any) {
        // TODO: toast template
        toast.add({
            title: 'Request Error',
            orientation: 'horizontal',
            color: 'error',
            description: error.message || 'Login failed',
            progress: false
        });
    }
};

const onLoginReset = () => {
    loginForm.identity = '';
    loginForm.token = '';
    loginForm.rememberMe = false;
};
</script>
<template>
    <UForm :schema="loginSchema" :state="loginForm" class="space-y-4 w-full" @submit="onLoginClick">
        <UFormField label="用户名/邮箱" name="identity" required>
            <UInput v-model="loginForm.identity" placeholder="输入用户名或邮箱" type="text"
                    autocomplete="username" name="identity" class="w-full"/>
        </UFormField>
        <UFormField label="密码" name="token" required>
            <UInput v-model="loginForm.token" placeholder="输入密码" :type="showPassword ? 'text' : 'password'"
                    :ui="{ trailing: 'pe-1' }"
                    autocomplete="current-password" name="token" class="w-full">
                <template #trailing>
                    <UButton
                            color="neutral"
                            variant="link"
                            size="sm"
                            :icon="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                            :aria-label="showPassword ? 'Hide password' : 'Show password'"
                            :aria-pressed="showPassword"
                            aria-controls="password"
                            @click="showPassword = !showPassword"
                    />
                </template>
            </UInput>
        </UFormField>
        <UCheckbox v-model="loginForm.rememberMe" label="记住我" name="rememberMe"/>
        <div class="flex flex-col sm:flex-row gap-2">
            <UButton type="submit" class="flex-1" color="primary">登录</UButton>
            <UButton variant="outline" class="flex-1" @click="onLoginReset">重置</UButton>
        </div>
    </UForm>
    <UButton variant="link" class="self-start p-0 h-auto text-sm" @click="() => {}">忘记密码？</UButton>
</template>

