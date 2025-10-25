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
import {onMounted, ref} from "vue"
import {useAxios} from "@/composables/useAxios"
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {systemSettingService} from "@/services/system/system.service.ts";
import type {SettingVo} from "@/services/system/system.type.ts";
import {newErrorToastFromError} from "@/utils/toasts.ts";
import {useI18n} from "vue-i18n";

const axios = useAxios()
const {t} = useI18n()
const toast = useToast()

const settings = ref<SettingVo[]>([])
const loading = ref(false)

const loadSettings = async () => {
    try {
        loading.value = true
        systemSettingService(axios).listSettings({
            page: 1,
            size: 100,
        }).then(response => {
            settings.value = response.data!
        })
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}


onMounted(() => {
    loadSettings()
})

</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                   <span class="text-lg font-medium mr-2">
                      设置管理
                    </span>
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <div class="w-full lg:w-3/5 xl:w-1/2">
                <div class="w-full flex flex-col">
                    <h3 class="text-base font-medium">当前生效配置源</h3>
                    <!--TODO: allow switch the source -->
                    <UStepper
                            disabled
                            :items="[
                              { title: '数据库' },
                              { title: '命令行' },
                              { title: '环境变量' },
                              { title: '配置文件' }
                            ]"
                            :model-value="4"
                            orientation="horizontal"
                            class="mt-4"
                    />
                </div>
            </div>

            <div class="mt-4 w-full lg:w-3/5 xl:w-1/2">
                <SettingEntry v-for="item in settings" :key="item.key" :setting="item"/>
            </div>
        </template>
    </DashboardPanel>
</template>
