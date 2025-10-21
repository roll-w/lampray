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

import {createApp} from "vue";
import {createPinia} from "pinia";
import ui from "@nuxt/ui/vue-plugin";
import App from "./App.vue";
import router from "./router";
import {createAxios} from "@/services/request.ts";
import {useUserStore} from "@/stores/user.ts";
import {RouteName} from "@/router/routeName.ts";
import {i18n} from "@/i18n/i18n.ts";
import {addCollection} from "@iconify/vue"

const lucide = await import("@iconify-json/lucide/icons.json");
addCollection(lucide);

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(i18n);
app.use(ui);

const userStore = useUserStore();

const onLoginExpired = () => {
    const userStore = useUserStore()
    userStore.logout()

    router.push({
        name: RouteName.LOGIN,
        query: {
            source: router.currentRoute.value.fullPath
        }
    }).then((failure) => {
        console.log(failure)
    })
}

const onUserBlocked = () => {
    const userStore = useUserStore()
    userStore.block = true
    router.push({
        name: RouteName.BLOCKED,
        query: {
            source: router.currentRoute.value.fullPath
        }
    }).then((failure) => {
        console.log(failure)
    })
}

const axios = createAxios(userStore, onLoginExpired, onUserBlocked);


interface WindowConfig {
    server: {
        httpProtocol: string
        host: string
        wsProtocol: string
    }
}

declare global {
    interface Window {
        config?: WindowConfig
    }
}

const server = (window.config || {}).server || {
    httpProtocol: "http",
    host: "localhost:5100",
    wsProtocol: "ws",
};

if (server) {
    axios.defaults.baseURL = `${server.httpProtocol}://${server.host}`;
}

app.provide("axios", axios);

app.mount("#app");
