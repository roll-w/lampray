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

<script setup>
import {getCurrentInstance, ref} from "vue";
import {useRouter} from "vue-router";
import {NIcon, NInput, useDialog, useMessage, useNotification} from "naive-ui";
import {searchPage} from "@/router";
import SearchFilled from "@/components/icon/SearchFilled.vue";

const router = useRouter()
const {proxy} = getCurrentInstance()
const notification = useNotification()
const message = useMessage()
const dialog = useDialog()

const props = defineProps({
    size: {
        type: String,
        default: "medium"
    },
    value: {
        type: String,
        default: ""
    }
})

const searchValue = ref(props.value)

const hover = ref(false)
const focus = ref(false)

const handleKeyDown = (event) => {
    if (event.key === "Enter" || event.type === "click") {
        if (!searchValue.value) {
            message.warning("请输入搜索内容")
            return
        }

        router.push({
            name: searchPage,
            query: {
                keyword: searchValue.value,
                source: "search"
            }
        })
        return
    }
    if (event.key === "Escape") {
        searchValue.value = ""
    }
}


</script>

<template>
    <div>
        <n-input v-model:value="searchValue"
                 :input-props="{
                    'class': 'h-full' + (size === 'large' ? ' text-xl' : ''),
                    style: 'height: 100%; width: 100%; padding: 0; vertical-align: middle;',
                 }"
                 :on-blur="() => focus = false"
                 :on-focus="() => focus = true"
                 :on-keyup="handleKeyDown"
                 :size="size === 'large' ? 'large' : 'medium'"
                 clearable
                 placeholder="搜索..."
                 style="height: 100%; width: 100%;"
                 @mouseenter="hover = true"
                 @mouseleave="hover = false">
            <template #suffix>
                <n-icon :size="size === 'large' ? '40' : '33'"
                        class="cursor-pointer rounded-md transition-all duration-300 hover:bg-gray-300/30"
                        depth="1.0"
                        @click="handleKeyDown">
                    <SearchFilled class="p-1 hover:text-amber-500 transition-all"/>
                </n-icon>
            </template>
        </n-input>
    </div>
</template>
