/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {createRouter, createWebHistory, type RouteRecordRaw} from "vue-router"
import {RouteName} from "@/router/routeName.ts";
import {useUserStore} from "@/stores/user.ts";
import {UserRole} from "@/services/user/user.type.ts";
import {i18n} from "@/i18n/i18n.ts";

const routes: RouteRecordRaw[] = [
    {
        path: "/layout",
        name: "user-layout",
        redirect: "/",
        component: () => import("@/views/userfaced/UserLayout.vue"),
        children: [
            {
                path: "/",
                name: RouteName.USER_HOME,
                component: () => import("@/views/userfaced/user/UserHome.vue"),
            },
            {
                path: "/articles",
                name: RouteName.ARTICLE_LIST,
                component: () => import("@/views/userfaced/article/ArticleList.vue"),
            },
            {
                path: "/articles/:id",
                name: RouteName.ARTICLE_DETAIL,
                component: () => import("@/views/userfaced/article/ArticleDetail.vue"),
            },
            {
                path: "/article/editor",
                name: RouteName.ARTICLE_EDITOR,
                component: () => import("@/views/userfaced/article/ArticleEditor.vue"),
                meta: {
                    requireLogin: true
                }
            },
            {
                path: "/user/login",
                name: RouteName.LOGIN,
                component: () => import("@/views/userfaced/user/LoginRegisterPage.vue"),
            },
            {
                path: "/user/register",
                name: RouteName.REGISTER,
                component: () => import("@/views/userfaced/user/LoginRegisterPage.vue"),
            },
            {
                path: "/user/register/complete",
                name: RouteName.REGISTER_TIPS,
                component: () => import("@/views/userfaced/user/RegisterTipsPage.vue"),
            },
            {
                path: "/user/register/activate/:code",
                name: RouteName.REGISTER_ACTIVATE,
                component: () => import("@/views/userfaced/user/RegisterActivatePage.vue"),
            },
            {
                path: "/users/:id",
                name: RouteName.USER_SPACE,
                component: () => import("@/views/userfaced/user/UserSpacePage.vue"),
            },
            {
                path: "/user/settings",
                name: RouteName.USER_SETTINGS,
                component: () => import("@/views/userfaced/user/UserSettingsPage.vue"),
                meta: {
                    requireLogin: true
                }
            },
            {
                path: "/notfound",
                name: RouteName.NOT_FOUND,
                component: () => import("@/views/NotFound.vue"),
            },
            {
                path: "/:path(.*)*",
                redirect: "/notfound"
            },
            {
                path: "/blocked",
                name: RouteName.BLOCKED,
                component: () => import("@/views/Blocked.vue"),
            }
        ]
    },
    {
        path: "/admin-layout",
        name: "admin-layout",
        redirect: "/admin",
        component: () => import("@/views/adminfaced/AdminLayout.vue"),
        children: [
            {
                path: "/admin",
                name: RouteName.ADMIN_HOME,
                component: () => import("@/views/adminfaced/AdminHome.vue"),
            },
            {
                path: "/admin/users",
                name: RouteName.ADMIN_USER_LIST,
                component: () => import("@/views/adminfaced/user/UserListManagementPage.vue"),
            },
            {
                path: "/admin/users/:id",
                name: RouteName.ADMIN_USER_DETAIL,
                component: () => import("@/views/adminfaced/user/UserDetailManagementPage.vue"),
            },
            {
                path: "/admin/content/review/queue",
                name: RouteName.ADMIN_CONTENT_REVIEW_QUEUE,
                component: () => import("@/views/adminfaced/review/ReviewQueuePage.vue"),
            },
            {
                path: "/admin/settings",
                name: RouteName.ADMIN_SYSTEM_SETTINGS,
                component: () => import("@/views/adminfaced/system/SystemSettingListPage.vue"),
            },
            {
                path: "/admin/settings/:key",
                name: RouteName.ADMIN_SYSTEM_SETTING_DETAIL,
                component: () => import("@/views/adminfaced/system/SettingDetailPage.vue"),
            },
            {
                path: "/admin/firewall",
                name: RouteName.ADMIN_FIREWALL,
                component: () => import("@/views/adminfaced/system/FirewallManagementPage.vue"),
            },
            {
                path: "/admin/firewall/filtertable",
                name: RouteName.ADMIN_FILTER_TABLE,
                component: () => import("@/views/adminfaced/system/FilterTableManagementPage.vue"),
            },
        ],
        meta: {
            requireAdmin: true,
            requireLogin: true
        },
    },
]

if (import.meta.env.DEV) {
    const layout = routes.find(r => r.name === "user-layout")
    layout!.children!.push({
        path: "/editor",
        name: "editor",
        component: () => import("@/components/structuraltext/EditorPreview.vue"),
    });
}

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: routes
})

export const getTitleSuffix = () => {
    return " | 灯辉 - Lampray"
}

router.afterEach((to, from) => {
    document.title = i18n.global.t(`route.${to.name as string}`) + getTitleSuffix()
})


router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()
    if (to.meta.requireLogin && !userStore.isLogin) {
        return next({
            name: RouteName.LOGIN,
            query: { source: to.fullPath }
        })
    }
    if (userStore.isBlocked && to.name !== RouteName.BLOCKED) {
        return next({
            name: RouteName.BLOCKED,
        })
    }
    if (!to.meta.requireAdmin) {
        return next()
    }

    const role = userStore.user?.role
    if (!userStore.isLogin || !role || role === UserRole.USER) {
        return next({
            name: RouteName.LOGIN,
            query: { source: to.fullPath }
        })
    }
    return next()
})


export default router
