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
import {h, onMounted, reactive, ref, resolveComponent} from "vue";
import {useAxios} from "@/composables/useAxios";
import {firewallService} from "@/services/system/firewall.service";
import type {AddFilterEntryRequest, FilterEntryVo, UpdateFilterEntryRequest} from "@/services/system/firewall.type";
import {
    FilterIdentifierType as FilterIdentifierTypeConst,
    FilterMode as FilterModeConst
} from "@/services/system/firewall.type";
import type {FormSubmitEvent, TableColumn} from "@nuxt/ui";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import DurationInput from "@/components/DurationInput.vue";
import {useI18n} from "vue-i18n";
import {newErrorToastFromError, newSuccessToast} from "@/utils/toasts.ts";
import {z} from "zod";

const UButton = resolveComponent("UButton")
const UBadge = resolveComponent("UBadge")

const axios = useAxios()
const firewall = firewallService(axios)
const toast = useToast()
const {t} = useI18n()

const entries = ref<FilterEntryVo[]>([])
const loading = ref(false)
const isAddModalOpen = ref(false)
const isEditModalOpen = ref(false)
const isDeleteModalOpen = ref(false)
const isClearModalOpen = ref(false)
const selectedEntry = ref<FilterEntryVo | null>(null)

// Zod validation schema
const filterEntrySchema = z.object({
    identifier: z.string()
            .min(1, t("views.adminfaced.system.firewall.validation.identifierRequired"))
            .refine((val) => {
                // Basic IP validation or user ID validation
                const ipPattern = /^(\d{1,3}\.){3}\d{1,3}(\/\d{1,2})?$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}(\/\d{1,3})?$/;
                const userIdPattern = /^\d+$/;
                return ipPattern.test(val) || userIdPattern.test(val);
            }, t("views.adminfaced.system.firewall.validation.identifierInvalid")),
    type: z.enum([FilterIdentifierTypeConst.IP, FilterIdentifierTypeConst.USER], {
        message: t("views.adminfaced.system.firewall.validation.typeRequired")
    }),
    mode: z.enum([FilterModeConst.ALLOW, FilterModeConst.DENY], {
        message: t("views.adminfaced.system.firewall.validation.modeRequired")
    }),
    expirationSeconds: z.number(),
    reason: z.string().min(1, t("views.adminfaced.system.firewall.validation.reasonRequired"))
})

type FilterEntrySchema = z.output<typeof filterEntrySchema>

const newEntryState = reactive<Partial<FilterEntrySchema>>({
    identifier: "",
    type: FilterIdentifierTypeConst.IP,
    mode: FilterModeConst.DENY,
    expirationSeconds: -1,
    reason: ""
})

const editingEntryState = reactive<Partial<FilterEntrySchema>>({
    identifier: "",
    type: FilterIdentifierTypeConst.IP,
    mode: FilterModeConst.DENY,
    expirationSeconds: -1,
    reason: ""
})

const columns: TableColumn<FilterEntryVo>[] = [
    {
        accessorKey: "identifier",
        header: () => t("views.adminfaced.system.firewall.identifier"),
    },
    {
        accessorKey: "type",
        header: () => t("views.adminfaced.system.firewall.type"),
        cell: ({row}) => {
            return h(UBadge, {
                color: "primary",
                variant: "outline",
                size: "lg"
            }, () => t(`views.adminfaced.system.firewall.identifierType.${row.original.type}`))
        }
    },
    {
        accessorKey: "mode",
        header: () => t("views.adminfaced.system.firewall.mode"),
        cell: ({row}) => {
            const color = row.original.mode === FilterModeConst.DENY ? "error" : "success"
            return h(UBadge, {
                color: color,
                variant: "soft",
                size: "lg"
            }, () => t(`views.adminfaced.system.firewall.filterMode.${row.original.mode}`))
        }
    },
    {
        accessorKey: "expiration",
        header: () => t("views.adminfaced.system.firewall.expiration"),
        cell: ({row}) => formatDate(row.original.expiration)
    },
    {
        accessorKey: "reason",
        header: () => t("views.adminfaced.system.firewall.reason"),
    },
    {
        id: "actions",
        header: () => t("views.adminfaced.system.firewall.actions"),
        cell: ({row}) => {
            return h("div", {class: "flex gap-2"}, [
                h(UButton, {
                    variant: "ghost",
                    color: "primary",
                    size: "sm",
                    onClick: () => openEditModal(row.original)
                }, () => t("views.adminfaced.system.firewall.edit")),
                h(UButton, {
                    variant: "ghost",
                    color: "error",
                    size: "sm",
                    onClick: () => openDeleteModal(row.original)
                }, () => t("views.adminfaced.system.firewall.delete"))
            ])
        }
    }
]

const identifierTypeOptions = [
    {label: t("views.adminfaced.system.firewall.identifierType.IP"), value: FilterIdentifierTypeConst.IP},
    {label: t("views.adminfaced.system.firewall.identifierType.USER"), value: FilterIdentifierTypeConst.USER}
]

const filterModeOptions = [
    {label: t("views.adminfaced.system.firewall.filterMode.ALLOW"), value: FilterModeConst.ALLOW},
    {label: t("views.adminfaced.system.firewall.filterMode.DENY"), value: FilterModeConst.DENY}
]

const loadFilterTable = async () => {
    try {
        loading.value = true
        const response = await firewall.getFilterTable()
        const body = response.data
        entries.value = body.data || []
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}

const openAddModal = () => {
    Object.assign(newEntryState, {
        identifier: "",
        type: FilterIdentifierTypeConst.IP,
        mode: FilterModeConst.DENY,
        expirationSeconds: -1,
        reason: ""
    })
    isAddModalOpen.value = true
}

const closeAddModal = () => {
    isAddModalOpen.value = false
}

const addFilterEntry = async (event: FormSubmitEvent<FilterEntrySchema>) => {
    try {
        loading.value = true
        await firewall.addFilterEntry(event.data as AddFilterEntryRequest)
        toast.add(newSuccessToast(t("views.adminfaced.system.firewall.addSuccess")))
        closeAddModal()
        await loadFilterTable()
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}

const openEditModal = (entry: FilterEntryVo) => {
    const expirationDate = new Date(entry.expiration)
    const now = new Date()
    let seconds = Math.floor((expirationDate.getTime() - now.getTime()) / 1000)

    if (expirationDate.getFullYear() > 9000) {
        seconds = -1
    }

    Object.assign(editingEntryState, {
        identifier: entry.identifier,
        type: entry.type,
        mode: entry.mode,
        expirationSeconds: seconds > 0 ? seconds : -1,
        reason: entry.reason
    })
    isEditModalOpen.value = true
}

const closeEditModal = () => {
    isEditModalOpen.value = false
}

const updateFilterEntry = async (event: FormSubmitEvent<FilterEntrySchema>) => {
    try {
        loading.value = true
        await firewall.updateFilterEntry(event.data as UpdateFilterEntryRequest)
        toast.add(newSuccessToast(t("views.adminfaced.system.firewall.updateSuccess")))
        closeEditModal()
        await loadFilterTable()
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}

const openDeleteModal = (entry: FilterEntryVo) => {
    selectedEntry.value = entry
    isDeleteModalOpen.value = true
}

const closeDeleteModal = () => {
    isDeleteModalOpen.value = false
    selectedEntry.value = null
}

const deleteFilterEntry = async () => {
    if (!selectedEntry.value) return

    try {
        loading.value = true
        await firewall.removeFilterEntry(selectedEntry.value.identifier, selectedEntry.value.type)
        toast.add(newSuccessToast(t("views.adminfaced.system.firewall.deleteSuccess")))
        closeDeleteModal()
        await loadFilterTable()
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}

const openClearModal = () => {
    isClearModalOpen.value = true
}

const closeClearModal = () => {
    isClearModalOpen.value = false
}

const clearFilterTable = async () => {
    try {
        loading.value = true
        await firewall.clearFilterTable()
        toast.add(newSuccessToast(t("views.adminfaced.system.firewall.clearSuccess")))
        closeClearModal()
        await loadFilterTable()
    } catch (error) {
        toast.add(newErrorToastFromError(error, t("request.error.title")))
    } finally {
        loading.value = false
    }
}

const formatDate = (dateString: string) => {
    try {
        const date = new Date(dateString)
        if (date.getFullYear() > 9000 || isNaN(date.getTime())) {
            return t("views.adminfaced.system.firewall.permanent")
        }
        return date.toLocaleString()
    } catch {
        return dateString
    }
}

onMounted(() => {
    loadFilterTable()
})
</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    <div class="flex flex-col">
                        <span class="text-lg font-medium">
                            {{ t("views.adminfaced.system.firewall.filterTable") }}
                        </span>
                        <span class="text-sm text-neutral-500 font-normal mt-1">
                            {{ t("views.adminfaced.system.firewall.filterTableDescription") }}
                        </span>
                    </div>
                </template>
                <template #right>
                    <div class="flex gap-2">
                        <UButton
                                color="error"
                                variant="outline"
                                @click="openClearModal"
                                :disabled="entries.length === 0"
                        >
                            {{ t("views.adminfaced.system.firewall.clear") }}
                        </UButton>
                        <UButton color="primary" variant="solid" @click="openAddModal">
                            {{ t("views.adminfaced.system.firewall.addEntry") }}
                        </UButton>
                    </div>
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <UTable
                    :data="entries"
                    :columns="columns"
                    class="flex-1"
                    :loading="loading"
                    sticky
                    :ui="{
                    separator: 'hidden',
                    thead: 'bg-elevated/50 [&>tr]:after:content-none',
                    tbody: '[&>tr]:last:[&>td]:border-b-0',
                    th: 'sticky top-0 py-4 first:rounded-l-lg last:rounded-r-lg',
                    td: 'border-b border-default text-md text-black dark:text-white',
                }"
            >
                <template #empty>
                    <div class="text-center py-8 text-neutral-500">
                        {{ t("views.adminfaced.system.firewall.noEntries") }}
                    </div>
                </template>
            </UTable>
        </template>
    </DashboardPanel>

    <UModal v-model:open="isAddModalOpen" :title="t('views.adminfaced.system.firewall.addEntry')">
        <template #body>
            <UForm :schema="filterEntrySchema" :state="newEntryState" @submit="addFilterEntry">
                <div class="space-y-4 p-4">
                    <UFormField :label="t('views.adminfaced.system.firewall.identifier')" name="identifier" required>
                        <UInput
                                v-model="newEntryState.identifier"
                                :placeholder="t('views.adminfaced.system.firewall.identifierPlaceholder')"
                                class="w-full"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.type')" name="type" required>
                        <USelectMenu
                                v-model="newEntryState.type"
                                :items="identifierTypeOptions"
                                value-key="value"
                                class="w-full"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.mode')" name="mode" required>
                        <USelectMenu
                                v-model="newEntryState.mode"
                                :items="filterModeOptions"
                                value-key="value"
                                class="w-full"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.expiration')" name="expirationSeconds">
                        <DurationInput v-model="newEntryState.expirationSeconds"/>
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.reason')" name="reason" required>
                        <UTextarea
                                v-model="newEntryState.reason"
                                :placeholder="t('views.adminfaced.system.firewall.reasonPlaceholder')"
                                :rows="3"
                                class="w-full"
                        />
                    </UFormField>
                </div>
            </UForm>
        </template>

        <template #footer>
            <div class="w-full flex justify-end gap-2">
                <UButton variant="ghost" @click="closeAddModal" type="button">
                    {{ t("common.cancel") }}
                </UButton>
                <UButton color="primary" type="submit" :loading="loading">
                    {{ t("common.submit") }}
                </UButton>
            </div>
        </template>
    </UModal>

    <UModal v-model:open="isEditModalOpen" :title="t('views.adminfaced.system.firewall.editEntry')">
        <template #body>
            <UForm :schema="filterEntrySchema" :state="editingEntryState" @submit="updateFilterEntry">
                <div class="space-y-4 p-4">
                    <UFormField :label="t('views.adminfaced.system.firewall.identifier')" name="identifier" required>
                        <UInput
                                v-model="editingEntryState.identifier"
                                :placeholder="t('views.adminfaced.system.firewall.identifierPlaceholder')"
                                disabled
                                class="w-full"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.type')" name="type" required>
                        <USelectMenu
                                v-model="editingEntryState.type"
                                :items="identifierTypeOptions"
                                value-key="value"
                                disabled
                                class="w-full"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.mode')" name="mode" required>
                        <USelectMenu
                                v-model="editingEntryState.mode"
                                :items="filterModeOptions"
                                value-key="value"
                                class="w-full"
                        />
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.expiration')" name="expirationSeconds">
                        <DurationInput v-model="editingEntryState.expirationSeconds"/>
                    </UFormField>

                    <UFormField :label="t('views.adminfaced.system.firewall.reason')" name="reason" required>
                        <UTextarea
                                v-model="editingEntryState.reason"
                                :placeholder="t('views.adminfaced.system.firewall.reasonPlaceholder')"
                                :rows="3"
                                class="w-full"
                        />
                    </UFormField>
                </div>
            </UForm>
        </template>

        <template #footer>
            <div class="w-full flex justify-end gap-2">
                <UButton variant="ghost" @click="closeEditModal" type="button">
                    {{ t("common.cancel") }}
                </UButton>
                <UButton color="primary" type="submit" :loading="loading">
                    {{ t("common.update") }}
                </UButton>
            </div>
        </template>
    </UModal>

    <UModal v-model:open="isDeleteModalOpen" :title="t('views.adminfaced.system.firewall.confirmDelete')">
        <template #body>
            <p>{{ t("views.adminfaced.system.firewall.deleteDescription") }}</p>
            <div v-if="selectedEntry" class="mt-4 p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg">
                <p><strong>{{ t("views.adminfaced.system.firewall.identifier") }}:</strong> {{
                        selectedEntry.identifier
                    }}</p>
                <p><strong>{{ t("views.adminfaced.system.firewall.type") }}:</strong> {{ selectedEntry.type }}</p>
            </div>
        </template>
        <template #footer>
            <div class="w-full flex justify-end gap-2">
                <UButton variant="ghost" @click="closeDeleteModal">
                    {{ t("common.cancel") }}
                </UButton>
                <UButton color="error" @click="deleteFilterEntry" :loading="loading">
                    {{ t("common.delete") }}
                </UButton>
            </div>
        </template>
    </UModal>

    <UModal v-model:open="isClearModalOpen" :title="t('views.adminfaced.system.firewall.confirmClear')">
        <template #body>
            <div class="mt-4 p-4 bg-error-50 dark:bg-error-950 border border-error-200 dark:border-error-800 rounded-lg">
                <p class="text-error-700 dark:text-error-300 font-semibold">
                    {{ t("views.adminfaced.system.firewall.clearDescription", {count: entries.length}) }}
                </p>
            </div>
        </template>
        <template #footer>
            <div class="w-full flex justify-end gap-2">
                <UButton variant="ghost" @click="closeClearModal">
                    {{ t("common.cancel") }}
                </UButton>
                <UButton color="error" @click="clearFilterTable" :loading="loading">
                    {{ t("views.adminfaced.system.firewall.clear") }}
                </UButton>
            </div>
        </template>
    </UModal>
</template>

