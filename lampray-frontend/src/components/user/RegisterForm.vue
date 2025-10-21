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
import {reactive} from "vue";
import * as z from "zod";
import {loginRegisterService} from "@/services/user/user.service.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {useRouter} from "vue-router";
import {RouteName} from "@/router/routeName.ts";
import {newErrorToast, newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useI18n} from "vue-i18n";

const axios = useAxios();
const toast = useToast();
const router = useRouter();
const {t} = useI18n();

const registerSchema = z.object({
    username: z.string().min(1, "Username required").refine(val => val !== null && val.trim() !== "", {
        message: "Username required"
    }),
    password: z.string().min(1, "Password required").refine(val => val !== null && val.trim() !== "", {
        message: "Password required"
    }),
    email: z.email("Valid email required").refine(val => val !== null && val.trim() !== "", {
        message: "Email required"
    }),
    confirmPassword: z.string().refine(val => val !== null && val.trim() !== "", {
        message: "Please confirm password"
    }).min(1, "Please confirm password"),
    agree: z.boolean().refine(val => val === true, {
        message: "You must agree to the terms"
    }).default(false)
});
type RegisterSchema = z.output<typeof registerSchema>;
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
    const result = registerSchema.safeParse(registerForm);
    if (!result.success) {
        const firstError = result.error.message;
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
    <UForm :schema="registerSchema" :state="registerForm" class="space-y-4 w-full" @submit="onRegisterClick">
        <UFormField label="用户名" name="username" required>
            <UInput v-model="registerForm.username" placeholder="输入用户名" type="text" autocomplete="username"
                    name="username" class="w-full"/>
        </UFormField>
        <UFormField label="电子邮件" name="email" required>
            <UInput v-model="registerForm.email" placeholder="输入电子邮件" type="email" autocomplete="email"
                    name="email" class="w-full"/>
        </UFormField>
        <UFormField label="密码" name="password" required>
            <UInput v-model="registerForm.password" placeholder="输入密码" type="password"
                    autocomplete="new-password" name="password" class="w-full"/>
        </UFormField>
        <UFormField label="确认密码" name="confirmPassword" required>
            <UInput v-model="registerForm.confirmPassword" placeholder="确认密码" type="password"
                    autocomplete="new-password" name="confirmPassword" class="w-full"/>
        </UFormField>
        <UCheckbox v-model="registerForm.agree" label="我已阅读并同意相关条款" name="agree" required/>
        <div class="flex flex-col sm:flex-row gap-2">
            <UButton type="submit" class="flex-1" color="primary">注册</UButton>
            <UButton variant="outline" class="flex-1" @click="onRegisterReset">重置</UButton>
        </div>
    </UForm>
</template>

