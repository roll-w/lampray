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
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {ref, watch} from "vue";
import type {UserDetailsVo} from "@/services/user/user.type.ts";
import {userManageService} from "@/services/user/user.service.ts";
import {useRouter} from "vue-router";
import type {FieldConfig} from "@/components/DynamicFieldInput.vue";
import {findFieldConfig} from "@/components/DynamicFieldInput.vue";
import {RouteName} from "@/router/routeName.ts";
import {useAxios} from "@/composables/useAxios.ts";
import {newErrorToastFromError} from "@/utils/toasts.ts";
import {useI18n} from "vue-i18n";

const router = useRouter();
const route = router.currentRoute;
const axios = useAxios();
const toast = useToast();
const {t} = useI18n();

const user = ref<UserDetailsVo | null>(null);
const rawUser = ref<UserDetailsVo | null>(null);

const loading = ref(false);

const fetchUser = async (id: string) => {
    loading.value = true;
    try {
        const response = await userManageService(axios).getUserDetails(id);
        const body = response.data;
        user.value = body.data!;
        rawUser.value = {...body.data!}; // Make a copy for reset
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false;
    }
}

watch(() => route.value.params.id, (newId) => {
    if (newId) {
        fetchUser(String(newId));
    }
}, {immediate: true});

const userDetailKeys: (keyof UserDetailsVo)[] = [
    "userId", "username", "nickname", "avatar", "cover", "role", "email",
    "birthday", "introduction", "enabled", "locked", "canceled",
    "createTime", "updateTime"
];

const userDetailFieldConfigs: FieldConfig[] = [
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
        render(value: string) {
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
        render: (value: string) => {
            return new Date(value).toLocaleString()
        }
    },
    {
        key: "updateTime",
        name: "更新时间",
        modifiable: false,
        render: (value: string) => {
            return new Date(value).toLocaleString()
        }
    }
]

// TODO: Implement actual navigations
const userRelatedResourceEntries = [
    {
        name: "用户所属用户组",
        key: "group",
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

const back = () => {
    router.push({name: RouteName.ADMIN_USER_LIST});
}

const submitUserInfoUpdate = () => {
}

const resetUserInfo = () => {
    if (rawUser.value) {
        user.value = {...rawUser.value};
    }
}

</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    <span class="text-lg font-medium mr-2">
                      用户管理
                    </span>
                    <UButton variant="soft" size="lg" @click="back">
                        返回
                    </UButton>
                </template>
                <template #right>
                    <UBadge color="info" variant="soft" size="lg">
                        {{ user?.username }}
                    </UBadge>
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <!--
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
                    </div>-->
            <UPageList class="gap-y-4">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <UPageCard title="用户关联资源入口">
                        <template #footer>
                            <div class="flex flex-wrap gap-2">
                                <UButton v-for="entry in userRelatedResourceEntries"
                                         :key="entry.key"
                                         variant="soft"
                                         size="lg"
                                         @click="() => {}">
                                    {{ entry.name }}
                                </UButton>
                            </div>
                        </template>

                    </UPageCard>

                    <UPageCard title="危险区" class="bg-linear-to-tl from-error/10 from-5% to-default">
                        <template #footer>
                            <div class="flex flex-wrap gap-2">
                                <UButton variant="soft" color="error" @click="() => {}">
                                    重置用户密码
                                </UButton>
                                <UButton variant="soft" color="error" @click="() => {}">
                                    账号状态设置
                                </UButton>
                                <UButton variant="soft" color="error" @click="() => {}">
                                    注销用户
                                </UButton>
                            </div>
                        </template>
                    </UPageCard>
                </div>


                <UPageCard title="基本用户信息">

                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        <DynamicFieldInput v-for="key in userDetailKeys"
                                           v-if="user"
                                           :key="key"
                                           :loading="loading"
                                           v-model:value="user[key]"
                                           :config="findFieldConfig(userDetailFieldConfigs, key)!"/>
                    </div>
                    <UFieldGroup class="flex justify-end mt-4 ">
                        <UButton color="primary" variant="solid" size="lg" @click="submitUserInfoUpdate">
                            保存修改
                        </UButton>
                        <UButton variant="outline" size="lg" @click="resetUserInfo">
                            重置
                        </UButton>
                    </UFieldGroup>
                </UPageCard>
            </UPageList>
        </template>
    </DashboardPanel>
</template>
