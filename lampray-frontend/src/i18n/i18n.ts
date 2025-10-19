import {createI18n} from "vue-i18n";

export const i18n = createI18n({
    locale: 'en',
    availableLocales: ['en', 'zh'],
    messages: {
        en: {
            // ...
        },
        zh: {}
    }
})