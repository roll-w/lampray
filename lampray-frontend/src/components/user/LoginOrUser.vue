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
import {useI18n} from "vue-i18n";

const router = useRouter();
const userStore = useUserStore();
const axios = useAxios();
const {t, locale} = useI18n()

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

const menuItems = ref<DropdownMenuItem[]>([]);

const buildUserMenu = (): DropdownMenuItem[] => [
    [
        {
            label: t("navbar.profile"),
            icon: "i-lucide-user",
            to: {
                name: RouteName.USER_SPACE,
                params: {
                    id: userStore.user?.id
                }
            },
        },
        {
            label: t("navbar.settings"),
            icon: "i-lucide-settings",
        },
    ],
    [
        {
            label: t("navbar.logout"),
            icon: "i-lucide-log-out",
            onSelect: (e: Event) => {
                userStore.logout();
                router.push({name: RouteName.USER_HOME});
            }
        }
    ]
];

const buildAdminMenu = (): DropdownMenuItem[] => [
    [
        {
            label: t("navbar.admin"),
            icon: "i-lucide-shield-check",
            onSelect: (e: Event) => {
                router.push({name: RouteName.ADMIN_HOME});
            }
        }
    ],
    [
        {
            label: t("navbar.profile"),
            icon: "i-lucide-user",
            to: {
                name: RouteName.USER_SPACE,
                params: {
                    id: userStore.user?.id
                }
            },
        },
        {
            label: t("navbar.settings"),
            icon: "i-lucide-settings",
            to: {
                name: RouteName.USER_SETTINGS
            }
        },
    ],
    [
        {
            label: t("navbar.logout"),
            icon: "i-lucide-log-out",
            onSelect: (e: Event) => {
                userStore.logout();
                router.push({name: RouteName.USER_HOME});
            }
        }
    ]
];

const buildMenus = (): DropdownMenuItem[] => {
    if (!userStore.isLogin) {
        return [];
    }
    if (userStore.hasAdminRole) {
        return buildAdminMenu();
    }
    return buildUserMenu();
};

menuItems.value = buildMenus();

watch([locale, () => userStore.user], () => {
    menuItems.value = buildMenus();
});

</script>
<template>
    <div>
        <UButton @click="handleLoginClick" v-if="!userStore.isLogin" size="xl"
                 color="primary"
                 variant="soft">
            {{ t("views.common.user.login") }}
        </UButton>
        <!--TODO: hover to open-->
        <UDropdownMenu v-else class="cursor-pointer"
                       :items="menuItems"
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