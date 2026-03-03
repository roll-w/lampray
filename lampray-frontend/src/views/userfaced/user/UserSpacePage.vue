<!--
  - Copyright (C) 2023-2026 RollW
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

<script lang="ts" setup>
import {useAxios} from "@/composables/useAxios.ts";
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";
import {computed, ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {useRoute} from "vue-router";
import {userService} from "@/services/user/user.service.ts";
import UserSpaceArticleFeed from "@/components/user/space/UserSpaceArticleFeed.vue";
import UserSpacePageHeader from "@/components/user/space/UserSpacePageHeader.vue";

const ARTICLES_SECTION_ID = "user-space-articles"

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
const articleSectionTitle = computed(() => t("views.userfaced.space.sections.articles.title"))

const userDisplayName = computed(() => {
    if (!userInfo.value) {
        return ""
    }

    const nickname = userInfo.value.nickname?.trim() ?? ""
    if (nickname.length > 0) {
        return nickname
    }

    return userInfo.value.username
})

const userHandle = computed(() => {
    if (!userInfo.value) {
        return ""
    }
    return `@${userInfo.value.username}`
})

const userIntroduction = computed(() => {
    const introduction = userInfo.value?.introduction?.trim() ?? ""
    if (introduction.length > 0) {
        return introduction
    }

    return t("views.userfaced.space.profile.introductionPlaceholder")
})

const userLocation = computed(() => {
    const location = userInfo.value?.location?.trim() ?? ""
    if (location.length > 0) {
        return location
    }

    return t("views.userfaced.space.profile.unknownLocation")
})

const userWebsiteUrl = computed(() => {
    const website = userInfo.value?.website?.trim() ?? ""
    if (website.length === 0) {
        return ""
    }

    const protocolSeparator = "://"
    const protocolIndex = website.indexOf(protocolSeparator)
    if (protocolIndex > 0) {
        const pathIndex = protocolIndex + protocolSeparator.length
        const secureProtocol = "https"
        const protocol = website.slice(0, protocolIndex).toLowerCase()
        if (protocol === secureProtocol) {
            return website
        }

        return `${secureProtocol}${protocolSeparator}${website.slice(pathIndex)}`
    }

    return `https://${website}`
})

const tabs = computed<SpaceTabItem[]>(() => {
    return [
        {
            label: articleSectionTitle.value,
            value: "articles",
            icon: "i-lucide-file-text",
        },
    ]
})

function scrollToArticles(): void {
    if (typeof window === "undefined") {
        return
    }

    const target = document.getElementById(ARTICLES_SECTION_ID)
    target?.scrollIntoView({
        behavior: "smooth",
        block: "start",
    })
}

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
    userInfo.value = null
    message.value = null

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
    <div class="w-full mx-auto max-w-screen-2xl px-2 sm:px-4 lg:px-8 py-6 space-y-6">
        <UserSpacePageHeader
                v-if="userInfo"
                :user-info="userInfo"
        />

        <div v-else-if="loadingUserInfo"
             class="min-h-56 rounded-2xl bg-neutral-100/70 dark:bg-neutral-900/40 px-6 py-6 space-y-3">
            <USkeleton class="h-8 w-40"/>
            <USkeleton class="h-5 w-64"/>
            <USkeleton class="h-5 w-44"/>
        </div>

        <div v-else class="min-h-56 flex items-center rounded-2xl bg-neutral-100/70 dark:bg-neutral-900/40 p-4 sm:p-5">
            <UAlert
                    :description="message || t('views.userfaced.space.state.requestFailed')"
                    :title="t('request.error.title')"
                    class="w-full"
                    color="neutral"
                    icon="i-lucide-user-x"
                    variant="subtle"
            />
        </div>

        <div v-if="userInfo && userId > 0" class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_18.5rem] items-start">
            <div class="space-y-4">
                <section
                        class="rounded-2xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 p-3 sm:p-4 space-y-4">
                    <div class="flex flex-wrap items-center justify-between gap-3">
                        <UTabs
                                v-model="activeTab"
                                :content="false"
                                :items="tabs"
                                class="w-full md:w-auto"
                                color="primary"
                                variant="link"
                        />

                        <UButton color="primary" icon="i-lucide-arrow-down" size="sm" variant="soft"
                                 @click="scrollToArticles">
                            {{ t("views.userfaced.space.profile.latest") }}
                        </UButton>
                    </div>

                    <div :id="ARTICLES_SECTION_ID">
                        <UserSpaceArticleFeed :user-id="userId"/>
                    </div>
                </section>
            </div>

            <aside class="xl:sticky xl:top-[calc(var(--ui-header-height)+1rem)] space-y-4">
                <section
                        class="rounded-2xl bg-white dark:bg-neutral-950/70 ring-1 ring-neutral-200/70 dark:ring-neutral-800/70 p-4 space-y-3">
                    <div class="space-y-1">
                        <p class="text-base font-semibold text-neutral-900 dark:text-neutral-100">{{ userDisplayName }}</p>
                        <p class="text-sm text-neutral-500 dark:text-neutral-400">{{ userHandle }}</p>
                    </div>

                    <p class="text-sm leading-6 text-neutral-700 dark:text-neutral-300">
                        {{ userIntroduction }}
                    </p>


                    <div class="space-y-1">
                        <p class="text-xs text-neutral-500 dark:text-neutral-400">
                            {{ t("views.userfaced.space.profile.locationLabel") }}</p>
                        <p class="text-sm text-neutral-800 dark:text-neutral-100">{{ userLocation }}</p>
                    </div>

                    <div v-if="userWebsiteUrl.length > 0" class="space-y-1">
                        <p class="text-xs text-neutral-500 dark:text-neutral-400">
                            {{ t("views.userfaced.space.profile.websiteLabel") }}</p>
                        <a
                                :href="userWebsiteUrl"
                                class="text-sm text-primary-600 dark:text-primary-400 hover:underline break-all"
                                rel="noopener noreferrer"
                                target="_blank"
                        >
                            {{ userWebsiteUrl }}
                        </a>
                    </div>
                </section>
            </aside>
        </div>
    </div>
</template>

<style scoped>

</style>
