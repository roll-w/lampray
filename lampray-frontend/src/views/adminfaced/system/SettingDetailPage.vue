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
import {computed, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useAxios} from "@/composables/useAxios";
import {systemSettingService} from "@/services/system/system.service.ts";
import type {SettingDetailsVo} from "@/services/system/system.type.ts";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {RouteName} from "@/router/routeName.ts";
import SettingLayersLine from "@/components/admin/system/SettingLayersLine.vue";

const route = useRoute();
const router = useRouter();
const axios = useAxios();
const toast = useToast();
const {t} = useI18n();

const loading = ref(false);
const setting = ref<SettingDetailsVo | null>(null);

const editModalState = ref(false);
const deleteModalState = ref(false);
const editingValue = ref<any>(null);
const savingEdit = ref(false);
const deleting = ref(false);

const showSecret = ref(false);

const loadSetting = async (key: string) => {
    loading.value = true;
    try {
        const body = await systemSettingService(axios).getSetting(key);
        setting.value = body.data ?? null;
    } catch (err) {
        toast.add(newErrorToastFromError(err, t("request.error.title")));
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    const key = String(route.params?.key || "");
    if (!key) {
        toast.add(newErrorToastFromError(new Error(t("views.adminfaced.system.settings.detail.missingKey")), t("request.error.title")));
        router.push({name: RouteName.ADMIN_SYSTEM_SETTINGS});
        return;
    }
    loadSetting(key);
});

const defaultsIndexSet = computed(() => {
    const set = new Set<number>();
    if (!setting.value) return set;
    const settingValue: any = setting.value as any;
    if (Array.isArray(settingValue.defaults) && settingValue.defaults.length > 0) {
        for (const i of settingValue.defaults) {
            if (typeof i === 'number') set.add(i);
        }
    } else if (typeof settingValue.defaultValue === 'number') {
        set.add(settingValue.defaultValue);
    }
    return set;
});


const showEditModal = () => {
    if (!setting.value) return;
    if (setting.value.value !== undefined) {
        editingValue.value = setting.value.value
    } else if (setting.value.valueEntries && setting.value.valueEntries.length > 0) {
        editingValue.value = setting.value.valueEntries[0]
    } else {
        editingValue.value = null
    }
    showSecret.value = false;
    editModalState.value = true;
}

const submitEdit = async () => {
    if (!setting.value) return;
    savingEdit.value = true;
    try {
        // TODO: validate editingValue based on setting.type
        // Convert editingValue to string (stringify objects/arrays)
        let payload: string | null;
        if (editingValue.value === null || editingValue.value === undefined) {
            payload = null;
        } else if (typeof editingValue.value === 'string') {
            payload = editingValue.value;
        } else if (typeof editingValue.value === 'number' || typeof editingValue.value === 'boolean') {
            payload = String(editingValue.value);
        } else {
            payload = JSON.stringify(editingValue.value);
        }
        await systemSettingService(axios).setSetting(setting.value.key, payload);
        toast.add(newSuccessToast(t('request.success.title'), t('views.adminfaced.system.settings.saveChanges')));
        editModalState.value = false;
        await loadSetting(setting.value.key);
    } catch (err) {
        toast.add(newErrorToastFromError(err, t('request.error.title')));
    } finally {
        savingEdit.value = false;
    }
}

const confirmDelete = async () => {
    if (!setting.value) return;
    deleting.value = true;
    try {
        await systemSettingService(axios).deleteSetting(setting.value.key);
        toast.add(newSuccessToast(t('request.success.title'), t('views.adminfaced.system.settings.deleteSuccess', {key: setting.value.key})))
        deleteModalState.value = false;
        await loadSetting(setting.value.key);
    } catch (err) {
        toast.add(newErrorToastFromError(err, t('request.error.title')));
    } finally {
        deleting.value = false;
    }
}

const back = () => {
    router.push({name: RouteName.ADMIN_SYSTEM_SETTINGS});
};
</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    <span class="text-lg font-medium mr-2">{{
                            t('views.adminfaced.system.settings.detail.title')
                        }}</span>
                </template>
                <template #right>
                    <div class="gap-2 flex">
                        <UButton color="primary" variant="outline" @click="showEditModal">{{
                                t('common.edit')
                            }}
                        </UButton>
                        <UButton color="error" variant="outline" size="lg" @click="deleteModalState = true">
                            {{ t('common.delete') }}
                        </UButton>
                        <UButton variant="soft" size="lg" @click="back">{{ t('common.back') }}</UButton>
                    </div>
                </template>
            </UDashboardNavbar>
        </template>

        <template #body>
            <UPageList>
                <div class="w-full">
                    <UPageCard>
                        <template #title>
                            <div class="flex gap-3 items-center">
                                <div class="text-lg font-medium">
                                    <span>{{
                                            setting ? setting.key : t('views.adminfaced.system.settings.detail.loadingTitle')
                                        }}</span>
                                    <span v-if="setting?.required" class="text-red-500 ml-1" title="Required">*</span>
                                </div>
                                <UBadge v-if="setting" variant="subtle" color="success" size="lg">
                                    {{ setting.source }}
                                </UBadge>
                            </div>
                        </template>

                        <template #footer>
                            <div>
                                <div class="w-full">
                                    <div class="text-sm text-gray-500 whitespace-pre-line">{{
                                            setting?.description
                                        }}
                                    </div>
                                </div>
                            </div>
                        </template>

                        <div class="text-sm text-gray-500">{{
                                t('views.adminfaced.system.settings.detail.type')
                            }}: {{ setting?.type }}
                        </div>
                        <div v-if="loading" class="py-6">
                            <div class="text-center text-gray-500">{{ t('common.loading') }}</div>
                        </div>
                        <div v-else-if="!setting" class="py-6">
                            <div class="text-center text-gray-500">
                                {{ t('views.adminfaced.system.settings.detail.notFound') }}
                            </div>
                        </div>
                    </UPageCard>

                    <UPageCard class="mt-4" :title="t('views.adminfaced.system.settings.detail.layersTitle')">
                        <SettingLayersLine :layers="setting?.layers" :activeSource="setting?.source"/>
                    </UPageCard>

                    <UPageCard class="mt-4" :title="t('views.adminfaced.system.settings.detail.valueEntriesTitle')">
                        <div v-if="setting?.valueEntries && setting.valueEntries.length > 0" class="space-y-2">
                            <div v-for="(entry, idx) in setting.valueEntries" :key="idx"
                                 class="p-3 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-700 text-sm">
                                <div class="flex items-center justify-between">
                                    <div class="flex items-center gap-3">
                                        <div class="text-sm text-gray-500 dark:text-gray-400">{{
                                                t('views.adminfaced.system.settings.detail.valueEntryIndex', {index: idx})
                                            }}
                                        </div>
                                        <UBadge v-if="defaultsIndexSet.has(idx)" variant="solid" color="primary"
                                                size="md">
                                            {{ t('views.adminfaced.system.settings.detail.defaultBadgeText') }}
                                        </UBadge>
                                    </div>
                                </div>
                                <div class="mt-2 text-lg">
                                    <template v-if="entry === null || entry === undefined || entry === ''">
                                        <div>
                                            {{ t('views.adminfaced.system.settings.detail.noValue') }}
                                        </div>
                                    </template>
                                    <template v-else-if="Array.isArray(entry)">
                                        <ul class="list-disc pl-5">
                                            <li v-for="(it, i) in entry" :key="i">
                                                {{ typeof it === 'object' ? JSON.stringify(it) : String(it) }}
                                            </li>
                                        </ul>
                                    </template>
                                    <template v-else-if="typeof entry === 'object'">
                                        <div class="p-2 rounded overflow-auto">
                                            <pre class="whitespace-pre-wrap">{{ JSON.stringify(entry, null, 2) }}</pre>
                                        </div>
                                    </template>
                                    <template v-else>
                                        <div>{{ String(entry) }}</div>
                                    </template>
                                </div>
                            </div>
                        </div>
                        <div v-else class="text-gray-500">
                            {{ t('views.adminfaced.system.settings.detail.noValueEntries') }}
                        </div>
                    </UPageCard>
                </div>
            </UPageList>
        </template>
    </DashboardPanel>

    <UModal v-model:open="editModalState">
        <template #content>
            <div class="p-4">
                <div class="text-lg font-medium mb-2">{{ t('views.adminfaced.system.settings.detail.editTitle') }}</div>
                <div class="mb-2 text-sm text-gray-500">{{ t('views.adminfaced.system.settings.detail.editHint') }}</div>
                <UInput v-model="editingValue"
                        :type="setting?.secret ? (showSecret ? 'text' : 'password') : 'text'"
                        class="w-full p-2">
                    <template #trailing v-if="setting?.secret">
                        <UButton
                                color="neutral"
                                variant="link"
                                size="sm"
                                :icon="showSecret ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                                :aria-label="showSecret ? t('views.adminfaced.system.settings.hideSecret') : t('views.adminfaced.system.settings.showSecret')"
                                :aria-pressed="showSecret"
                                @click="showSecret = !showSecret"
                        />
                    </template>
                </UInput>
                <div class="flex justify-end gap-2 mt-4">
                    <UButton variant="outline" @click="editModalState = false">{{ t('common.cancel') }}</UButton>
                    <UButton color="primary" :loading="savingEdit" @click="submitEdit">{{
                            t('common.submit')
                        }}
                    </UButton>
                </div>
            </div>
        </template>
    </UModal>

    <UModal v-model:open="deleteModalState">
        <template #content>
            <div class="p-4">
                <div class="text-lg font-medium mb-2">{{ t('views.adminfaced.system.settings.deleteTitle') }}</div>
                <p class="mb-4">{{ t('views.adminfaced.system.settings.deleteDescription', {key: setting?.key}) }}</p>
                <div class="flex justify-end gap-2">
                    <UButton variant="outline" @click="deleteModalState = false">{{ t('common.cancel') }}</UButton>
                    <UButton color="error" :loading="deleting" @click="confirmDelete">{{ t('common.delete') }}</UButton>
                </div>
            </div>
        </template>
    </UModal>
</template>
