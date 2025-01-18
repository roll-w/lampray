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

<script setup>
import {getCurrentInstance, ref} from "vue";
import {useRouter} from "vue-router";
import {NCard, NEmpty, NGi, NGrid, useDialog, useMessage, useNotification} from "naive-ui";
import {createConfig} from "@/request/axios_config";
import api from "@/request/api";
import {popUserErrorTemplate} from "@/views/utils/error";
import {userPage} from "@/router";

const router = useRouter()
const {proxy} = getCurrentInstance()
const notification = useNotification()
const message = useMessage()
const dialog = useDialog()

const users = ref([])

const keyword = router.currentRoute.value.query.keyword || ''
const inputRef = ref(keyword)

const handleSearch = () => {
    router.push({
        query: {
            keyword: encodeURI(inputRef.value)
        }
    })
}

const requestSearch = () => {
    if (!inputRef.value) {
        return
    }
    const config = createConfig()
    config.params = {
        keyword: inputRef.value
    }

    proxy.$axios.get(api.searchUsers(), config).then(resp => {
        users.value = resp.data
    }).catch(error => {
        popUserErrorTemplate(notification, error, '搜索用户失败')
    })
}

const handleUserClick = (user) => {
    router.push({
        name: userPage,
        params: {
            id: user.userId
        }
    })
}

requestSearch()

</script>

<template>
    <div class="p-5">
        <n-empty v-if="users.length === 0"
                 class="py-5"
                 description="没有搜索到用户"/>

        <div class="">
            <n-grid cols="3" x-gap="10">
                <n-gi v-for="user in users" class="py-2">
                    <router-link #="{ navigate, href }"
                                 :to="{name: userPage, params: {userId: user.userId}}" custom>
                        <n-a :href="href" class="cursor-pointer" @click="navigate">
                            <!--TODO: replace with UserCard component-->
                            <n-card>
                                <div class="flex flex-row">
                                    <div class="flex flex-col">
                                        <div class="text-xl">
                                            {{ user.nickname }}
                                        </div>
                                        <div class="text-lg">
                                            @{{ user.username }}
                                        </div>
                                    </div>
                                    <div class="flex-grow"/>
                                    <div class="flex flex-col justify-end">
                                        <div class="text-gray-500 text-sm">
                                            {{ user.userId }}
                                        </div>
                                    </div>
                                </div>
                            </n-card>
                        </n-a>
                    </router-link>
                </n-gi>
            </n-grid>
        </div>
    </div>
</template>

<style scoped>

</style>