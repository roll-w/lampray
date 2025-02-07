<!--
  - Copyright (C) 2023 RollW
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

<template>
    <div class="p-5">
        <AdminBreadcrumb :location="adminArticles" :menu="menuArticle"/>
        <div class="flex items-baseline mt-5">
            <n-h1>文章列表</n-h1>
        </div>
        <n-data-table
                :bordered="false"
                :columns="columns"
                :data="data"
                :pagination="false"
                class="mt-5"
        />
        <FastPagination :page="page" :route-name="adminArticles"/>
    </div>
</template>

<script setup>
import AdminBreadcrumb from "@/components/admin/AdminBreadcrumb.vue";
import {adminArticles} from "@/router";
import {useRouter} from "vue-router";
import {getCurrentInstance, h, ref} from "vue";
import {menuArticle} from "@/views/menu";
import api from "@/request/api";
import {createConfig} from "@/request/axios_config";
import {NButton, NDataTable, NH1, useDialog, useMessage, useNotification} from "naive-ui";
import {formatTimestamp} from "@/util/format";
import {popAdminErrorTemplate} from "@/views/utils/error";
import FastPagination from "@/components/FastPagination.vue";
import {usePage} from "@/views/utils/pages.js";

const router = useRouter()
const {proxy} = getCurrentInstance()
const notification = useNotification()
const message = useMessage()
const dialog = useDialog()

const page = usePage()

const columns = [
    {
        title: "ID",
        key: "id"
    },
    {
        title: "内容状态",
        key: "contentStatus"
    },
    {
        title: "从属用户",
        key: "authorId"
    },
    {
        title: "标题",
        key: "title",
        ellipsis: {
            tooltip: true
        }
    },
    {
        title: "访问权限",
        key: "accessAuthType"
    },
    {
        title: "内容",
        key: "content",
        ellipsis: true
    },
    {
        title: "发布时间",
        key: "createTime",
        ellipsis: {
            tooltip: true
        }
    },
    {
        title: "最后更新",
        key: "updateTime",
        ellipsis: {
            tooltip: true
        }
    },
    {
        title: "操作",
        key: "actions",
        render(row) {
            return h(NButton,
                    {
                        size: 'small',
                        onClick: () => {
                            // router.push({
                            //   name: adminArticles,
                            //
                            // })
                        }
                    },
                    {default: () => "查看/编辑"}
            )
        }
    }
]

const data = ref([])

const accessTypeTransform = (accessType) => {
    switch (accessType) {
        case "PUBLIC":
            return "公开"
        case "PASSWORD":
            return "密码可见"
        case "PRIVATE":
            return "私密"
        case "USER":
            return "部分用户可见"

        default:
            return accessType
    }
}

const contentStatusTransform = (contentStatus) => {
    switch (contentStatus) {
        case "REVIEWING":
            return "审核中"
        case "REVIEW_REJECTED":
            return "审核未通过"
        case "PUBLISHED":
            return "已发布"
        case "DELETED":
            return "已删除"
        case "FORBIDDEN":
            return "已屏蔽"
        case "HIDE":
            return "已隐藏"
    }
    return contentStatus
}

const requestArticles = (page) => {
    const config = createConfig()
    config.params = {
        page: page.page
    }
    proxy.$axios.get(api.articles(true), config)
            .then(res => {
                res.data.forEach((item) => {
                    if (item.createTime)
                        item.createTime = formatTimestamp(item.createTime)
                    if (item.updateTime)
                        item.updateTime = formatTimestamp(item.updateTime)
                    item.accessAuthType = accessTypeTransform(item.accessAuthType)
                    item.contentStatus = contentStatusTransform(item.contentStatus)
                })
                data.value = res.data
                pageCount.value = res.total
            })
            .catch(err => {
                popAdminErrorTemplate(notification, err)
            })
}

requestArticles(page.value)

</script>

<style scoped>

</style>