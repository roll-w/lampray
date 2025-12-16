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
import type {NavigationMenuItem} from "@nuxt/ui";
import {RouteName} from "@/router/routeName.ts";
import {RouterView} from "vue-router";
import {useI18n} from "vue-i18n";
import {computed} from "vue";

const {t} = useI18n()

const items = computed<NavigationMenuItem[]>(() => [
    {
        label: t("adminMenu.home"),
        icon: "i-lucide-home",
        to: {name: RouteName.ADMIN_HOME}
    },
    {
        label: t("adminMenu.backToBlog"),
        icon: "i-lucide-arrow-left-right",
        to: {name: RouteName.USER_HOME}
    },
    {
        label: t("adminMenu.userManagement"),
        icon: "i-lucide-users",
        children: [
            {
                label: t("adminMenu.userList"),
                to: {name: RouteName.ADMIN_USER_LIST}
            },
            {
                label: t("adminMenu.staffManagement"),
            }
        ]
    },
    {
        label: t("adminMenu.articleManagement"),
        icon: "i-lucide-file-text",
        children: [
            {
                label: t("adminMenu.articleList"),
            },
            {
                label: t("adminMenu.articleCategory"),
            },
            {
                label: t("adminMenu.articleTag"),
            }
        ]
    },
    {
        label: t("adminMenu.commentManagement"),
        icon: "i-lucide-message-square",
        children: [
            {
                label: t("adminMenu.commentList"),
            }
        ]
    },
    {
        label: t("adminMenu.reviewManagement"),
        icon: "i-lucide-check-circle",
        children: [
            {
                label: t("adminMenu.adminReviewList"),
            },
            {
                label: t("adminMenu.reviewList"),
            },
            {
                label: t("adminMenu.reviewTask"),
            }
        ]
    },
    {
        label: t("adminMenu.systemManagement"),
        icon: "i-lucide-settings",
        children: [
            {
                label: t("adminMenu.systemSettings"),
                to: {name: RouteName.ADMIN_SYSTEM_SETTINGS}
            },
            {
                label: t("adminMenu.firewallManagement"),
                to: {name: RouteName.ADMIN_FIREWALL}
            },
            {
                label: t("adminMenu.systemLog"),
            },
            {
                label: t("adminMenu.messageResource"),
            },
            {
                label: t("adminMenu.systemMonitor"),
            }
        ]
    }
])
</script>

<template>
    <UDashboardGroup class="h-[calc(100vh-var(--ui-header-height))] top-(--ui-header-height)">
        <UDashboardSidebar class="min-h-[calc(100vh-var(--ui-header-height))]" :default-size="13" collapsible resizable>
            <template #default="{ collapsed }">
                <UNavigationMenu
                        :collapsed="collapsed"
                        :items="items"
                        orientation="vertical"
                />
            </template>
        </UDashboardSidebar>
        <RouterView/>
    </UDashboardGroup>
</template>

<style scoped>

</style>