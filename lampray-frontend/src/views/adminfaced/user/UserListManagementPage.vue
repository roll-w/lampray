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
import {h, onMounted, ref, resolveComponent} from "vue"
import {useAxios} from "@/composables/useAxios"
import {userManageService} from "@/services/user/user.service"
import type {UserDetailsVo} from "@/services/user/user.type"
import type {TableColumn} from "@nuxt/ui";
import {RouteName} from "@/router/routeName.ts";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError} from "@/utils/toasts.ts";

const UButton = resolveComponent("UButton")
const RouterLink = resolveComponent("RouterLink")
const UBadge = resolveComponent("UBadge")

const axios = useAxios()
const userManage = userManageService(axios)
const toast = useToast()
const {t} = useI18n()


const users = ref<UserDetailsVo[]>([])
const loading = ref(false)

const columns: TableColumn<UserDetailsVo>[] = [
    {
        accessorKey: "userId",
        header: "#",
    },
    {
        accessorKey: "username",
        header: "用户名",
    },
    {
        accessorKey: "role",
        header: "角色",
        cell: ({row}) => {
            const label = roleOptions.find(option => option.value === row.original.role)?.label || row.original.role
            return h(UBadge, {color: "primary", variant: "outline", size: "lg"}, () => label)
        }
    },
    {
        accessorKey: "email",
        header: "邮件"
    },
    {
        header: "状态",
        cell: ({row}) => {
            const user = row.original
            return h(UBadge, {
                color: getStatusColor(user),
                variant: "soft",
                size: "lg"
            }, () => getStatusText(user))
        }
    },
    {
        header: "创建时间",
        cell: ({row}) => formatDate(row.original.createTime)
    },
    {
        header: "更新时间",
        cell: ({row}) => formatDate(row.original.updateTime)
    },
    {
        id: "actions",
        cell: ({row}) => {
            return h(UButton, {
                        variant: "link",
                        size: "lg",
                    }, () => h(RouterLink, {
                        to: {
                            name: RouteName.ADMIN_USER_DETAIL, params: {id: row.original.userId}
                        }
                    }, () => "详情")
            )
        }
    }
]


const roleOptions = [
    {label: "用户", value: "USER"},
    {label: "管理员", value: "ADMIN"},
    {label: "工作人员", value: "STAFF"},
    {label: "审核", value: "REVIEWER"},
    {label: "客服", value: "CUSTOMER_SERVICE"},
    {label: "编辑", value: "EDITOR"}
]

const loadUsers = async () => {
    try {
        loading.value = true
        const response = await userManage.listUsers({
            page: 1,
            size: 10
        })
        const body = response.data
        users.value = body.data || []
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}

const getStatusColor = (user: UserDetailsVo) => {
    if (!user.enabled) return "error"
    if (user.locked) return "neutral"
    if (user.canceled) return "warning"
    return "success"
}

const getStatusText = (user: UserDetailsVo) => {
    if (!user.enabled) return "已禁用"
    if (user.locked) return "锁定"
    if (user.canceled) return "已注销"
    return "正常"
}

const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString()
}

const createUser = async () => {
    try {
        loading.value = true
        await loadUsers()
        // Show success toast
    } catch (error) {
        console.error("Failed to create user:", error)
        // Show error toast
    } finally {
        loading.value = false
    }
}

// Lifecycle
onMounted(() => {
    loadUsers()
})

</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                   <span class="text-lg font-medium mr-2">
                      用户管理
                    </span>
                </template>
                <template #right>
                    <UButton color="primary" variant="solid">
                        创建用户
                    </UButton>
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <!--TODO: a common table component-->
            <UTable :data="users" :columns="columns"
                    class="flex-1"
                    :loading="loading"
                    sticky
                    :ui="{
                        separator: 'hidden',
                        thead: 'bg-elevated/50 [&>tr]:after:content-none',
                        tbody: '[&>tr]:last:[&>td]:border-b-0',
                        th: 'sticky top-0 py-4 first:rounded-l-lg last:rounded-r-lg',
                        td: 'border-b border-default text-md text-black dark:text-white ',
                    }"
            >
                <template #expanded="{ row }">
                    <pre>{{ row.original }}</pre>
                </template>
            </UTable>
        </template>
    </DashboardPanel>
</template>
