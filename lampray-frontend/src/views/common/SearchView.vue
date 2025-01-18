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
import {NTabPane, NTabs, useDialog, useMessage, useNotification} from "naive-ui";
import SearchBar from "@/components/SearchBar.vue";
import UserSearchView from "@/views/user/UserSearchView.vue";

const router = useRouter()
const {proxy} = getCurrentInstance()
const notification = useNotification()
const message = useMessage()
const dialog = useDialog()

const currentRoute = router.currentRoute.value

const searchKeyword = ref(router.currentRoute.value.query.keyword || "")


</script>

<template>
    <div class="p-5">
        <div class="flex justify-center items-center">
            <div class="px-12 pt-3 h-20 w-[40%]">
                <SearchBar :value="searchKeyword"
                           class="h-full text-xl" size="large"/>
            </div>
        </div>
        <div class="p-3">
            <n-tabs animated default-value="综合"
                    label-size="large" size="large">
                <n-tab-pane name="综合">
                    <div>

                    </div>
                </n-tab-pane>
                <n-tab-pane name="文章">

                </n-tab-pane>
                <n-tab-pane name="用户">
                    <UserSearchView/>
                </n-tab-pane>
            </n-tabs>
        </div>
    </div>
</template>

<style scoped>

</style>