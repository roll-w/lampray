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
    <div class="center">
        <n-grid :cols="2" class="pr-10 pl-10">
            <n-gi class="h-full">
                <n-card class="h-full">
                    <n-h1>
                        <n-text type="primary">Lampray</n-text>
                    </n-h1>
                    <n-card :bordered="false" embedded>
                        <!-- TODO: get from api -->
                        <n-p>生命是束纯净的火焰，我们内心都拥有一颗无形的太阳。</n-p>
                        <n-p class="text-right">—— 托马斯·布朗</n-p>
                    </n-card>
                </n-card>
            </n-gi>
            <n-gi>
                <n-card>
                    <LoginForm v-if="isLoginPage()"/>
                    <RegisterForm v-else/>
                </n-card>
            </n-gi>
        </n-grid>
    </div>
</template>

<script setup>
import RegisterForm from "@/components/user/RegisterForm.vue";
import LoginForm from "@/components/user/LoginForm.vue";
import {useRouter} from "vue-router";
import {useUserStore} from "@/stores/user";
import {index, login} from "@/router";
import {NCard, NGi, NGrid, NH1, NP, NText} from "naive-ui";

const router = useRouter()
const userStore = useUserStore()

const checkLogin = () => {
    if (userStore.isLogin) {
        router.push({
            name: index
        })
    }
}

checkLogin()

const isLoginPage = () => {
    return router.currentRoute.value.name === login
}
</script>

<style scoped>
.center {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 64px);
    justify-content: center;
    position: relative;
}
</style>
