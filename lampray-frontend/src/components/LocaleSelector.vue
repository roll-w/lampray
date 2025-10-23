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
import {useNavigatorLanguage, useStorage} from "@vueuse/core";
import {availableLocales, mappingToAvailableLocale} from "@/i18n/i18n.ts";

const {language} = useNavigatorLanguage()

const localeStored = useStorage<string>("app-locale", mappingToAvailableLocale(language.value).code, undefined, {
    listenToStorageChanges: true
});

</script>

<template>
    <div>
        <!-- TODO: replace with a icon select-->
        <USelect
                :items="availableLocales.map(locale => ({label: locale.label, value: locale.code}))"
                v-model="localeStored"
                size="lg"
                class="w-28"
        />
    </div>
</template>