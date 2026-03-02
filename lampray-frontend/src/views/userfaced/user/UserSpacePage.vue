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
import {useAxios} from "@/composables/useAxios.ts";
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";
import {computed, ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {useRoute} from "vue-router";
import {userService} from "@/services/user/user.service.ts";
import UserSpaceArticleFeed from "@/components/user/space/UserSpaceArticleFeed.vue";

const axios = useAxios()
const route = useRoute()
const {t} = useI18n()
const loadingUserInfo = ref(false)
const userInfo = ref<UserCommonDetailsVo | null>(null)
const message = ref<string | null>(null)

type SpaceTabValue = "articles"

interface SpaceTabItem {
    label: string
    value: SpaceTabValue
    icon: string
}

const userId = computed<number>(() => {
    const value = Number(route.params.id)
    if (!Number.isFinite(value) || value <= 0) {
        return 0
    }
    return value
})

const activeTab = ref<SpaceTabValue>("articles")

const tabs = computed<SpaceTabItem[]>(() => {
    return [
        {
            label: t("views.userfaced.space.sections.articles.title"),
            value: "articles",
            icon: "i-lucide-file-text",
        },
    ]
})

const resolveUserInfoMessage = (status: number): string => {
    if (status >= 500) {
        return t("views.userfaced.space.state.requestFailed")
    }

    if (status === 404) {
        return t("views.userfaced.space.state.userNotFound")
    }

    return t("views.userfaced.space.state.userUnavailable")
}

const loadUserInfo = async () => {
    loadingUserInfo.value = true

    if (userId.value <= 0) {
        userInfo.value = null
        message.value = t("views.userfaced.space.state.userNotFound")
        loadingUserInfo.value = false
        return
    }

    try {
        const response = await userService(axios).getUserInfo(userId.value)
        const body = response.data
        userInfo.value = body.data!
        message.value = null
        return
    } catch (error: any) {
        userInfo.value = null
        if (error.response) {
            message.value = resolveUserInfoMessage(error.response.status)
        } else {
            message.value = t("views.userfaced.space.state.requestFailed")
        }
    } finally {
        loadingUserInfo.value = false
    }
}

watch(() => route.params.id, () => {
    void loadUserInfo()
}, {immediate: true})

</script>

<template>
    <div class="px-2 sm:px-4 lg:px-5 py-6 space-y-6">
        <UserSpacePageHeader
                v-if="userInfo"
                :user-info="userInfo"
        />
        <div v-else-if="loadingUserInfo" class="min-h-56 rounded-xl bg-neutral-100/70 dark:bg-neutral-900/40 px-6 py-6 space-y-3">
            <USkeleton class="h-8 w-40"/>
            <USkeleton class="h-5 w-64"/>
            <USkeleton class="h-5 w-44"/>
        </div>
        <div v-else class="min-h-56 flex items-center rounded-xl bg-neutral-100/70 dark:bg-neutral-900/40 p-4 sm:p-5">
            <UAlert
                    :title="t('request.error.title')"
                    :description="message || t('views.userfaced.space.state.requestFailed')"
                    color="neutral"
                    icon="i-lucide-user-x"
                    variant="subtle"
                    class="w-full"
            />
        </div>

        <div v-if="userInfo && userId > 0" class="space-y-4">
            <section class="rounded-xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 p-3 sm:p-4 space-y-3">
                <UTabs
                        v-model="activeTab"
                        :items="tabs"
                        :content="false"
                        color="primary"
                        variant="link"
                        class="w-full"
                />

                <div class="space-y-1 px-1">
                    <p class="text-sm font-medium text-neutral-800 dark:text-neutral-100">{{ t("views.userfaced.space.sections.articles.title") }}</p>
                    <p class="text-xs text-neutral-500 dark:text-neutral-400">{{ t("views.userfaced.space.sections.articles.description") }}</p>
                </div>
            </section>

            <UserSpaceArticleFeed :user-id="userId"/>
        </div>
    </div>
</template>

<style scoped>

</style>
