/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {createI18n} from "vue-i18n";

export const i18n = createI18n({
    locale: 'en',
    availableLocales: ['en', 'zh-CN'],
    messages: {
    }
})

export type LocaleOption = {
    code: string;
    label: string;
    isoCode?: string;
}

export const availableLocales: LocaleOption[] = [
    {code: "en", label: "English", isoCode: "en-US"},
    {code: "zh-CN", label: "简体中文", isoCode: "zh-Hans"},
]

export const mappingToAvailableLocale = (lang: string | undefined): LocaleOption => {
    if (!lang) {
        return availableLocales[0]!;
    }
    // TODO: find related locale
    const found = availableLocales.find(locale => locale.code === lang);
    return found ? found : availableLocales[0]!;
}
