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

import {createRouter, createWebHistory} from "vue-router"
import {RouteName} from "@/router/routeName.ts";
import {useUserStore} from "@/stores/user.ts";
import {UserRole} from "@/services/user/user.type.ts";
import {i18n} from "@/i18n/i18n.ts";

const router = createRouter({
    // history: createWebHistory(import.meta.env.BASE_URL),
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
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
                    meta: {
                        title: "首页"
                    }
                },
                {
                    path: "/user/login",
                    name: RouteName.LOGIN,
                    component: () => import("@/views/userfaced/user/LoginRegisterPage.vue"),
                    meta: {
                        title: "登录"
                    }
                },
                {
                    path: "/user/register",
                    name: RouteName.REGISTER,
                    component: () => import("@/views/userfaced/user/LoginRegisterPage.vue"),
                    meta: {
                        title: "注册"
                    }
                },
                 {
                    path: "/user/register/complete",
                    name: RouteName.REGISTER_TIPS,
                    component: () => import("@/views/userfaced/user/RegisterTipsPage.vue"),
                    meta: {
                        title: "注册完成"
                    }
                },
                {
                    path: "/user/register/activate/:code",
                    name: RouteName.REGISTER_ACTIVATE,
                    component: () => import("@/views/userfaced/user/RegisterActivatePage.vue"),
                    meta: {
                        title: "注册激活"
                    }
                },
                {
                    path: "/users/:id",
                    name: RouteName.USER_SPACE,
                    component: () => import("@/views/userfaced/user/UserSpacePage.vue"),
                    meta: {
                        title: "用户空间"
                    }
                },
                {
                    path: "/user/settings",
                    name: RouteName.USER_SETTINGS,
                    component: () => import("@/views/userfaced/user/UserSettingsPage.vue"),
                    meta: {
                        title: "用户设置",
                        requireLogin: true
                    }
                },
                {
                    path: "/notfound",
                    name: RouteName.NOT_FOUND,
                    component: () => import("@/views/NotFound.vue"),
                    meta: {
                        title: "Not Found"
                    }
                },
                {
                    path: "/:path(.*)*",
                    redirect: "/notfound"
                },
                {
                    path: "/blocked",
                    name: RouteName.BLOCKED,
                    component: () => import("@/views/Blocked.vue"),
                    meta: {
                        title: "Blocked"
                    }
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
                    meta: {
                        title: "管理后台"
                    }
                },
                {
                    path: "/admin/users",
                    name: RouteName.ADMIN_USER_LIST,
                    component: () => import("@/views/adminfaced/user/UserListManagementPage.vue"),
                    meta: {
                        title: "用户管理"
                    }
                },
                {
                    path: "/admin/users/:id",
                    name: RouteName.ADMIN_USER_DETAIL,
                    component: () => import("@/views/adminfaced/user/UserDetailManagementPage.vue"),
                    meta: {
                        title: "用户详情"
                    }
                },
                {
                    path: "/admin/settings",
                    name: RouteName.ADMIN_SYSTEM_SETTINGS,
                    component: () => import("@/views/adminfaced/system/SystemSettingListPage.vue"),
                    meta: {
                        title: "系统设置"
                    }
                },

            ],
            meta: {
                requireAdmin: true,
                requireLogin: true
            },
        },

    ],
})

export const getTitleSuffix = () => {
    return " | 灯辉 - Lampray"
}

router.afterEach((to, from) => {
    document.title = i18n.global.t(`route.${to.name as string}`) + getTitleSuffix()
})


router.beforeEach((to, from, next) => {
    const userStore = useUserStore()

    // TODO: improve performance
    userStore.load()
    if (to.meta.requireLogin && !userStore.isLogin) {
        return next({
            name: RouteName.LOGIN,
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

    const role = userStore.user!.role
    if (!userStore.isLogin || !role || role === UserRole.USER) {
        return next({
            name: RouteName.LOGIN,
        })
    }
    return next()
})


export default router
