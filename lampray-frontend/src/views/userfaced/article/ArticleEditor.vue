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
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {ref} from "vue";
import type {StructuralText} from "@/components/structuraltext/types.ts";

const content = ref<StructuralText | undefined>()
const handleChange = (value: StructuralText) => {
    content.value = value
}
</script>

<template>
    <div class="mx-auto">
        <StructuralTextEditor
                v-model="content"
                :editable="true"
                placeholder="Start writing your content here..."
                :show-toolbar="true"
                :show-outline="true"
                @change="handleChange"
                :ui="{content: {
                    root: 'w-full md:w-[85vw] lg:w-[50vw] mx-auto overflow-y-auto'
                }, toolbar: {
                    root: 'border-b border-gray-200 dark:border-gray-700 overflow-hidden top-[var(--ui-header-height)]',
                    centered: true
                }, outline:{
                    root: 'top-[calc(var(--ui-header-height)+var(--toolbar-height))]',
                }}"
        >
            <template #before-content>
                <div class="p-4 w-full md:w-[85vw] lg:w-[50vw] mx-auto">
                    <input type="text"
                           placeholder="Type your article title here..."
                           class="w-full h-12 text-3xl font-bold outline-none border-none
                           focus:ring-0 focus:outline-none bg-transparent"/>
                </div>
            </template>

            <template #outline-bottom>
                <div>
                    <UCard>
                        <template #header>
                            <h3 class="text-lg font-medium">文章大纲</h3>
                        </template>
                        <template #body>
                            <div class="text-sm text-gray-600 dark:text-gray-400">
                                这里显示文章的大纲信息，帮助你快速导航和了解文章结构。
                            </div>
                        </template>

                    </UCard>

                </div>
            </template>
        </StructuralTextEditor>
    </div>
</template>

<style scoped>

</style>