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
        <AdminBreadcrumb :location="name" :menu="menuReview"/>
        <div class="flex items-baseline mt-5">
            <n-h1>{{ title }}</n-h1>
        </div>
        <n-data-table
                :bordered="false"
                :columns="columns"
                :data="data"
                :pagination="false"
                class="mt-5"
        />
        <div class="flex items-start justify-start mt-5">
            <div>
                <n-pagination v-model:page="page" :page-count="1"/>
            </div>
        </div>
    </div>
</template>

<script setup>
import {menuReview} from "@/views/menu";
import {useRouter} from "vue-router";
import AdminBreadcrumb from "@/components/admin/AdminBreadcrumb.vue";
import {getCurrentInstance, h, ref} from "vue";
import {NButton, NDataTable, NH1, NPagination} from "naive-ui";
import api from "@/request/api";
import {formatTimestamp} from "@/util/format";
import {createConfig} from "@/request/axios_config";
import {adminReviews} from "@/router";

const {proxy} = getCurrentInstance()

const page = ref(1)
const router = useRouter()

const name = router.currentRoute.value.name
const isAdmin = () => name === adminReviews


const getTitle = (name) => {
    if (isAdmin()) {
        return "管理审核列表"
    }
    return "审核列表"
}

const title = getTitle(name)

const columns = [
    {
        title: "任务ID",
        key: "id"
    },
    {
        title: "审核状态",
        key: "status"
    },
    {
        title: "分配人员",
        key: "reviewer"
    },
    {
        title: "最终操作人",
        key: "operator"
    },
    {
        title: "分配时间",
        key: "assignedTime",
        ellipsis: {
            tooltip: true
        }
    },
    {
        title: "类型",
        key: "type"
    },
    {
        title: "内容ID",
        key: "contentId"
    },
    {
        title: "标记",
        key: "reviewMark"
    },
    {
        title: "审核时间",
        key: "reviewTime",
        ellipsis: {
            tooltip: true
        }
    },
    {
        title: "说明",
        key: "result",
        ellipsis: true
    },
    {
        title: "操作",
        key: "actions",
        render(row) {
            return h(NButton,
                    {
                        size: 'small',
                        onClick: () => {
                        }
                    },
                    {default: () => "查看/编辑"}
            )
        }
    }
]

const data = ref([]);

const markTransform = (reviewMark) => {
    switch (reviewMark) {
        case "NORMAL":
            return "常规"
        case "REPORT":
            return "举报"
        default:
            return reviewMark
    }
}

const statusTransform = (reviewStatus) => {
    switch (reviewStatus) {
        case "NOT_REVIEWED":
            return "未审核"
        case "REVIEWED":
            return "已通过"
        case "REJECTED":
            return "已拒绝"
        default:
            return reviewStatus
    }
}

const contentTypeTransform = (contentType) => {
    switch (contentType) {
        case "ARTICLE":
            return "文章"
        case "COMMENT":
            return "评论"
        case "POST":
            return "动态"
        default:
            return contentType
    }
}

const requestForData = (page, isFullReviews) => {
    const config = createConfig()
    config.params = {
        page: page
    }
    const url = isFullReviews ? api.allReviews : api.currentReviews
    proxy.$axios.get(url, config).then((res) => {
        const recvData = res.data
        recvData.forEach((item) => {
            if (item.assignedTime)
                item.assignedTime = formatTimestamp(item.assignedTime)
            else item.assignedTime = "未分配"
            if (item.reviewTime)
                item.reviewTime = formatTimestamp(item.reviewTime)
            else item.reviewTime = "未审核"

            item.reviewMark = markTransform(item.reviewMark)
            item.status = statusTransform(item.status)
            item.type = contentTypeTransform(item.type)
        })
        data.value = recvData
    }).catch((err) => {
        console.log(err)
    })
}

requestForData(page.value, isAdmin())

</script>
