<script setup lang="ts">
import {useRouter} from "vue-router";

const router = useRouter();

</script>

<template>
    <div class="h-[75vh] flex flex-col justify-center items-center">
        <div class="p-5">
            <UError :error="{
                statusCode: 404,
                statusMessage: '页面不存在',
                message: '您访问的页面不存在，可能是因为地址错误或页面已被删除。',}">
                <template #links>
                    <UButton color="primary" @click="router.push('/')">回到首页</UButton>
                </template>
            </UError>
        </div>
    </div>
</template>

<style scoped>

</style>