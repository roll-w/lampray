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
import {useRoute, useRouter} from "vue-router";
import LoginForm from "@/components/user/LoginForm.vue";
import RegisterForm from "@/components/user/RegisterForm.vue";
import {RouteName} from "@/router/routeName.ts";
import {useI18n} from "vue-i18n";

const route = useRoute();
const router = useRouter();
const {t} = useI18n();

const isLogin = ref(route.name === RouteName.LOGIN);

watch(() => [route.name],
        ([name]) => {
            isLogin.value = name === RouteName.LOGIN;
        }
);

const handleSwitch = () => {
    if (isLogin.value) {
        router.push({name: RouteName.REGISTER});
    } else {
        router.push({name: RouteName.LOGIN});
    }
};
</script>
<template>
    <div class="relative min-h-[80vh] flex items-center justify-center overflow-hidden animate-gradient">
        <div class="absolute inset-0 z-0 pointer-events-none"></div>
        <Transition
                enter-active-class="transition-all duration-500 ease-in-out"
                leave-active-class="transition-all duration-500 ease-in-out"
                enter-from-class="opacity-0 translate-y-8"
                enter-to-class="opacity-100 translate-y-0"
                leave-from-class="opacity-100 translate-y-0"
                leave-to-class="opacity-0 translate-y-8"
                mode="out-in"
        >
            <div v-if="isLogin" key="login"
                 class="relative z-10 flex flex-col gap-6 w-full max-w-md p-8
                 rounded-xl border border-gray-200 dark:border-gray-700
                 bg-linear-to-br from-blue-50 via-default to-amber-50 dark:from-blue-950/50 dark:via-default dark:to-amber-950/50">
                <div class="flex justify-between items-center">
                    <h2 class="text-xl font-semibold text-primary">{{ t('views.userfaced.user.login.title') }}</h2>
                    <p class="text-sm text-gray-500 dark:text-gray-400">
                        <UButton variant="link" color="secondary" class="p-0 h-auto" @click="handleSwitch">
                            {{ t('views.userfaced.user.login.noAccount') }}
                        </UButton>
                    </p>
                </div>
                <LoginForm/>
            </div>
            <div v-else key="register"
                 class="relative z-10 flex flex-col gap-6 w-full max-w-md p-8
                 rounded-xl border border-gray-200 dark:border-gray-700
                 bg-linear-to-br from-blue-50 via-default to-amber-50 dark:from-blue-950/50 dark:via-default dark:to-amber-950/50">
                <div class="flex justify-between items-center">
                    <h2 class="text-xl font-semibold text-primary">{{ t('views.userfaced.user.register.title') }}</h2>
                    <p class="text-sm text-gray-500 dark:text-gray-400">
                        <UButton variant="link" color="secondary" class="p-0 h-auto" @click="handleSwitch">
                            {{ t('views.userfaced.user.register.hasAccount') }}
                        </UButton>
                    </p>
                </div>
                <RegisterForm/>
            </div>
        </Transition>
    </div>
</template>