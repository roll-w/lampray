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
import {reactive, ref, useTemplateRef, watch} from "vue";
import * as z from "zod";
import {loginRegisterService} from "@/services/user/user.service.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {useRouter} from "vue-router";
import {RouteName} from "@/router/routeName.ts";
import {newErrorToast, newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useI18n} from "vue-i18n";
import {PASSWORD_REGEX, USERNAME_REGEX} from "@/components/user/constants.ts";

const axios = useAxios();
const toast = useToast();
const router = useRouter();
const {t, locale} = useI18n();

const createRegisterSchema = () => z.object({
    username: z.string()
        .min(3, t("views.userfaced.user.register.invalidUsernameDetail"))
        .max(20, t("views.userfaced.user.register.invalidUsernameDetail"))
        .regex(USERNAME_REGEX, {message: t("views.userfaced.user.register.invalidUsernameDetail")}),
    password: z.string()
        .min(4, t("views.userfaced.user.register.invalidPasswordDetail"))
        .max(20, t("views.userfaced.user.register.invalidPasswordDetail"))
        .regex(PASSWORD_REGEX, {message: t("views.userfaced.user.register.invalidPasswordDetail")}),
    email: z.email(t("views.userfaced.user.register.invalidEmail")).refine(val => val !== null && val.trim() !== "", {
        message: t("views.userfaced.user.register.invalidEmail")
    }),
    confirmPassword: z.string().min(1, t("views.common.user.confirmPasswordPlaceholder")).refine(val => val !== null && val.trim() !== "", {
        message: t("views.common.user.confirmPasswordPlaceholder")
    }),
    agree: z.boolean().refine(val => val === true, {
        message: t("views.userfaced.user.register.agree")
    }).default(false)
}).refine(data => data.password === data.confirmPassword, {
    message: t("views.userfaced.user.register.passwordMismatch"),
    path: ["confirmPassword"]
});

const registerSchema = ref(createRegisterSchema());
const form = useTemplateRef("form")

watch(locale, async () => {
    registerSchema.value = createRegisterSchema();
    // TODO: fix form error message update
    form.value?.clear()
}, {immediate: true});

type RegisterSchema = z.output<ReturnType<typeof createRegisterSchema>>;
const registerForm = reactive<Partial<RegisterSchema>>({
    username: "",
    password: "",
    email: "",
    confirmPassword: "",
    agree: false
});

const jumpToSuccess = () => {
    router.push({name: RouteName.REGISTER_TIPS});
};

const onRegisterClick = async () => {
    const result = registerSchema.value.safeParse(registerForm);
    if (!result.success) {
        const firstError = result.error.issues[0]?.message || t("request.error.title");
        toast.add(newErrorToastFromError(new Error(firstError), t("request.error.title")));
        return;
    }
    if (registerForm.password !== registerForm.confirmPassword) {
        toast.add(newErrorToast(t("request.error.title"),
                t("views.userfaced.user.register.passwordMismatch")));
        return;
    }
    try {
        await loginRegisterService(axios).registerUser({
            username: registerForm.username!,
            password: registerForm.password!,
            email: registerForm.email!
        });
        toast.add(newSuccessToast(t("views.userfaced.user.register.success")));
        onRegisterReset();
        jumpToSuccess();
    } catch (error: any) {
        toast.add(newErrorToastFromError(error, t("request.error.title")));
    }
};

const onRegisterReset = () => {
    registerForm.username = "";
    registerForm.password = "";
    registerForm.email = "";
    registerForm.confirmPassword = "";
    registerForm.agree = false;
};
</script>
<template>
    <UForm :schema="registerSchema" ref="form" :state="registerForm" class="space-y-4 w-full" @submit="onRegisterClick">
        <UFormField :label="t('views.common.user.username')" name="username" required>
            <UInput v-model="registerForm.username"
                    :placeholder="t('views.common.user.usernamePlaceholder')" type="text"
                    autocomplete="username"
                    name="username" class="w-full"/>
        </UFormField>
        <UFormField :label="t('views.common.user.email')" name="email" required>
            <UInput v-model="registerForm.email" :placeholder="t('views.common.user.emailPlaceholder')"
                    type="email" autocomplete="email"
                    name="email" class="w-full"/>
        </UFormField>
        <UFormField :label="t('views.common.user.password')" name="password" required>
            <UInput v-model="registerForm.password"
                    :placeholder="t('views.common.user.passwordPlaceholder')" type="password"
                    autocomplete="new-password" name="password" class="w-full"/>
        </UFormField>
        <UFormField :label="t('views.common.user.confirmPassword')" name="confirmPassword" required>
            <UInput v-model="registerForm.confirmPassword"
                    :placeholder="t('views.common.user.confirmPasswordPlaceholder')" type="password"
                    autocomplete="new-password" name="confirmPassword" class="w-full"/>
        </UFormField>
        <UCheckbox v-model="registerForm.agree" :label="t('views.userfaced.user.register.agree')" name="agree"
                   required/>
        <div class="flex flex-col sm:flex-row gap-2">
            <UButton type="submit" class="flex-1" color="primary">{{ t('common.submit') }}</UButton>
            <UButton variant="outline" class="flex-1" @click="onRegisterReset">{{ t('common.reset') }}</UButton>
        </div>
    </UForm>
</template>
