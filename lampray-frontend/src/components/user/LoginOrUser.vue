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
import {useRouter} from "vue-router";
import {RouteName} from "@/router/routeName.ts";
import {ref, watch} from "vue";
import {useUserStore} from "@/stores/user.ts";
import {userService} from "@/services/user/user.service.ts";
import type {DropdownMenuItem} from "@nuxt/ui";
import {useAxios} from "@/composables/useAxios.ts";
import type {AxiosResponse} from "axios";

const router = useRouter();
const userStore = useUserStore();
const axios = useAxios();

const handleLoginClick = () => {
    router.push({name: RouteName.LOGIN});
}

const fetchCurrentUser = async () => {
    if (!userStore.isLogin) {
        return;
    }
    try {
        const response = await userService(axios).getCurrentUser()
        const body = response.data;
        const user = body.data!;
        userStore.setUserData({...user, setup: true});
    } catch (error: AxiosResponse | any) {
    }
};

watch(() => userStore.isLogin, (newVal) => {
    if (newVal && !userStore.userData?.setup) {
        fetchCurrentUser();
    }
}, {immediate: true});


const userMenuItems = ref<DropdownMenuItem[]>([
    [
        {
            label: '个人主页',
            icon: 'i-lucide-user',
            to: {
                name: RouteName.USER_SPACE,
                params: {
                    id: userStore.user?.id
                }
            },
        },
        {
            label: '设置',
            icon: 'i-lucide-settings',
        },
    ],
    [
        {
            label: '退出登录',
            icon: 'i-lucide-log-out',
            onSelect: (e: Event) => {
                userStore.logout();
                router.push({name: RouteName.USER_HOME});
            }
        }
    ]
])

const adminMenuItems = ref<DropdownMenuItem[]>([
    [{
        label: '管理后台',
        icon: 'i-lucide-shield-check',
        onSelect: (e: Event) => {
            router.push({name: RouteName.ADMIN_HOME});
        }
    }],
    [
        {
            label: '个人主页',
            icon: 'i-lucide-user',
            to: {
                name: RouteName.USER_SPACE,
                params: {
                    id: userStore.user?.id
                }
            },
        },
        {
            label: '设置',
            icon: 'i-lucide-settings',
        },
    ],

    [
        {
            label: '退出登录',
            icon: 'i-lucide-log-out',
            onSelect: (e: Event) => {
                userStore.logout();
                router.push({name: RouteName.USER_HOME});
            }
        }
    ]
])


</script>
<template>
    <div>
        <UButton @click="handleLoginClick" v-if="!userStore.isLogin" size="xl"
                 color="primary"
                 variant="soft">
            登录
        </UButton>
        <!--TODO: hover to open-->
        <UDropdownMenu v-else class="cursor-pointer"
                       :items="userStore.hasAdminRole ? adminMenuItems : userMenuItems"
                       size="md">
            <UButton
                    :avatar="{
                        src: userStore.userData?.avatar,
                        alt: userStore.userData?.nickname,
                    }"
                    color="neutral"
                    variant="ghost"
                    size="xl"
            />
        </UDropdownMenu>
    </div>
</template>