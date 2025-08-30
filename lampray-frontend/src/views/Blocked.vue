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
    <div class="h-[75vh] flex flex-col justify-center items-center">
        <div class="p-5 ">
            <n-result status="403" title="您的访问被阻止了">
                <template #default>
                    <div class="text-center text-xl">
                        <div>{{ result }}</div>
                        <div>{{ tip }}</div>
                        <div>{{ message }}</div>
                    </div>
                </template>
                <template #footer>
                    <n-button secondary type="primary" @click="">对您的封禁有疑问？</n-button>
                </template>
            </n-result>
        </div>
    </div>
</template>

<script setup>
import {useRouter} from "vue-router";
import {NButton, NResult} from "naive-ui";

const router = useRouter()
const result = router.currentRoute.value.query.result
const tip = router.currentRoute.value.query.tip
const message = router.currentRoute.value.query.message
const source = router.currentRoute.value.query.source
</script>

<style scoped>

</style>