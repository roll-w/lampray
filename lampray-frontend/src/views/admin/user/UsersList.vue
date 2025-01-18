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
        <AdminBreadcrumb :location="adminUsers" :menu="menuUsers"/>
        <div class="flex items-baseline mt-5">
            <n-h1>用户列表</n-h1>
            <div class="flex flex-grow justify-end">
                <n-button>创建新用户</n-button>
            </div>
        </div>
        <n-data-table
                :bordered="false"
                :columns="columns"
                :data="data"
                :pagination="false"
                class="mt-5"
        />
        <FastPagination :page="page" :route-name="adminUsers"/>
    </div>
</template>

<script setup>
import {getCurrentInstance, h, ref} from "vue";
import {NButton, NButtonGroup, NDataTable, NTag, useDialog, useMessage, useNotification} from "naive-ui";
import api from "@/request/api";
import {useUserStore} from "@/stores/user";
import AdminBreadcrumb from "@/components/admin/AdminBreadcrumb.vue";
import {formatTimestamp} from "@/util/format";
import {adminUserDetails, adminUsers} from "@/router";
import {menuUsers} from "@/views/menu";
import {useRouter} from "vue-router";
import {usePage} from "@/views/utils/pages";
import {createConfig} from "@/request/axios_config";
import FastPagination from "@/components/FastPagination.vue";
import {getRoleName} from "@/views/names";

const router = useRouter()
const {proxy} = getCurrentInstance()
const notification = useNotification()
const message = useMessage()
const dialog = useDialog()

const userStore = useUserStore()

const page = usePage()

const columns = [
    {
        title: "ID",
        key: "userId",
    },
    {
        title: "用户名",
        key: "username"
    },
    {
        title: "昵称",
        key: "nickname"
    },
    {
        title: "角色",
        key: "role",
        render(row) {
            return h(
                    NTag,
                    {
                        type: "primary",
                        bordered: false,
                        size: "medium"
                    },
                    {
                        default: () => getRoleName(row.role)
                    }
            )
        }
    },
    {
        title: "电子邮箱",
        key: "email",
    },
    {
        title: "注册时间",
        key: "createTime",
        render(row) {
            return formatTimestamp(row.createTime)
        },
    },
    {
        title: "已启用",
        key: "enabled",
        render(row) {
            if (row.enabled === true) {
                return "是"
            }
            return "否"
        }
    },
    {
        title: "已锁定",
        key: "locked",
        render(row) {
            if (row.locked === true) {
                return "是"
            }
            return "否"
        }
    },
    {
        title: "已注销",
        key: "canceled",
        render(row) {
            if (row.canceled === true) {
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
                                            }
                                        })
                                    }
                                },
                                {default: () => "查看/编辑"}),
                        h(NButton,
                                {
                                    size: 'small',
                                    onClick: () => {
                                        handleDeleteUser(row)
                                    }
                                },
                                {default: () => "删除"}),
                    ]
            );
        }
    }
]

const data = ref([]);

const handleDeleteUser = (user) => {
    dialog.error({
        title: "确认删除",
        transformOrigin: "center",
        content: `请确认是否删除用户名为'${user.username}'，id为'${user.userId}'的用户？\n删除后用户将转为注销状态。`,
        negativeText: "取消",
        positiveText: "确认",
        onPositiveClick: () => {
            console.log("delete user of " + user.userId)
        },
        onNegativeClick: () => {

        }
    })
}

const requestForData = () => {
    const config = createConfig()
    config.params = {
        page: page.value.page,
    }
    proxy.$axios.get(api.userList, config).then((res) => {
        page.value.count = Math.ceil(res.total / res.size)
        page.value.page = res.page
        data.value = res.data
    }).catch((err) => {
        console.log(err)
    })
}

requestForData(1, 10)

</script>

<style scoped>

</style>