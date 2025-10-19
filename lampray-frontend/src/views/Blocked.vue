<script setup lang="ts">
import {useRouter} from "vue-router";

const router = useRouter();


</script>

<template>
    <div class="h-[75vh] flex flex-col justify-center items-center">
        <div class="p-5 ">
            <UError :error="{
                statusCode: 403,
                statusMessage: '访问被阻止',
                message: '您没有权限访问此页面，可能是因为您的账号被封禁或权限不足。',}">
                <template #links>
                    <UButton color="primary" @click="router.push('/')">回到首页</UButton>
                </template>
            </UError>
        </div>
    </div>
</template>

<style scoped>

</style>