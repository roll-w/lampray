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
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {useI18n} from "vue-i18n";

const axios = useAxios()
const {t} = useI18n()
const toast = useToast()

const settings = ref<SettingVo[]>([])
const loading = ref(false)
const deleting = ref(false)

const loadSettings = async () => {
    try {
        loading.value = true
        const response = await systemSettingService(axios).listSettings({
            page: 1,
            size: 100,
        })
        // TODO: fix, the input value not reflect the updated value after reload
        settings.value = response.data!
    } catch (error) {
        console.log(error)
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}


onMounted(() => {
    loadSettings()
})

const changedSettings = ref<SettingVo[]>([])

const onSettingChanged = (setting: SettingVo) => {
    const index = changedSettings.value.findIndex(s => s.key === setting.key)
    if (index !== -1) {
        changedSettings.value[index] = setting
    } else {
        changedSettings.value.push(setting)
    }
}

const onSettingReset = (setting: SettingVo) => {
    const index = changedSettings.value.findIndex(s => s.key === setting.key)
    if (index !== -1) {
        changedSettings.value.splice(index, 1)
    }
}

const onSettingDelete = async (setting: SettingVo) => {
    const index = changedSettings.value.findIndex(s => s.key === setting.key)
    if (index !== -1) {
        changedSettings.value.splice(index, 1)
    }

    if (!setting || !setting.key) return
    deleting.value = true
    try {
        await systemSettingService(axios).deleteSetting(setting.key)
        toast.add(newSuccessToast(t("request.success.title"), t("views.adminfaced.system.settings.deleteSuccess", {key: setting.key})))
        await loadSettings()
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        deleting.value = false
    }
}

const saving = ref(false)

const saveChanges = async () => {
    if (changedSettings.value.length === 0) return
    saving.value = true
    try {
        await Promise.all(changedSettings.value.map(s => {
            const value = s.value === undefined ? null : String(s.value)
            return systemSettingService(axios).setSetting(s.key, value)
        }))

        // Reload settings and clear tracked changes
        await loadSettings()
        changedSettings.value = []
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        saving.value = false
    }
}

const resetChanges = async () => {
    // Simply discard local changes and reload from server
    changedSettings.value = []
    await loadSettings()
}

</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    <span class="text-lg font-medium mr-2">{{ t('route.admin-system-settings') }}</span>
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <div class="w-full lg:w-3/5 xl:w-1/2">
                <div class="w-full flex flex-col">
                    <h3 class="text-base font-medium">{{ t('views.adminfaced.system.settings.currentSource') }}</h3>
                    <!--TODO: allow switch the source -->
                    <UStepper
                            disabled
                            :items="[
                              { title: t('views.adminfaced.system.settings.source.DATABASE') },
                              { title: t('views.adminfaced.system.settings.source.COMMAND') },
                              { title: t('views.adminfaced.system.settings.source.ENVIRONMENT') },
                              { title: t('views.adminfaced.system.settings.source.FILE') }
                            ]"
                            :model-value="4"
                            orientation="horizontal"
                            class="mt-4"
                    />
                </div>
            </div>

            <div class="mt-4 w-full lg:w-3/5 xl:w-1/2">
                <SettingEntry v-for="item in settings" :key="item.key" :setting="item"
                              :on-change="onSettingChanged"
                              :on-reset="onSettingReset"
                              :on-delete="onSettingDelete"/>
            </div>

            <Transition name="slide-fade">
                <div v-if="changedSettings.length > 0" class="fixed right-6 bottom-6 z-50">
                    <div class="flex space-x-3 bg-white dark:bg-gray-800 rounded-md p-3 items-center border border-gray-300 dark:border-gray-700 shadow-lg">
                        <UButton
                                color="primary"
                                variant="solid"
                                :loading="saving || loading"
                                :disabled="saving || loading"
                                @click="saveChanges"
                        >
                            {{ t('views.adminfaced.system.settings.saveChanges') }}
                        </UButton>
                        <UButton
                                color="neutral"
                                variant="outline"
                                :disabled="saving || loading"
                                @click="resetChanges"
                        >
                            {{ t('common.reset') }}
                        </UButton>
                        <div class="text-sm text-gray-500 ml-2">
                            {{ t('views.adminfaced.system.settings.changes', {count: changedSettings.length}) }}
                        </div>
                    </div>
                </div>
            </Transition>
        </template>
    </DashboardPanel>
</template>
