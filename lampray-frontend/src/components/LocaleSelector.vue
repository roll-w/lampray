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
import {watch} from "vue";
import {useI18n} from "vue-i18n";
import {useNavigatorLanguage, useStorage} from "@vueuse/core";

type LocaleOption = {
    code: string;
    label: string;
    isoCode?: string;
}

const i18n = useI18n();

const availableLocales: LocaleOption[] = [
    {code: "en", label: "English", isoCode: "en"},
    {code: "zh-CN", label: "简体中文", isoCode: "zh-Hans"},
]

const { language } = useNavigatorLanguage()

const mappingToAvailableLocale = (lang: string | undefined): LocaleOption => {
    if (!lang) {
        return availableLocales[0]!;
    }
    // TODO: find related locale
    const found = availableLocales.find(locale => locale.code === lang);
    return found ? found : availableLocales[0]!;
}

const localeStored = useStorage<string>("app-locale", mappingToAvailableLocale(language.value).code, undefined, {
    listenToStorageChanges: true
});

const refreshLocale = async (localeCode: LocaleOption) => {
    const messages = await import(`@/i18n/${localeCode.code}.json`);
    i18n.setLocaleMessage(localeCode.code, messages.default);
    i18n.locale.value = localeCode.code;
    document.documentElement.lang = localeCode.isoCode || localeCode.code;
};

watch(localeStored, async (newLocale) => {
    await refreshLocale(mappingToAvailableLocale(newLocale));
});

// Initial load
refreshLocale(mappingToAvailableLocale(localeStored.value)).catch(err => {
    console.error("Failed to load locale", err);
})
</script>

<template>
    <div>
        <!-- TODO: replace with a icon select-->
        <USelect
                :items="availableLocales.map(locale => ({label: locale.label, value: locale.code}))"
                v-model="localeStored"
                class="w-32"
        />
    </div>
</template>