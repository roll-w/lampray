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
    <n-layout
            :native-scrollbar="false" has-sider
            position="absolute"
            style="top: var(--header-height);">
        <!--sidebar-->
        <AdminNavBar/>
        <n-layout
                :native-scrollbar="false"
                content-style="min-height: calc(100vh - var(--header-height)); display: flex; flex-direction: column;"
                position="absolute"
                style="left: var(--sidebar-width);">
            <router-view v-slot="{ Component }" v-model:name="name">
                <transition mode="out-in" name="fade">
                    <component :is="Component" :key="$route.fullPath"/>
                </transition>
            </router-view>
            <n-back-top :right="100"/>
            <Footer/>
        </n-layout>
    </n-layout>
</template>

<script setup>
import AdminNavBar from "@/components/AdminNavBar.vue";
import Footer from "@/components/Footer.vue";
import {ref, watch} from "vue";
import {NBackTop, NLayout} from "naive-ui";

const name = ref()

watch(name, (newName) => {
    console.log(newName)
})

</script>

<style scoped>
</style>
