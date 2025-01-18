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
        <AdminBreadcrumb :location="menuLocation" :menu="menuUsers"/>
        <div class="flex items-baseline mt-5">
            <n-h1>用户详情</n-h1>
            <div class="flex flex-grow justify-end">
                <n-button @click="back()">回退</n-button>
            </div>
        </div>
        <n-h2>用户关联资源管理入口</n-h2>
        <div>
            <n-space>
                <n-button v-for="entry in userRelatedResourceEntries"
                          secondary
                          size="large" type="primary" @click="handleUserRelatedClick(entry.key)">
                    {{ entry.name }}
                </n-button>
            </n-space>
        </div>
        <n-h2>
            危险区
        </n-h2>
        <div>
            <n-space>
                <n-button secondary type="error" @click="showResetPasswordModal = true">
                    重置用户密码
                </n-button>
                <n-button secondary type="error">
                    账号状态设置
                </n-button>
                <n-button secondary type="error">
                    注销用户
                </n-button>
            </n-space>
        </div>

        <n-h2>基本用户信息</n-h2>
        <div>
            <n-table :bordered="false" :single-line="true">
                <div v-for="info in userInfoPairs">
                    <DisplayInput v-model:value="formValues.user[info.key]"
                                  :config="findFieldConfig(info.key)"/>
                </div>
            </n-table>
        </div>
        <n-h2>工作人员信息</n-h2>
        <n-alert v-if="!isStaff(userInfo.role || 'USER')"
                 :bordered="false" type="warning">
            <n-text tag="p">
                此用户无工作人员信息。
            </n-text>
        </n-alert>
        <n-table :bordered="false" :single-line="true">
            <div v-for="info in staffInfoPairs">
                <DisplayInput v-model:value="formValues.staff[info.key]"
                              :config="findFieldConfig(info.key)"/>
            </div>
        </n-table>

        <div>
            <n-modal v-model:show="showResetPasswordModal"
                     :show-icon="false"
                     preset="dialog"
                     title="重置密码"
                     transform-origin="center">
            </n-modal>
        </div>
    </div>
</template>

<script setup>
import {NAlert, NButton, NH1, NH2, NModal, NSpace, NTable, useNotification} from "naive-ui";
import {useRouter} from "vue-router";
import AdminBreadcrumb from "@/components/admin/AdminBreadcrumb.vue";
import {adminStaffs, adminUsers} from "@/router";
import {menuUsers} from "@/views/menu";
import api from "@/request/api";
import {createConfig} from "@/request/axios_config";
import {getCurrentInstance, ref} from "vue";
import {popAdminErrorTemplate} from "@/views/utils/error";
import DisplayInput from "@/components/DisplayInput.vue";
import {formatTimestamp} from "@/util/format";
import {useUserRulesOf} from "@/views/rules";

const {proxy} = getCurrentInstance()

const notification = useNotification()

const router = useRouter()
const userId = router.currentRoute.value.params.userId
const source = router.currentRoute.value.query.source

const showResetPasswordModal = ref(false)

const userInfoPairs = ref([])
const userInfo = ref({})

const staffInfoPairs = ref([])
const staffInfo = ref({})

const menuLocation = ref(adminUsers)

const isStaffSource = () => {
    return source === "staff"
}
isStaffSource() ? menuLocation.value = adminStaffs : menuLocation.value = adminUsers


const formRules = useUserRulesOf(['nickname', 'email', 'role'])
const formValues = ref({
    user: {},
    staff: {}
})

const userRelatedResourceEntries = [
    {
        name: "用户所属用户组",
        key: "group",
    },
    {
        name: "用户文件",
        key: "file",
    },
    {
        name: "用户分享",
        key: "share",
    },
    {
        name: "用户操作日志",
        key: "operationLog",
    },
    {
        name: "用户登录日志",
        key: "loginLog",
    },
    {
        name: "用户数据",
        key: "data",
    },
]

const fieldConfig = [
    {
        key: "userId",
        name: "用户ID",
        modifiable: false,
    },
    {
        key: "username",
        name: "用户名",
        modifiable: false,
    },
    {
        key: "nickname",
        name: "昵称",
        modifiable: true,
        placeholder: "请输入昵称，不填写则默认为用户名",
    },
    {
        key: "avatar",
        name: "头像",
        modifiable: true,
        info: "头像尺寸为 200x200",
        type: "image",
        extra: {
            preset: "avatar",
        },
    },
    {
        key: "cover",
        name: "封面",
        modifiable: true,
        type: "image",
        extra: {
            preset: "avatar",
        },
        render(value) {
            return value + "?q=75"
        }
    },
    {
        key: "role",
        name: "角色",
        modifiable: true,
        type: 'select',
        options: [
            {
                label: "管理员",
                value: "ADMIN"
            },
            {
                label: "普通用户",
                value: "USER"
            },
        ]
    },
    {
        key: "email",
        name: "邮箱",
        modifiable: true,
        placeholder: "请输入邮箱",
    },
    {
        key: "birthday",
        name: "生日",
        modifiable: true,
        type: "date",
    },
    {
        key: "introduction",
        name: "简介",
        modifiable: true,
        type: "text",
    },
    {
        key: "enabled",
        name: "是否启用",
        tip: "启用影响",
        modifiable: false,
        type: "checkbox",
    },
    {
        key: "locked",
        name: "是否锁定",
        modifiable: false,
        type: "checkbox",
    },
    {
        key: "canceled",
        name: "是否注销",
        modifiable: false,
        type: "checkbox",
    },
    {
        key: "id",
        name: "工作人员ID",
        modifiable: false,
    },
    {
        key: "types",
        name: "工作人员类型",
        modifiable: true,
        type: "select",
        extra: {
            multiple: true
        },
        options: [
            {
                label: "未分配",
                value: "UNASSIGNED"
            },
            {
                label: "管理员",
                value: "ADMIN"
            },
            {
                label: "审核员",
                value: "REVIEWER"

            },
            {
                label: "编辑员",
                value: "EDITOR"

            },
            {
                label: "客服",
                value: "CUSTOMER_SERVICE"
            }
        ]
    },
    {
        key: "createTime",
        name: "创建时间",
        modifiable: false,
        render: (value) => {
            return formatTimestamp(value)
        }
    },
    {
        key: "updateTime",
        name: "更新时间",
        modifiable: false,
        render: (value) => {
            return formatTimestamp(value)
        }
    }
]

const findFieldConfig = (key) => {
    for (let i = 0; i < fieldConfig.length; i++) {
        if (fieldConfig[i].key === key) {
            return fieldConfig[i]
        }
    }
    return null
}

const back = () => {
    if (isStaffSource()) {
        router.push({
            name: adminStaffs
        })
        return
    }
    router.push({
        name: adminUsers
    })
}

const handleUserRelatedClick = (key) => {
    switch (key) {
        case "group":
            break
        case "file":
            break
        case "share":
            break
        case "operationLog":
            break
        case "loginLog":
            break
        case "data":
            break
    }
}

const sortByFieldConfig = (pairsRef) => {
    const result = []
    for (let i = 0; i < fieldConfig.length; i++) {
        for (let j = 0; j < pairsRef.value.length; j++) {
            if (fieldConfig[i].key === pairsRef.value[j].key) {
                result.push(pairsRef.value[j])
            }
        }
    }
    pairsRef.value = result
}


const requestUserDetails = () => {
    const config = createConfig()
    proxy.$axios.get(api.users(true, userId), config).then((res) => {
        const pairs = []

        if (isStaff(res.data.role)) {
            requestStaffInfo(userId)
        }
        formValues.value.user = res.data
        userInfo.value = res.data
        for (let dataKey in res.data) {
            pairs.push({
                key: dataKey,
            })
        }
        userInfoPairs.value = pairs
        sortByFieldConfig(userInfoPairs)
    }).catch((error) => {
        popAdminErrorTemplate(notification, error)
    })
}

const requestStaffInfo = (userId) => {
    const config = createConfig()
    proxy.$axios.get(api.staffInfoByUser(userId), config).then((res) => {
        staffInfo.value = res.data
        formValues.value.staff = res.data
        const pairs = []
        for (let dataKey in res.data) {
            if (dataKey === 'role') {
                // no need to display 'role' as it is
                // already displayed in user info
                continue
            }
            pairs.push({
                key: dataKey,
            })
        }
        staffInfoPairs.value = pairs
        sortByFieldConfig(staffInfoPairs)
    }).catch((error) => {
        popAdminErrorTemplate(notification, error)
    })
}

const isStaff = (role) => {
    return role !== "USER";
}

requestUserDetails()

</script>