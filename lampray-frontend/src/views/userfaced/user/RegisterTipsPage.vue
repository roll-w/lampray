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
</script>

<template>
    <div class="min-h-[80vh] flex items-center justify-center p-4">
        <div class="max-w-md mx-auto w-full">
            <UCard variant="outline"
                   class="rounded-lg bg-linear-to-br from-default to-90% to-green-50 dark:to-green-950/50">
                <template #header>
                    <div class="flex flex-col items-center gap-2 py-4">
                        <div class="w-14 h-14 rounded-full bg-gradient-to-br flex items-center justify-center">
                            <UIcon name="i-lucide-check-circle" class="w-7 h-7 text-success"/>
                        </div>
                        <h1 class="text-xl font-bold text-gray-900 dark:text-white">注册成功</h1>
                        <p class="text-normal text-gray-600 dark:text-gray-400">
                            请检查你的邮箱，点击激活链接激活你的账户。
                        </p>
                    </div>
                </template>
                <template #footer>
                    <div class="text-center py-2">
                        <p class="text-xs text-gray-500 dark:text-gray-400">
                            如遇问题请
                            <UButton variant="link" size="xs" color="primary" class="p-0 h-auto">
                                联系客服
                            </UButton>
                            获取帮助。
                        </p>
                    </div>
                </template>
            </UCard>
        </div>
    </div>
</template>