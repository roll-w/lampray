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
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {RouteName} from "@/router/routeName.ts";


const shortcutMenus = [
    {
        title: "用户管理",
        to: {name: RouteName.ADMIN_USER_LIST},
        icon: 'i-lucide-users',
    },
    {
        title: "文章管理",
        icon: 'i-lucide-file-text',
    },
    {
        title: "评论管理",
        icon: 'i-lucide-message-square',
    },
    {
        title: "系统设置",
        to: {name: RouteName.ADMIN_SYSTEM_SETTINGS},
        icon: 'i-lucide-settings',
    },
]

</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    管理后台
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <div class="">

                <div>
                    <h2 class="text-2xl font-semibold p-4">快捷操作</h2>
                    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 p-4">
                        <UPageCard v-for="menu in shortcutMenus" :key="menu.title" :to="menu.to"
                                   class="cursor-pointer">
                            <div class="flex flex-col items-center justify-center space-y-2">
                                <span class="text-lg font-medium">{{ menu.title }}</span>
                            </div>
                        </UPageCard>
                    </div>
                </div>

            </div>
        </template>
    </DashboardPanel>

</template>

<style scoped>

</style>