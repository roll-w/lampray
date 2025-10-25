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
const year = new Date().getFullYear()
</script>

<template>
    <UFooter :ui="{container: 'max-w-full'}"
             class="border-t border-gray-200 dark:border-gray-700 flex justify-between items-center text-gray-500 dark:text-gray-400">
        <template #left>
            <div class="p-10 text-sm">
                ©2023-{{ year }} Lamprism. All rights reserved. 版权所有
            </div>
        </template>

        <template #right>
            <a href="https://www.apache.org/licenses/LICENSE-2.0" target="_blank" class="hover:underline underline-offset-4 decoration-2
                      decoration-amber-500/50 hover:decoration-amber-500 transition-all
                      transition-duration-500 ease-in-out">
                Apache License 2.0
            </a>
        </template>
    </UFooter>
</template>

<style scoped>

</style>