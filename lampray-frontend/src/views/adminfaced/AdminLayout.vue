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
import type {NavigationMenuItem} from '@nuxt/ui';
import {RouteName} from "@/router/routeName.ts";


//const menus = [
// {
//     linked: true,
//             name: "系统首页",
//         key: admin
// },
// {
//     linked: true,
//             name: "返回博客",
//         key: index
// },
// {
//     name: "用户管理",
//             key: menuUsers,
//         children: [
//     {
//         linked: true,
//         name: "用户列表",
//         key: adminUsers
//     },
//     {
//         linked: true,
//         name: "工作人员管理",
//         key: adminStaffs
//     }
// ]
// },
// {
//     name: "文章管理",
//             key: menuArticle,
//         children: [
//     {
//         name: "文章列表",
//         linked: true,
//         key: adminArticles
//     },
//     {
//         name: "文章分类",
//         key: "article-management-category"
//     },
//     {
//         name: "文章标签",
//         key: "article-management-tag"
//     },
// ]
// },
// {
//     name: "评论管理",
//             key: menuComment,
//         children: [
//     {
//         name: "评论列表",
//         key: "comment-management-list"
//     },
// ]
// },
// {
//     name: "审核管理",
//             key: menuReview,
//         children: [
//     {
//         name: "管理审核列表",
//         key: adminReviews,
//         linked: true,
//     },
//     {
//         name: "审核列表",
//         key: reviewerReviews,
//         linked: true,
//     },
//
//     {
//         name: "审核任务",
//         key: reviewsQueue,
//         linked: true,
//     }
// ]
// },
// {
//     name: "系统管理",
//             key: menuSystem,
//         children: [
//     {
//         linked: true,
//         name: "系统设置",
//         key: systemSettings
//     },
//     {
//         linked: true,
//         name: "系统日志",
//         key: systemLog
//     },
//     {
//         name: "消息资源",
//         key: systemMessageResource
//     },
//     {
//         name: "系统监控",
//         key: "system-management-monitor"
//     },
// ]
// },
// ]

const items: NavigationMenuItem[] = [
    {
        label: '系统首页',
        icon: 'i-lucide-home',
        to: {name: RouteName.ADMIN_HOME}
    },
    {
        label: '返回博客',
        icon: 'i-lucide-arrow-left-right',
        to: {name: RouteName.USER_HOME}
    },
    {
        label: '用户管理',
        icon: 'i-lucide-users',
        children: [
            {
                label: '用户列表',
                to: {name: RouteName.ADMIN_USER_LIST}
            },
            {
                label: '工作人员管理',
            }
        ]
    },
    {
        label: '文章管理',
        icon: 'i-lucide-file-text',
        children: [
            {
                label: '文章列表',
            },
            {
                label: '文章分类',
            },
            {
                label: '文章标签',
            }
        ]
    },
    {
        label: '评论管理',
        icon: 'i-lucide-message-square',
        children: [
            {
                label: '评论列表',
            }
        ]
    },
    {
        label: '审核管理',
        icon: 'i-lucide-check-circle',
        children: [
            {
                label: '管理审核列表',
            },
            {
                label: '审核列表',
            },
            {
                label: '审核任务',
            }
        ]
    },
    {
        label: '系统管理',
        icon: 'i-lucide-settings',
        children: [
            {
                label: '系统设置',
                to: {name: RouteName.ADMIN_SYSTEM_SETTINGS}
            },
            {
                label: '系统日志',
            },
            {
                label: '消息资源',
            },
            {
                label: '系统监控',
            }
        ]
    }
]
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