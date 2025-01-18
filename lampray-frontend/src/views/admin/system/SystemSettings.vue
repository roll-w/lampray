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
    <div class="p-5 ">
        <AdminBreadcrumb :location="systemSettings" :menu="menuSystem"/>
        <n-h1>系统设置</n-h1>
        <n-text class="mt-5">
            系统内部设置调整。
        </n-text>
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
import AdminBreadcrumb from "@/components/admin/AdminBreadcrumb.vue";
import {NButton, NButtonGroup, NDataTable, NH1, NPagination, useNotification} from "naive-ui";
import {getCurrentInstance, h, ref} from "vue";
import api from "@/request/api";
import {systemSettings} from "@/router";
import {menuSystem} from "@/views/menu";
import {createConfig} from "@/request/axios_config";
import {popAdminErrorTemplate} from "@/views/utils/error";

const {proxy} = getCurrentInstance()
const notification = useNotification()

const columns = [
    {
        title: "设置名",
        key: "key"
    },
    {
        title: "设置值",
        key: "value"
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
                                        console.log("edit: " + row.key)
                                    }
                                },
                                {default: () => "编辑"}),
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

const data = ref([])

const page = ref(1)

const config = createConfig()

const getSettings = () => {
    proxy.$axios.get(api.systemSettings, config).then((res) => {
        data.value = res.data
    }).catch((err) => {
        popAdminErrorTemplate(notification, err)
    })
}

getSettings()

</script>
