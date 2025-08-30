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

import {
    admin,
    adminArticles,
    adminReviews,
    adminStaffs,
    adminUsers,
    index,
    reviewerReviews,
    reviewsQueue,
    systemLog,
    systemMessageResource,
    systemSettings
} from "@/router";
import {h} from "vue";
import {RouterLink} from "vue-router";

export const menuUsers = "user-management"
export const menuArticle = "article-management"
export const menuComment = "comment-management"
export const menuReview = "review-management"
export const menuSystem = "system-management"

// admin menus

const menus = [
    {
        linked: true,
        name: "系统首页",
        key: admin
    },
    {
        linked: true,
        name: "返回博客",
        key: index
    },
    {
        name: "用户管理",
        key: menuUsers,
        children: [
            {
                linked: true,
                name: "用户列表",
                key: adminUsers
            },
            {
                linked: true,
                name: "工作人员管理",
                key: adminStaffs
            }
        ]
    },
    {
        name: "文章管理",
        key: menuArticle,
        children: [
            {
                name: "文章列表",
                linked: true,
                key: adminArticles
            },
            {
                name: "文章分类",
                key: "article-management-category"
            },
            {
                name: "文章标签",
                key: "article-management-tag"
            },
        ]
    },
    {
        name: "评论管理",
        key: menuComment,
        children: [
            {
                name: "评论列表",
                key: "comment-management-list"
            },
        ]
    },
    {
        name: "审核管理",
        key: menuReview,
        children: [
            {
                name: "管理审核列表",
                key: adminReviews,
                linked: true,
            },
            {
                name: "审核列表",
                key: reviewerReviews,
                linked: true,
            },

            {
                name: "审核任务",
                key: reviewsQueue,
                linked: true,
            }
        ]
    },
    {
        name: "系统管理",
        key: menuSystem,
        children: [
            {
                linked: true,
                name: "系统设置",
                key: systemSettings
            },
            {
                linked: true,
                name: "系统日志",
                key: systemLog
            },
            {
                name: "消息资源",
                key: systemMessageResource
            },
            {
                name: "系统监控",
                key: "system-management-monitor"
            },
        ]
    },
]

export const requestChildrenMenus = (name) => {
    return menus.find(menu => menu.key === name)
}

export const requestFullMenu = () => {
    return [...menus]
}

export const requestFullMenuByNames = (names) => {
    return menus.filter(menu => names.includes(menu.key))
}

export const convertsToMenuOptions = (menus) => {
    return menus.map(menu => {
        let children
        if (menu.children) {
            children = [...menu.children]
            children = convertsToMenuOptions(children)
        } else {
            children = undefined
        }
        if (!menu.linked) {
            return {
                key: menu.key,
                label: menu.name,
                children: children
            }
        }
        return {
            key: menu.key,
            label: () => h(
                RouterLink,
                {
                    to: {
                        name: menu.key,
                    }
                },
                {default: () => menu.name}
            ),
        }
    })
}
