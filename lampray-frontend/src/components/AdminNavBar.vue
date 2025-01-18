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

<template>
    <!--  <n-watermark-->
    <!--      :content="username  + ' ID:' + userId + ' ' + role"-->
    <!--      :font-size="16"-->
    <!--      :height="384"-->
    <!--      :line-height="16"-->
    <!--      :rotate="-15"-->
    <!--      :width="384"-->
    <!--      :x-offset="12"-->
    <!--      :y-offset="60"-->
    <!--      cross-->
    <!--      fullscreen-->
    <!--  />-->
    <!--  TODO: collapse sidebar -->
    <n-layout-sider :collapsed-width="0"
                    :native-scrollbar="false"
                    bordered
                    position="absolute"
                    width="var(--sidebar-width)">
        <div>
            <n-menu :options="menuOptions" :value="chooseOn"/>
        </div>
    </n-layout-sider>
</template>

<script setup>

import {useRouter} from "vue-router";
import {onBeforeMount, ref} from "vue";
import {index} from "@/router";
import {useUserStore} from "@/stores/user";
import {NLayoutSider, NMenu, useMessage} from "naive-ui";
import {convertsToMenuOptions, requestFullMenu} from "@/views/menu";

const message = useMessage()
const userStore = useUserStore()
const username = ref(userStore.user.username)
const userId = ref(userStore.user.id)
const role = ref(userStore.user.role)

const router = useRouter()
const checkAdminRole = () => {
    if (!userStore.isLogin || !role || role.value === "USER") {
        message.error("无权限访问该页面")
        router.push({name: index})
        return false
    }
    return true
}

onBeforeMount(() => {
    checkAdminRole()
})

let routerName = router.currentRoute.value.name
const chooseOn = ref()
const calcChooseOption = () => {
    return routerName
}

chooseOn.value = calcChooseOption()

router.afterEach(() => {
    routerName = router.currentRoute.value.name
    chooseOn.value = calcChooseOption()
})

const menuOptions = convertsToMenuOptions(requestFullMenu())

</script>

<style scoped>

</style>