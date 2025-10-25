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
    <div class="rounded-2xl bg-no-repeat bg-cover bg-center bg">
        <div class="rounded-2xl pt-10 backdrop-blur-3xl">
            <div class="rounded-b-2xl p-7 pb-7 mt-14 w-full bg-opacity-20 bg-gradient-to-b from-transparent to-gray-500/50">
                <div class="flex items-center mx-auto w-full lg:w-4/5">
                    <UAvatar :text="getInitials()"
                             :alt="userInfo.nickname"
                             size="2xl"
                             class="mr-5">
                    </UAvatar>
                    <div class="leading-tight">
                        <div class="text-white text-2xl font-bold">
                            {{ userInfo.nickname }}
                        </div>
                        <div class="text-gray-200 text-lg font-light">
                            @{{ userInfo.username }}
                        </div>
                    </div>
                    <div class="flex-grow"></div>
                    <div class="flex items-end pr-5">
                        <div class="py-0.5 px-2 text-gray-100 bg-neutral-300/50 rounded text-sm font-medium">
                            UID
                        </div>
                        <div class="ml-3 text-gray-100">
                            {{ userInfo.userId }}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";

const props = defineProps<{
    userInfo: Partial<UserCommonDetailsVo>
}>()

const getInitials = (): string => {
    const name = props.userInfo.nickname || props.userInfo.username
    if (name!.length === 0) return '?'
    return name!
}
</script>

<style scoped>
/*TODO: the background is temporary, replace with image*/
.bg {
    background: radial-gradient(
            110vw 250vh ellipse at 0% 11%,
            #b4cfe8,
            #5faffd 28%,
            #ff5858 35%,
            #e754db 40%,
            #fb206e 50%,
            #f09819 65%
    );
}
</style>