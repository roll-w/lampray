<!--
  - Copyright (C) 2023 RollW
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

<script setup>
import {RouterView} from 'vue-router'
import BlogNavBar from "@/components/BlogNavBar.vue";
import {tokenKey, userDataKey, userKey, useUserStore} from "@/stores/user";
import router from "@/router";
import {ref} from "vue";
import {
    darkTheme,
    enUS,
    lightTheme,
    NConfigProvider,
    NDialogProvider,
    NGlobalStyle,
    NLayout,
    NLoadingBarProvider,
    NMessageProvider,
    NNotificationProvider,
    zhCN
} from "naive-ui";
import {useSiteStore} from "@/stores/site";

import katex from 'katex'
import 'katex/dist/katex.css'

// highlight.js
import hljs from 'highlight.js/lib/core'
import cpp from 'highlight.js/lib/languages/cpp'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import html from 'highlight.js/lib/languages/xml'
import xml from 'highlight.js/lib/languages/xml'
import less from 'highlight.js/lib/languages/less'
import markdown from 'highlight.js/lib/languages/markdown'
import php from 'highlight.js/lib/languages/php'
import python from 'highlight.js/lib/languages/python'
import scss from 'highlight.js/lib/languages/scss'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import yaml from 'highlight.js/lib/languages/yaml'
import bash from 'highlight.js/lib/languages/bash'
import csharp from 'highlight.js/lib/languages/csharp'
import go from 'highlight.js/lib/languages/go'
import ini from 'highlight.js/lib/languages/ini'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import MessageApi from "@/MessageApi.vue";

hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('css', css)
hljs.registerLanguage('java', java)
hljs.registerLanguage('html', html)
hljs.registerLanguage('less', less)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('php', php)
hljs.registerLanguage('python', python)
hljs.registerLanguage('scss', scss)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('go', go)
hljs.registerLanguage('ini', ini)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('json', javascript)
// highlight.js

const userStore = useUserStore()

const prefix = "YWQ5ZWM0MGQ3N2FlNzA3OTE5OGUwZjMwNjNiNGJjNDNiZmM"

const encodeToken = (token) => {
    if (!token) {
        return null
    }

    return prefix + window.btoa(token)
}

const restoreToken = (token) => {
    if (!token) {
        return null
    }

    const removePrefix = token.substring(prefix.length)
    return window.atob(removePrefix)
}

const loadFromLocal = () => {
    const token = restoreToken(localStorage.getItem(tokenKey))
    const user = JSON.parse(localStorage.getItem(userKey))
    const userData = JSON.parse(localStorage.getItem(userDataKey))
    return {user, token, userData}
}

const loadFromSession = () => {
    const token = restoreToken(sessionStorage.getItem(tokenKey))

    const user = JSON.parse(sessionStorage.getItem(userKey))
    const userData = JSON.parse(sessionStorage.getItem(userDataKey))
    return {user, token, userData}
}

const local = loadFromLocal()
const session = loadFromSession()

const tryLoginFromState = () => {
    if (local.user && local.token) {
        userStore.loginUser(local.user, local.token, true)
        if (local.userData) {
            userStore.setUserData(local.userData)
        }
        return
    }

    if (session.user && session.token) {
        userStore.loginUser(session.user, session.token, false)
    }
    if (session.userData) {
        userStore.setUserData(session.userData)
    }
}
tryLoginFromState()

userStore.$subscribe((mutation, state) => {
    const now = new Date()
    console.log(now.toLocaleString() + ' ' + now.getMilliseconds() + ' ' + 'App.vue' + ' ' + 'subscribe', state)

    if (!state.remember) {
        sessionStorage.setItem(tokenKey, encodeToken(state.token))
        sessionStorage.setItem(userKey, JSON.stringify(state.user))
        sessionStorage.setItem(userDataKey, JSON.stringify(state.userData))
        return
    }
    localStorage.setItem(tokenKey, encodeToken(state.token))
    localStorage.setItem(userKey, JSON.stringify(state.user))
    localStorage.setItem(userDataKey, JSON.stringify(state.userData))
})

/**
 * @type import('naive-ui').GlobalThemeOverrides
 */
const themeOverrides = {
    "common": {
        "primaryColor": "#ECA602FF",
        "baseColor": "#F4F4F4FF",
        "primaryColorHover": "#F1BB55FF",
        "primaryColorPressed": "#F6CC6AFF",
        "primaryColorSuppl": "#D0A75FFF",
        "warningColor": "#ED7F3BFF",
        "warningColorHover": "#FC8E40FF",
        "warningColorPressed": "#C96610FF",
        "warningColorSuppl": "#FC8240FF",
        "errorColor": "#D03050FF",
        "errorColorHover": "#DE5782FF",
        "errorColorPressed": "#AB1F39FF",
        "errorColorSuppl": "#E85364FF",
        "fontFamily": `'Muli', '思源黑体', 'Source Han Sans',  -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen-Sans, Ubuntu, Cantarell, 'Helvetica Neue', sans-serif`,
        "fontSize": "16px",
        "fontSizeMini": "14px",
        "fontSizeTiny": "14px",
        "fontSizeSmall": "16px",
        "fontSizeMedium": "16px",
        "fontSizeLarge": "17px",
        "fontSizeHuge": "18px",
    },
}

const dark = ref(lightTheme)
const locale = ref({
    locale: zhCN
})

const extractLocale = (locale) => {
    if (!locale) {
        return zhCN
    }
    switch (locale) {
        case "zh-CN":
        case "zh-TW":
            return zhCN
        case "en-US":
            return enUS

    }
    return zhCN
}

const setupSiteState = (isDark, localeString) => {
    if (isDark) {
        dark.value = darkTheme
    } else {
        dark.value = lightTheme
    }
    locale.value.locale = extractLocale(localeString)
}


const siteStore = useSiteStore()

const loadSiteDataLocal = () => {
    let data = localStorage.getItem("site");
    if (!data) {
        return
    }
    const site = JSON.parse(data)
    siteStore.setLocale(site.locale)
    siteStore.setDark(site.dark)

    setupSiteState(site.dark, site.locale)
}

loadSiteDataLocal()

router.afterEach((to, from) => {
    loadSiteDataLocal()
})

siteStore.$subscribe((mutation, state) => {
    setupSiteState(state.dark, state.locale)
    localStorage.setItem("site", JSON.stringify(state))
})

</script>

<template>
    <n-config-provider :hljs="hljs" :katex="katex" :locale="locale.locale"
                       :theme="dark" :theme-overrides="themeOverrides" class="h-100">
        <n-loading-bar-provider>
            <n-message-provider>
                <message-api/>
                <n-notification-provider :max="5">
                    <n-dialog-provider>
                        <n-layout position="absolute">
                            <BlogNavBar/>
                            <router-view/>
                        </n-layout>
                    </n-dialog-provider>
                </n-notification-provider>
            </n-message-provider>
        </n-loading-bar-provider>
        <n-global-style/>
    </n-config-provider>
</template>

<style scoped>

</style>
