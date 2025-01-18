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
    <div>
        <n-card :bordered="false" size="large">
            <UserPageHeader v-if="userInfo"
                            :id="userInfo.userId"
                            :avatar="userInfo.avatar"
                            :cover="userInfo.cover"
                            :introduction="userInfo.introduction"
                            :nickname="userInfo.nickname"
                            :username="userInfo.username"
            />
            <div v-else class="text-3xl p-10">
                {{ message }}
            </div>
        </n-card>
        <n-card>
            <div>

            </div>
        </n-card>
    </div>
</template>

<script setup>
import {useRouter} from "vue-router";
import {NCard} from "naive-ui";
import {getCurrentInstance, ref} from "vue";
import api from "@/request/api";
import {createConfig} from "@/request/axios_config";
import UserPageHeader from "@/components/user/UserPageHeader.vue";

const router = useRouter()
const {proxy} = getCurrentInstance()

const id = router.currentRoute.value.params.userId

const userInfo = ref(null)
const message = ref(null)

const getMessage = (status) => {
    if (status >= 500) {
        return "请求信息错误"
    }
    if (status === 404) {
        return "用户不存在"
    }
    return "用户注销或被屏蔽"
}

const getUserInfo = () => {
    const config = createConfig()
    proxy.$axios.get(api.users(false, id), config)
            .then(res => {
                const userData = res.data
                if (userData.introduction === null) {
                    userData.introduction = "这个人很懒，什么都没留下"
                }
                userInfo.value = res.data
            })
            .catch(err => {
                message.value = getMessage(err.status)
            })
}

getUserInfo()

</script>

<style scoped>

</style>