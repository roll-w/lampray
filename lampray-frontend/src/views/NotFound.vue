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
import {useRouter} from "vue-router";

const router = useRouter();

</script>

<template>
    <div class="h-[75vh] flex flex-col justify-center items-center">
        <div class="p-5">
            <UError :error="{
                statusCode: 404,
                statusMessage: '页面不存在',
                message: '您访问的页面不存在，可能是因为地址错误或页面已被删除。',}">
                <template #links>
                    <UButton color="primary" @click="router.push('/')">回到首页</UButton>
                </template>
            </UError>
        </div>
    </div>
</template>

<style scoped>

</style>