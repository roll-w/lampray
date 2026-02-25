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

import "./assets/main.css";

import {createApp, watch} from "vue";
import {createPinia} from "pinia";
import ui from "@nuxt/ui/vue-plugin";
import App from "./App.vue";
import router from "./router";
import {createAxios} from "@/services/request.ts";
import {useUserStore} from "@/stores/user.ts";
import {RouteName} from "@/router/routeName.ts";
import {i18n, type LocaleOption, mappingToAvailableLocale} from "@/i18n/i18n.ts";
import {addCollection} from "@iconify/vue";
import {useNavigatorLanguage, useStorage} from "@vueuse/core";
import type {Token, User} from "@/stores/user";

async function bootstrap() {
    try {
        const lucideModule = await import("@iconify-json/lucide/icons.json");
        const lucide = (lucideModule as any).default || lucideModule;
        addCollection(lucide);
    } catch (e) {
        console.error("Failed to load icon collection:", e);
    }

    const {language} = useNavigatorLanguage();

    const localeStored = useStorage<string>("app-locale", mappingToAvailableLocale(language.value).code, undefined, {
        listenToStorageChanges: true
    });

    const app = createApp(App);
    const pinia = createPinia();
    app.use(pinia);
    const userStore = useUserStore();
    userStore.load();
    app.use(router);
    app.use(ui);

    const refreshLocale = async (localeCode: LocaleOption) => {
        const existingMessage = i18n.global.getLocaleMessage(localeCode.code);
        if (existingMessage && Object.keys(existingMessage).length > 0) {
            i18n.global.locale = localeCode.code;
            document.documentElement.lang = localeCode.isoCode || localeCode.code;
            return;
        }

        try {
            const messages = await import(`@/i18n/${localeCode.code}.json`);
            i18n.global.setLocaleMessage(localeCode.code, (messages as any).default || messages);
            i18n.global.locale = localeCode.code;
            document.documentElement.lang = localeCode.isoCode || localeCode.code;
        } catch (e) {
            console.error(`Failed to load locale ${localeCode.code}:`, e);
        }
    };

    const onLoginExpired = () => {
        userStore.logout();
        router.push({
            name: RouteName.LOGIN,
            query: {source: router.currentRoute.value.fullPath}
        });
    };

    const onUserBlocked = () => {
        userStore.setBlock(true);
        router.push({
            name: RouteName.BLOCKED,
            query: {source: router.currentRoute.value.fullPath}
        });
    };


    const handleBroadcastLogin = (
        _user: User,
        _token: Token,
        _remember: boolean,
        _block: boolean
    ) => {
        const currentRoute = router.currentRoute.value;

        if (currentRoute.name === RouteName.LOGIN || currentRoute.name === RouteName.REGISTER) {
            const source = currentRoute.query.source;
            if (source) {
                const url = decodeURIComponent(source.toString());
                router.replace(url);
            } else {
                router.push({name: RouteName.USER_HOME});
            }
        }
    };

    const handleBroadcastLogout = () => {
        const currentRoute = router.currentRoute.value;
        const matched = currentRoute.matched;
        const requiresAuth = matched.some(record => record.meta.requireLogin);

        if (requiresAuth) {
            router.push({
                name: RouteName.LOGIN,
                query: {source: currentRoute.fullPath}
            });
        }
    };

    userStore.initBroadcast({
        onLogin: handleBroadcastLogin,
        onLogout: handleBroadcastLogout,
    });

    const axios = createAxios(userStore, onLoginExpired, onUserBlocked);
    const server = ((window as any).config?.server) || {
        httpProtocol: "http",
        host: "localhost:5100",
        wsProtocol: "ws",
    };

    if (server) {
        axios.defaults.baseURL = `${server.httpProtocol}://${server.host}`;
    }

    app.provide("axios", axios);

    const initialLocale = mappingToAvailableLocale(localeStored.value);
    await refreshLocale(initialLocale);
    app.use(i18n);
    app.mount("#app");

    watch(localeStored, (newVal, oldVal) => {
        if (newVal && newVal !== oldVal) {
            const lc = mappingToAvailableLocale(newVal);
            refreshLocale(lc).catch((e) => console.error(e));
        }
    });
}

bootstrap().catch((e) => {
    console.error("Failed to bootstrap the application:", e);
});

export {};
