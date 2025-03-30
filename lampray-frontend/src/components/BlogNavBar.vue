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

<template>
    <n-layout-header bordered style="height: var(--header-height)">
        <div class="px-1 pt-2 flex content-center">
            <div class="ml-5 flex rounded-2xl justify-start transition-all ease-in">
                <n-text :depth="1" class="ui-logo flex justify-start" @click="handleLogoClick">
                    <img alt="Logo" src="../assets/lamp.svg">
                    <span>Lampray</span>
                </n-text>
            </div>
            <div class="flex flex-grow justify-center items-center">
                <Transition name="fade">
                    <SearchBar v-if="$route.name !== searchPage"
                               class="flex justify-center w-[70%] h-[80%]"/>
                </Transition>
            </div>

            <div class="p-3 flex items-center justify-end justify-items-end ">
                <n-space class="flex items-center">
                    <n-button secondary @click="handleThemeClick">切换主题</n-button>
                    <div class="h-9 self-center">
                        <n-button v-if="!userStore.isLogin" @click="handleLoginClick">登录</n-button>
                        <n-dropdown v-else :options="options" trigger="hover" @select="handleSelect">
                            <n-avatar v-if="userStore.userData.setup"
                                      :src="userStore.userData.avatar"
                                      class="border"/>
                            <n-avatar v-else>
                                {{ username }}
                            </n-avatar>
                        </n-dropdown>
                    </div>
                </n-space>
            </div>
        </div>
    </n-layout-header>
</template>


<script setup>
import {RouterLink, useRouter} from "vue-router";
import {useUserStore} from "@/stores/user";
import {getCurrentInstance, h, onMounted, ref} from "vue";
import {
    NAvatar,
    NButton,
    NDropdown,
    NLayoutHeader,
    NSpace,
    NText,
    useDialog,
    useMessage,
    useNotification
} from "naive-ui";
import {useSiteStore} from "@/stores/site";
import {admin, articleEditorPage, index, login, searchPage, userPage, userSettingPage} from "@/router";
import api from "@/request/api";
import {createConfig} from "@/request/axios_config";
import SearchBar from "@/components/SearchBar.vue";
import {popUserErrorTemplate} from "@/views/utils/error.js";

const router = useRouter()
const {proxy} = getCurrentInstance()
const notification = useNotification()
const message = useMessage()
const dialog = useDialog()

const siteStore = useSiteStore()
const userStore = useUserStore()

const username = ref('')
const role = ref(userStore.user.role)

const requestPersonalData = () => {
    if (!userStore.isLogin) {
        return
    }
    const config = createConfig()
    proxy.$axios.get(api.currentUser, config).then((response) => {
        const userData = {
            avatar: response.data.avatar,
            nickname: response.data.nickname,
            setup: true
        }
        userStore.setUserData(userData)
    }).catch((error) => {
        popUserErrorTemplate(notification, error, "获取用户信息失败")
    })
}

const loadUsername = (newUsername, newRole) => {
    username.value = newUsername
    role.value = newRole
}

onMounted(() => {
    if (!userStore.userData.setup) {
        requestPersonalData()
    }
})

loadUsername(userStore.userData.nickname || userStore.user.username, userStore.user.role)

const userOptions = [
    {
        label: `${userStore.user.username}`,
        key: "username",
    },
    {
        label: () => h(
                RouterLink,
                {
                    to: {
                        name: userPage,
                        params: {
                            userId: userStore.user.id
                        }
                    }
                },
                {default: () => "个人主页"}
        ),
        key: "space",
    },
    {
        label: () => h(
                RouterLink,
                {
                    to: {
                        name: articleEditorPage,
                    }
                },
                {default: () => "发布文章"}
        ),
        // TODO: temporary option
        key: "publish",
    },
    {
        label: "文章管理",
        key: "article",
    },
    {
        key: 'header-divider',
        type: 'divider'
    },
    {
        label: "个人设置",
        key: "settings",
    },
    {
        label: "退出",
        key: "logout",
    }
]

const adminOptions = [
    {
        label: `${userStore.user.username}`,
        key: "username",
    },
    {
        label: () => h(
                RouterLink,
                {
                    to: {
                        name: userPage,
                        params: {
                            userId: userStore.user.id
                        }
                    }
                },
                {default: () => "个人主页"}
        ),
        key: "space",
    },
    {
        label: () => h(
                RouterLink,
                {
                    to: {
                        name: articleEditorPage,
                    }
                },
                {default: () => "发布文章"}
        ),
        // TODO: temporary option
        key: "publish",
    },
    {
        label: "文章管理",
        key: "article",
    },
    {
        label: () => h(
                RouterLink,
                {
                    to: {
                        name: admin
                    }
                },
                {default: () => "系统管理"}
        ),
        key: "system",
    },
    {
        key: 'header-divider',
        type: 'divider'
    },
    {
        label: "个人设置",
        key: "settings",
    },
    {
        label: "退出",
        key: "logout",
    }
]
const options = ref(userOptions)

const updatesOption = (options, username, id) => {
    options[0].label = username
    options[1].label = () => h(
            RouterLink,
            {
                to: {
                    name: userPage,
                    params: {
                        userId: id
                    }
                }
            },
            {default: () => "个人主页"}
    )
    return options
}

const chooseOptions = (username, role, id) => {
    if (!role) {
        options.value = null
        return
    }
    if (role !== "USER") {
        options.value = updatesOption(adminOptions, username, id)
    } else {
        options.value = updatesOption(userOptions, username, id)
    }
}

chooseOptions(userStore.user.username, userStore.user.role, userStore.user.id)

userStore.$subscribe((mutation, state) => {
    if (!state.user) {
        loadUsername(null, 'USER')
        chooseOptions(null, null, 0)
        return
    }
    loadUsername(state.userData.nickname || state.user.username, state.user.role)
    chooseOptions(state.user.username, state.user.role, state.user.id)
})


const handleLogoClick = () => {
    router.push({
        name: index
    });
};

const handleLoginClick = () => {
    router.push({
        name: login
    });
};

const handleSelect = (key) => {
    switch (key) {
        case "logout":
            userStore.logout();
            router.push({
                name: index
            })
            break;
    }
}

const handleThemeClick = () => {
    siteStore.toggleTheme()
}

</script>

<style scoped>
.ui-logo {
    cursor: pointer;
    display: flex;
    align-items: center;
    font-size: 18px;
}

.ui-logo > img {
    margin-right: 12px;
    height: 32px;
    width: 32px;
}
</style>
