<!--
  - Copyright (C) 2023-2026 RollW
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
import {RouterView} from "vue-router";
import {onBeforeUnmount} from "vue";
import LamprayLogo from "@/components/LamprayLogo.vue";
import * as locales from '@nuxt/ui/locale'
import type {Toast} from "@nuxt/ui/runtime/composables/useToast.d.ts";
import {useI18n} from "vue-i18n";
import type {ToasterProps} from "@nuxt/ui";
import {clearToastHandler, registerToastHandler} from "@/utils/toastBus.ts";

const toaster: ToasterProps = {position: 'top-right', progress: false};
const {locale} = useI18n()
const toast = useToast()
const toastHandler = (payload: Partial<Toast>) => toast.add(payload)

const uLocale = locales[locale.value.replace("-", "_").toLocaleLowerCase() as keyof typeof locales] || locales['en']

registerToastHandler(toastHandler)

onBeforeUnmount(() => {
    clearToastHandler(toastHandler)
})

</script>

<template>
    <UApp :toaster="toaster" :locale="uLocale">
            <UHeader :toggle="false" :ui="{container: 'max-w-full'}">
                <template #title>
                    <div class="flex items-center">
                        <UIcon class="size-8 fill-primary-500" :name="LamprayLogo"/>
                        <span class="ms-1 text-xl font-medium text-primary-500">Lampray</span>
                    </div>
                </template>

                <template #right>
                    <UColorModeButton size="xl"/>
                    <LocaleSelector/>
                    <LoginOrUser/>
                </template>
            </UHeader>
        <UMain>
            <RouterView/>
        </UMain>
    </UApp>
</template>
