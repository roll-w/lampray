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
import {ref} from "vue";
import {useRouter} from "vue-router";
import DashboardPanel from "@/views/adminfaced/DashboardPanel.vue";
import {useI18n} from "vue-i18n";
import {RouteName} from "@/router/routeName.ts";

const router = useRouter()
const {t} = useI18n()

const filterTableCount = ref(0)

const navigateToFilterTable = () => {
    router.push({name: RouteName.ADMIN_FILTER_TABLE})
}
</script>

<template>
    <DashboardPanel>
        <template #header>
            <UDashboardNavbar>
                <template #title>
                    <div class="flex flex-col">
                        <span class="text-lg font-medium">
                            {{ t("views.adminfaced.system.firewall.title") }}
                        </span>
                        <span class="text-sm text-neutral-500 font-normal mt-1">
                            {{ t("views.adminfaced.system.firewall.overview") }}
                        </span>
                    </div>
                </template>
            </UDashboardNavbar>
        </template>
        <template #body>
            <div class="space-y-6">
                <UCard>
                    <template #header>
                        <div class="flex items-center justify-between">
                            <div>
                                <h3 class="text-lg font-semibold">{{ t("views.adminfaced.system.firewall.filterTable") }}</h3>
                                <p class="text-sm text-neutral-500 mt-1">
                                    {{ t("views.adminfaced.system.firewall.filterTableDescription") }}
                                </p>
                            </div>
                            <UButton
                                color="primary"
                                variant="solid"
                                @click="navigateToFilterTable"
                                icon="i-lucide-arrow-right"
                                trailing
                            >
                                {{ t("views.adminfaced.system.firewall.manage") }}
                            </UButton>
                        </div>
                    </template>
                </UCard>
            </div>
        </template>
    </DashboardPanel>
</template>

