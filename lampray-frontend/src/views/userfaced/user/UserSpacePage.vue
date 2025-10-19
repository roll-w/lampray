<script setup lang="ts">
import {useAxios} from "@/composables/useAxios.ts";
import type {UserCommonDetailsVo} from "@/services/user/user.type.ts";
import {onMounted, ref, watch} from "vue";
import {useRoute} from "vue-router";
import {userService} from "@/services/user/user.service.ts";

const axios = useAxios()
const route = useRoute()
const userInfo = ref<UserCommonDetailsVo | null>(null)
const message = ref<string | null>(null)

const userId = route.params.id as string

watch(() => route.params.id, (newId) => {
    if (newId && newId !== userId) {
        getUserInfo()
    }
})


const getMessage = (status: number): string => {
    if (status >= 500) return "请求信息错误"
    if (status === 404) return "用户不存在"
    return "用户注销或被屏蔽"
}

const getUserInfo = async () => {
    try {
        const response = await userService(axios).getUserInfo(userId)
        const body = response.data
        userInfo.value = body.data!
        message.value = null
        return
    } catch (error: any) {
        userInfo.value = null
        if (error.response) {
            message.value = getMessage(error.response.status)
        } else {
            message.value = "请求信息错误"
        }
    }
}

onMounted(() => {
    getUserInfo()
})

</script>

<template>
    <div class="px-6 py-6">
        <UserSpacePageHeader
                v-if="userInfo"
                :user-info="userInfo"
        />
        <div v-else class="text-3xl p-10 text-gray-700 dark:text-gray-300">
            {{ message }}
        </div>
        <div>
        </div>
    </div>
</template>

<style scoped>

</style>