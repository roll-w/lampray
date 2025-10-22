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
import {RouteName} from "@/router/routeName.ts";
import {useRouter} from "vue-router";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {useUserStore} from "@/stores/user.ts";

const {t} = useI18n();

const axios = useAxios();
const toast = useToast();
const router = useRouter();
const userStore = useUserStore();

// username / email / password regex adapted from backend UserChecker.java
const USERNAME_REGEX = /^[a-zA-Z_\-][\w.\-]{2,19}$/; // 3-20 chars, start with letter/_/-
const PASSWORD_REGEX = /^[A-Za-z\d._\-~!@#$^&*+=<>%;'"\\/|()\[\]{}]{4,20}$/; // 4-20 allowed characters
const EMAIL_REGEX = /^\w+([-+. ]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/; // simplified email regex

const loginSchema = z.object({
    identity: z.string()
        .min(1, t("views.userfaced.user.login.identityRequired"))
        .refine(val => {
            if (!val) return false;
            const v = val.trim();
            if (v.includes("@")) {
                return EMAIL_REGEX.test(v);
            }
            return USERNAME_REGEX.test(v);
        }, {
            message: t("views.userfaced.user.login.invalidIdentity")
        }),
    token: z.string()
        .min(4, t("views.userfaced.user.login.invalidPassword"))
        .max(20, t("views.userfaced.user.login.invalidPassword"))
        .regex(PASSWORD_REGEX, {message: t("views.userfaced.user.login.invalidPasswordDetail")}),
    rememberMe: z.boolean().default(false)
});
type LoginSchema = z.output<typeof loginSchema>;
const loginForm = reactive<Partial<LoginSchema>>({
    identity: "",
    token: "",
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
         const firstError = result.error.issues[0]?.message || t("request.error.title");
         toast.add(newErrorToastFromError(new Error(firstError), t("request.error.title")));
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
         toast.add(newSuccessToast(t("views.userfaced.user.login.success")));
         jumpTo();
     } catch (error: any) {
         toast.add(newErrorToastFromError(error, t("request.error.title")));
     }
 };

const onLoginReset = () => {
    loginForm.identity = "";
    loginForm.token = "";
    loginForm.rememberMe = false;
};
</script>
<template>
    <UForm :schema="loginSchema" :state="loginForm" class="space-y-4 w-full" @submit="onLoginClick">
        <UFormField :label="t('views.userfaced.user.login.identity')" name="identity" required>
            <UInput v-model="loginForm.identity" :placeholder="t('views.userfaced.user.login.identityPlaceholder')" type="text"
                    autocomplete="username" name="identity" class="w-full"/>
        </UFormField>
        <UFormField :label="t('views.common.user.password')" name="token" required>
            <UInput v-model="loginForm.token" :placeholder="t('views.common.user.passwordPlaceholder')" :type="showPassword ? 'text' : 'password'"
                    :ui="{ trailing: 'pe-1' }"
                    autocomplete="current-password" name="token" class="w-full">
                <template #trailing>
                    <UButton
                            color="neutral"
                            variant="link"
                            size="sm"
                            :icon="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                            :aria-label="showPassword ? t('views.userfaced.user.login.hidePassword') : t('views.userfaced.user.login.showPassword')"
                            :aria-pressed="showPassword"
                            aria-controls="password"
                            @click="showPassword = !showPassword"
                    />
                </template>
            </UInput>
        </UFormField>
        <UCheckbox v-model="loginForm.rememberMe" :label="t('views.userfaced.user.login.rememberMe')" name="rememberMe"/>
        <div class="flex flex-col sm:flex-row gap-2">
            <UButton type="submit" class="flex-1" color="primary">{{ t('views.userfaced.user.login.loginButton') }}</UButton>
            <UButton variant="outline" class="flex-1" @click="onLoginReset">{{ t('common.reset') }}</UButton>
        </div>
    </UForm>
    <UButton variant="link" class="self-start p-0 h-auto text-sm" @click="() => {}">{{ t('views.userfaced.user.login.forgotPassword') }}</UButton>
</template>
