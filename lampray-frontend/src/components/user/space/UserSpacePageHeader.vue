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
    <div class="rounded-2xl overflow-hidden bg-no-repeat bg-cover bg-center bg">
        <div class="px-6 sm:px-8 pt-12 pb-8 bg-gradient-to-b from-transparent to-slate-900/35 backdrop-blur-lg">
            <div class="w-full flex items-center gap-5">
                <UAvatar
                        :text="getInitials()"
                        :alt="userInfo.nickname"
                        size="2xl"
                        class="shrink-0"
                />

                <div class="min-w-0">
                    <p class="text-white text-2xl sm:text-3xl font-semibold tracking-tight truncate">
                        {{ userInfo.nickname }}
                    </p>
                    <p class="text-blue-100/90 text-base sm:text-lg truncate">
                        @{{ userInfo.username }}
                    </p>
                </div>

                <div class="ml-auto hidden sm:flex items-end gap-3 text-white/90">
                    <span class="px-2 py-0.5 rounded-md bg-white/20 text-xs uppercase tracking-[0.12em]">UID</span>
                    <span class="text-sm font-medium">{{ userInfo.userId }}</span>
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
    const name = (props.userInfo.nickname || props.userInfo.username || "").trim()
    if (name.length === 0) {
        return "?"
    }
    return name[0]!.toUpperCase()
}
</script>

<style scoped>
/*TODO: the background is temporary, replace with image*/
.bg {
    background: radial-gradient(
            110vw 220vh ellipse at 0% 8%,
            #dce8f6,
            #93c6fa 36%,
            #5f8fbd 62%,
            #27405a 96%
    );
}
</style>
