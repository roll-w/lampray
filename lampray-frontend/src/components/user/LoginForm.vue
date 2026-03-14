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
import {reactive, ref, useTemplateRef, watch} from "vue";
import * as z from "zod";
import {loginRegisterService} from "@/services/user/user.service.ts";
import {RouteName} from "@/router/routeName.ts";
import {useRouter} from "vue-router";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {useUserStore} from "@/stores/user.ts";
import {PASSWORD_REGEX, USERNAME_REGEX} from "@/components/user/constants.ts";

const {t, locale} = useI18n();

const axios = useAxios();
const toast = useToast();
const router = useRouter();
const userStore = useUserStore();

const createLoginSchema = () => z.object({
    identity: z.string()
            .min(1, t("views.userfaced.user.login.identityRequired"))
            .refine(val => {
                if (!val) return false;
                const v = val.trim();
                if (v.includes("@")) {
                    return z.email().safeParse(v).success;
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

const loginSchema = ref(createLoginSchema());
const form = useTemplateRef("form")

watch(locale, async () => {
    loginSchema.value = createLoginSchema();
    // TODO: fix form error message update
    form.value?.clear()
}, {immediate: true});

type LoginSchema = z.output<ReturnType<typeof createLoginSchema>>;
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
        router.replace(url);
    } else {
        router.push({name: RouteName.USER_HOME});
    }
};

const onLoginClick = async () => {
    const result = loginSchema.value.safeParse(loginForm);
    if (!result.success) {
        const firstError = result.error.issues[0]?.message || t("request.error.title");
        toast.add(newErrorToastFromError(new Error(firstError), t("request.error.title")));
        return;
    }
    try {
        const response = await loginRegisterService(axios).loginByPassword({
            identity: loginForm.identity!,
            token: loginForm.token!,
            rememberMe: loginForm.rememberMe!,
        });
        const body = response.data;
        const data = body.data!;
        userStore.loginUser(data.user, {
            accessToken: data.accessToken,
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
    <UForm ref="form" :schema="loginSchema" :state="loginForm" class="space-y-4 w-full" @submit="onLoginClick">
        <UFormField :label="t('views.userfaced.user.login.identity')" name="identity" required>
            <UInput v-model="loginForm.identity" :placeholder="t('views.userfaced.user.login.identityPlaceholder')"
                    autocomplete="username"
                    class="w-full" name="identity" type="text"/>
        </UFormField>
        <UFormField :label="t('views.common.user.password')" name="token" required>
            <UInput v-model="loginForm.token" :placeholder="t('views.common.user.passwordPlaceholder')"
                    :type="showPassword ? 'text' : 'password'"
                    :ui="{ trailing: 'pe-1' }"
                    autocomplete="current-password" class="w-full" name="token">
                <template #trailing>
                    <UButton
                            :aria-label="showPassword ? t('views.userfaced.user.login.hidePassword') : t('views.userfaced.user.login.showPassword')"
                            :aria-pressed="showPassword"
                            :icon="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                            aria-controls="password"
                            color="neutral"
                            size="sm"
                            variant="link"
                            @click="showPassword = !showPassword"
                    />
                </template>
            </UInput>
        </UFormField>
        <UCheckbox v-model="loginForm.rememberMe" :label="t('views.userfaced.user.login.rememberMe')"
                   name="rememberMe"/>
        <div class="flex flex-col sm:flex-row gap-2">
            <UButton class="flex-1" color="primary" type="submit">{{
                    t('views.userfaced.user.login.loginButton')
                }}
            </UButton>
            <UButton class="flex-1" variant="outline" @click="onLoginReset">{{ t('common.reset') }}</UButton>
        </div>
    </UForm>
    <UButton class="self-start p-0 h-auto text-sm" variant="link" @click="() => {}">
        {{ t('views.userfaced.user.login.forgotPassword') }}
    </UButton>
</template>
