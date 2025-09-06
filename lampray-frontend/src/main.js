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

import {createApp} from 'vue'
import {createPinia} from 'pinia'
import App from './App.vue'
import './style.css'
import router, {login, pageBlocked} from "@/router"
import naive from "naive-ui";
import {createAxios} from "@/request/axios_config";
import {useUserStore} from "@/stores/user";

const debug = true

const app = createApp(App)
app.use(createPinia())

app.use(naive)
app.use(router)

const meta = document.createElement('meta')
meta.name = 'naive-ui-style'
document.head.appendChild(meta)

if (!debug) {
    console.log = () => {
    }
}

const onLoginExpired = () => {
    window.$message.error("登录已过期，请重新登录")
    const userStore = useUserStore()
    userStore.logout()

    router.push({
        name: login,
        query: {
            source: router.currentRoute.value.fullPath
        }
    }).then((failure) => {
        console.log(failure)
    })
}

const onUserBlocked = () => {
    window.$message.error("您的账号已被封禁")
    const userStore = useUserStore()
    userStore.block = true
    router.push({
        name: pageBlocked,
        query: {
            source: router.currentRoute.value.fullPath
        }
    }).then((failure) => {
        console.log(failure)
    })
}

const axios = createAxios(onLoginExpired, onUserBlocked)
app.config.globalProperties.$axios = axios

app.mount('#app')
