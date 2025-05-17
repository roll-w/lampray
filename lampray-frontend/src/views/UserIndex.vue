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

<template>
    <div class="p-5">
        <n-alert v-if="userStore.canAccessAdmin" :bordered="false" closable type="info">
            <n-text class="text-xl">
                当前作为{{ userStore.user.role }}登录，现在可以访问
                <router-link #="{ navigate, href }" :to="{name: admin}" custom>
                    <n-a :href="href" @click="navigate">后台管理系统</n-a>
                </router-link>
                。
            </n-text>
        </n-alert>
        <n-h1>Lampray</n-h1>
        <n-card>
            <n-h2>推荐文章</n-h2>
            <n-carousel
                    class="rounded-xl"
                    direction="vertical"
                    dot-placement="right"
                    show-arrow
                    style="width: 100%; height: 240px"
            >
                <router-link
                        :to="{name: articleView, params: {
                            user: 1,
                            id: 1
                        }}"
                        class="relative carousel-img cursor-pointer"
                        target="_blank">
                    <img alt="cover"
                         class="carousel-img select-none"
                         src="http://localhost:5100/static/images/cover4.png">
                    <div class="w-100 absolute bottom-0 left-0 bg-gradient-to-b
                                from-transparent to-neutral-800">
                        <div class="select-none mb-5 ml-5 text-5xl
                                    text-white opacity-50 leading-normal">
                            <span>Test</span>
                            <p class="text-2xl">@user</p>
                        </div>
                    </div>
                </router-link>
            </n-carousel>
        </n-card>
    </div>
</template>

<script setup>
import {useUserStore} from "@/stores/user";
import {admin} from "@/router/index.js";
import {NA, NAlert, NCard, NCarousel, NH1, NH2, NText} from "naive-ui";

const userStore = useUserStore()


</script>

<style scoped>
.carousel-img {
    width: 100%;
    height: 240px;
    object-fit: cover;
}
</style>