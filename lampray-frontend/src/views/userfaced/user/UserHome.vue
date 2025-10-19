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
import {RouteName} from "@/router/routeName.ts";
import {useUserStore} from "@/stores/user.ts";
import {ref} from "vue";

const userStore = useUserStore();

const showAlert = ref(true);
</script>

<template>
    <Transition name="slide-fade">
        <div v-if="showAlert && userStore.hasAdminRole" class="w-full px-5 md:px-8 lg:px-10 pt-4">
            <UAlert variant="soft"
                    class="w-full"
                    color="info"
                    :close="{color: 'neutral', class: 'rounded-md'}"
                    @update:open="(show: boolean) => showAlert = show">
                <template #title>
                            <span class="font-medium text-lg">当前为管理员账户，现在可以访问
                                <router-link
                                        class="text-amber-500 hover:underline underline-offset-4 decoration-2
                                        decoration-amber-500/50 hover:decoration-amber-500 transition-all
                                        transition-duration-500 ease-in-out"
                                        :to="{name: RouteName.ADMIN_HOME}">管理后台</router-link>
                            </span>
                </template>
            </UAlert>
        </div>
    </Transition>

    <UPageHero title="Placeholder" description="Placeholder"/>
</template>

<style scoped>

</style>