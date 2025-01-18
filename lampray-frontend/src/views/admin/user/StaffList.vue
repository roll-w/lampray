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
        <AdminBreadcrumb :location="adminStaffs" :menu="menuUsers"/>
        <div class="flex items-baseline mt-5">
            <n-h1>工作人员管理</n-h1>
            <div class="flex flex-grow justify-end">
                <n-button>创建工作人员</n-button>
            </div>
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
import {getCurrentInstance, h, ref} from "vue";
import {NButton, NButtonGroup, NDataTable, NPagination, NTag, useNotification} from "naive-ui";
import api from "@/request/api";
import {useUserStore} from "@/stores/user";
import AdminBreadcrumb from "@/components/admin/AdminBreadcrumb.vue";
import {formatTimestamp} from "@/util/format";
import {adminStaffs, adminUserDetails} from "@/router";
import {menuUsers} from "@/views/menu";
import {createConfig} from "@/request/axios_config";
import {popAdminErrorTemplate} from "@/views/utils/error";
import {useRouter} from "vue-router";

const notification = useNotification()
const userStore = useUserStore()
const {proxy} = getCurrentInstance()
const router = useRouter()

const page = ref(1)

const columns = [
    {
        title: "用户ID",
        key: "userId"
    },
    {
        title: "用户名",
        key: "username"
    },
    {
        title: "工号",
        key: "employeeId"
    },
    {
        title: "前台角色",
        key: "role",
    },
    {
        title: "角色类型",
        key: "types",
        render(row) {
            return row.types.map((tagKey) => {
                return h(
                        NTag,
                        {
                            style: {
                                marginRight: '6px',
                                marginTop: '1px',
                                marginBottom: '1px'
                            },
                            type: 'primary',
                            bordered: false
                        },
                        {
                            default: () => tagKey
                        }
                )
            })
        }
    },
    {
        title: "创建时间",
        key: "createTime"
    },
    {
        title: "最后更新",
        key: "updateTime"
    },
    {
        title: "允许用户接口",
        key: "allowUser",
        render(row) {
            if (row.allowUser === true) {
                return "是"
            }
            return "否"
        }
    },
    {
        title: "禁用",
        key: "deleted",
        render(row) {
            if (row.deleted === true) {
                return "是"
            }
            return "否"
        }
    },
    {
        title: "操作",
        key: "actions",
        render(row) {
            return h(
                    NButtonGroup,
                    {},
                    () => [
                        h(NButton,
                                {
                                    size: 'small',
                                    onClick: () => {
                                        router.push({
                                            name: adminUserDetails,
                                            params: {
                                                userId: row.userId
                                            },
                                            query: {
                                                source: "staff"
                                            }
                                        })
                                    }
                                },
                                {default: () => "查看/编辑"}),
                        h(NButton,
                                {
                                    size: 'small',
                                    onClick: () => {
                                    }
                                },
                                {default: () => "删除"}),
                    ]
            );
        }
    }
]

const data = ref([]);

const requestForData = (page) => {
    const config = createConfig()
    config.params = {
        page: page
    }
    proxy.$axios.get(api.staffs, config).then((res) => {
        const recvData = res.data
        recvData.forEach((item) => {
            item.createTime = formatTimestamp(item.createTime)
            item.updateTime = formatTimestamp(item.updateTime)
        })
        data.value = recvData
    }).catch((err) => {
        popAdminErrorTemplate(notification, err)
    })
}

requestForData(1)

</script>

<style scoped>

</style>