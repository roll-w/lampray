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
import {computed, ref} from 'vue'
import StructuralTextEditor from './StructuralTextEditor.vue'
import type {StructuralText} from './types'

const content = ref<StructuralText | undefined>()
const showJson = ref(false)

const handleChange = (value: StructuralText) => {
    content.value = value
}

const formattedJson = computed(() => {
    if (!content.value) return ''
    return JSON.stringify(content.value, null, 2)
})
</script>

<template>
    <div class="container mx-auto p-6 space-y-6">
        <div class="space-y-2">
            <h1 class="text-3xl font-bold">Text Editor</h1>
        </div>

        <div class="grid grid-cols-1 xl:grid-cols-[1fr_300px] gap-6">
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div class="space-y-4">
                    <div class="flex items-center justify-between">
                        <h2 class="text-xl font-semibold">Editor</h2>
                    </div>
                    <StructuralTextEditor
                            v-model="content"
                            :editable="true"
                            placeholder="Start writing your content here..."
                            :show-toolbar="true"
                            @change="handleChange"
                    />
                </div>

                <div class="space-y-4">
                    <div class="flex items-center justify-between">
                        <h2 class="text-xl font-semibold">Preview</h2>
                        <UButton
                                :variant="showJson ? 'solid' : 'ghost'"
                                color="neutral"
                                size="sm"
                                @click="showJson = !showJson"
                        >
                            {{ showJson ? 'Hide JSON' : 'Show JSON' }}
                        </UButton>
                    </div>

                    <div v-if="showJson" class="bg-gray-100 dark:bg-gray-900 rounded-lg p-4 overflow-auto max-h-[600px]">
                        <pre class="text-sm"><code>{{ formattedJson }}</code></pre>
                    </div>

                    <div v-else class="border border-gray-200 dark:border-gray-700 rounded-lg p-4 min-h-[400px]">
                        <StructuralTextEditor
                                v-model="content"
                                :editable="false"
                                :show-toolbar="false"
                        />
                    </div>
                </div>
            </div>

            <aside class="hidden xl:block">
                <div class="sticky top-6">
                    <StructuralTextOutline
                        v-if="content"
                        :document="content"
                        title="Document Outline"
                        color="primary"
                        size="sm"
                    />
                </div>
            </aside>
        </div>
    </div>
</template>
