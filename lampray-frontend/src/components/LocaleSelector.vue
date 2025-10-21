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
import {ref, watch} from "vue";
import {useI18n} from "vue-i18n";

type LocaleOption = { code: string; label: string }

const i18n = useI18n();

const availableLocales: LocaleOption[] = [
    {code: 'en', label: 'English'},
    {code: 'zh-CN', label: '简体中文'},
]

const locale = ref<string>('en');

watch(locale, async (newLocale) => {
    const messages = await import(`@/i18n/${newLocale}.json`);
    i18n.setLocaleMessage(newLocale, messages.default);
    i18n.locale.value = newLocale;
});

</script>

<template>
    <div>
        <!-- TODO: replace with a icon select-->
        <USelect
                :items="availableLocales.map(locale => ({label: locale.label, value: locale.code}))"
                v-model="locale"
                class="w-32"
        />
    </div>
</template>