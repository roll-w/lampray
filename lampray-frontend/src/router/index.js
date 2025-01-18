/*
 * Copyright (C) 2023 RollW
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

import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from "@/stores/user";

export const index = "index"
export const login = "login-page"
export const register = "register-page"
export const registerActivate = "register-activate"
export const registerTip = "register-tip"

export const searchPage = "search-page"

export const articleEditorPage = "article-page"
export const articleEditorPageUpdate = "article-page-update"
export const articleView = "article-details"
export const articleFocusView = "article-details-focus"
export const articleList = "article-list"

export const userPage = "user-page"
export const userSettingPage = "user-setting-page"
export const userSearchPage = "user-search-page"

export const userSharePage = "user-share-page"
export const userFavoritePage = "user-favorite-page"
export const userFavoritePageWithId = "user-favorite-page-with-id"
export const userStatsPage = "user-stats-page"


export const userChat = "user-chat"

// admins
export const admin = "admin-index"
export const adminUsers = "admin-user-list"
export const adminUserDetails = "admin-user-list-details"
export const adminStaffs = "admin-staff-list"
export const adminReviews = "admin-review-list"
export const adminArticles = "admin-article-list"
export const reviewsQueue = "admin-review-queue"
export const reviewerReviews = "admin-reviewer-review-list"
export const systemLog = "admin-system-log"
export const systemMessageResource = "admin-system-message-resource"
export const systemSettings = "admin-system-settings"
export const page404 = "404"

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/layout',
            name: 'user-layout',
            redirect: '/',
            component: () => import("@/views/UserLayout.vue"),
            children: [
                {
                    path: '/',
                    name: index,
                    component: () => import('@/views/UserIndex.vue'),
                    meta: {
                        title: "首页"
                    }
                },
                {
                    path: '/user/login',
                    name: login,
                    component: () => import('@/views/LoginView.vue'),
                    meta: {
                        title: "登录"
                    }
                },
                {
                    path: '/user/register',
                    name: register,
                    component: () => import('@/views/LoginView.vue'),
                    meta: {
                        title: "注册"
                    }
                },
                {
                    path: '/user/register/activate/:token',
                    name: registerActivate,
                    component: () => import('@/views/user/RegisterActivation.vue'),
                    meta: {
                        title: "激活账户"
                    }
                },
                {
                    path: '/user/register/activate',
                    name: registerTip,
                    component: () => import('@/views/user/RegisterTips.vue'),
                    meta: {
                        title: "注册"
                    }
                },
                {
                    path: '/user/:userId/space',
                    name: userPage,
                    component: () => import('@/views/user/UserPersonalPage.vue'),
                    meta: {
                        title: "用户主页"
                    }
                },
                {
                    path: '/user/manage/articles',
                    name: articleList,
                    component: () => import('@/views/article/PersonalArticleManageView.vue'),
                    meta: {
                        title: "文章管理",
                        requireLogin: true,
                    }
                },

                {
                    path: '/user/:user/article/:id',
                    name: articleView,
                    component: () => import('@/views/article/ArticleViewer.vue'),
                    meta: {
                        title: "文章查看"
                    }
                },
                {
                    path: '/user/:user/article/:id/focus',
                    name: articleFocusView,
                    component: () => import('@/views/article/ReadingView.vue'),
                    meta: {
                        title: "文章查看"
                    }
                },

                {
                    path: '/article/editor',
                    name: articleEditorPage,
                    component: () => import('@/views/article/ArticleEditor.vue'),
                    meta: {
                        title: "文章编辑",
                        requireLogin: true,
                    }
                },
                {
                    path: '/user/chat/:id',
                    name: userChat,
                    component: () => import('@/views/message/UserChat.vue'),
                    meta: {
                        title: "聊天",
                        requireLogin: true,
                    },
                },
                {
                    path: '/user/chat',
                    redirect: '/user/chat/s0',
                },
                {
                    path: '/article/editor/:id',
                    name: articleEditorPageUpdate,
                    component: () => import('@/views/article/ArticleEditor.vue'),
                    meta: {
                        title: "文章编辑",
                        requireLogin: true,
                    }
                },
                {
                    path: '/user/search',
                    name: userSearchPage,
                    component: () => import("@/views/user/UserSearchView.vue"),
                    meta: {
                        title: "用户搜索",
                        requireLogin: false
                    }
                },
                {
                    path: '/search',
                    name: searchPage,
                    component: () => import("@/views/common/SearchView.vue"),
                    meta: {
                        title: "搜索",
                        requireLogin: false
                    }
                },
                {
                    path: '/404',
                    name: page404,
                    component: () => import('@/views/NotFound.vue'),
                    meta: {
                        title: "404"
                    }
                },
                {
                    path: '/:path(.*)*',
                    redirect: '/404'
                },

            ]
        },
        {
            path: '/layout/admin',
            name: 'admin-layout',
            redirect: '/admin',
            component: () => import('@/views/AdminLayout.vue'),
            children: [
                {
                    path: '/admin',
                    name: admin,
                    component: () => import('@/views/admin/AdminIndex.vue'),
                    meta: {
                        title: "系统管理"
                    }
                },
                {
                    path: '/admin/users',
                    name: adminUsers,
                    component: () => import('@/views/admin/user/UsersList.vue'),
                    meta: {
                        title: "用户列表"
                    }
                },
                {
                    path: '/admin/users/staffs',
                    name: adminStaffs,
                    component: () => import('@/views/admin/user/StaffList.vue'),
                    meta: {
                        title: "工作人员列表"
                    }
                },
                {
                    path: '/admin/users/:userId',
                    name: adminUserDetails,
                    component: () => import('@/views/admin/user/AdminUserDetails.vue'),
                    meta: {
                        title: "用户信息与编辑"
                    }
                },
                {
                    path: '/admin/articles',
                    name: adminArticles,
                    component: () => import('@/views/admin/article/AdminArticlesList.vue'),
                    meta: {
                        title: "文章列表"
                    }
                },

                {
                    path: '/admin/reviews/manage',
                    name: adminReviews,
                    component: () => import('@/views/admin/review/ReviewJobs.vue'),
                    meta: {
                        title: "管理审核记录"
                    }
                },
                {
                    path: '/admin/reviews/reviewer',
                    name: reviewerReviews,
                    component: () => import('@/views/admin/review/ReviewJobs.vue'),
                    meta: {
                        title: "审核记录"
                    }
                },
                {
                    path: '/admin/reviews/queue',
                    name: reviewsQueue,
                    component: () => import('@/views/admin/review/ReviewJobQueue.vue'),
                    meta: {
                        title: "审核队列"
                    }
                },
                {
                    path: '/admin/reviews/:id',
                    meta: {
                        title: "审核详情"
                    }
                },
                {
                    path: '/admin/system/logs',
                    name: systemLog,
                    component: () => import('@/views/admin/system/SystemLog.vue'),
                    meta: {
                        title: "系统日志"
                    }
                },
                {
                    path: '/admin/system/settings',
                    name: systemSettings,
                    component: () => import('@/views/admin/system/SystemSettings.vue'),
                    meta: {
                        title: "系统设置"
                    }
                },
                {
                    path: '/admin/system/resources/messages',
                    name: systemMessageResource,
                    component: () => import('@/views/admin/system/SystemMessageResources.vue'),
                    meta: {
                        title: "消息资源"
                    }
                },
            ]
        }

    ]
})

const defaultTitle = "Lampray";

export const getTitleSuffix = () => {
    return " | 灯辉 - Lampray"
}

router.afterEach((to, from) => {
    document.title = to.meta.title ? to.meta.title + getTitleSuffix() : defaultTitle
})


router.beforeEach((to, from, next) => {
    const userStore = useUserStore()

    if (to.meta.requireLogin && !userStore.isLogin) {
        return next({
            name: login,
        })
    }

    if (!to.name.startsWith("admin")) {
        return next()
    }
    const role = userStore.user.role
    if (!userStore.isLogin || !role || role.value === "USER") {
        return next({
            name: page404
        })
    }
    return next()
})

export default router
