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
    <div class="flex flex-grow-1 flex-fill">
        <n-h2>
            <n-text type="primary">登录</n-text>
        </n-h2>
        <div class="flex flex-fill justify-end">
            <n-h3>
                <n-text type="info">尚未拥有账号？
                    <n-a @click="handleToRegister">点此注册</n-a>
                </n-text>
            </n-h3>
        </div>
    </div>
    <n-form ref="loginForm" :model="formValue" :rules="formRules">
        <n-form-item label="用户名/电子邮箱" path="identity">
            <n-input v-model:value="formValue.identity" placeholder="请输入用户名或电子邮箱"
                     @keydown.enter.prevent/>
        </n-form-item>
        <n-form-item label="密码" path="token">
            <n-input v-model:value="formValue.token" placeholder="请输入密码" show-password-on="click"
                     type="password"
                     @keydown.enter.prevent/>
        </n-form-item>
        <n-form-item path="rememberMe">
            <n-checkbox v-model:checked="formValue.rememberMe">
                记住我
            </n-checkbox>
        </n-form-item>
        <n-button-group class="w-full">
            <n-button class="w-full flex-grow-0" type="primary" @click="onLoginClick">
                登录
            </n-button>
            <n-button class="w-full" type="tertiary" @click="onResetClick">
                重置
            </n-button>
        </n-button-group>
        <n-a>忘记密码</n-a>
    </n-form>

</template>

<script setup>
import {getCurrentInstance, ref} from "vue";
import api from "@/request/api";
import {NA, NButton, NButtonGroup, NCheckbox, NForm, NFormItem, NInput, NText, useNotification} from "naive-ui";
import {useRouter} from "vue-router";
import {useUserStore} from "@/stores/user";
import {index, register} from "@/router";

const userStore = useUserStore()
const loginForm = ref(null)
const router = useRouter()
const notification = useNotification()
const {proxy} = getCurrentInstance()

const formValue = ref({
    identity: null,
    token: null,
    rememberMe: false
})

const formRules = {
    identity: [{
        required: true,
        validator(rule, value) {
            if (!value) {
                return new Error("需要填写用户名或电子邮箱地址")
            }
            return true
        },
        trigger: ['input']
    }],
    token: [{
        required: true,
        message: "请输入密码",
        trigger: ['input']
    }],
}

const validateFormValue = (callback) => {
    loginForm.value?.validate((errors) => {
        if (errors) {
            return
        }
        callback()
    });
}

const source = router.currentRoute.value.query.source

const onLoginClick = (e) => {
    e.preventDefault()
    validateFormValue(() => {
        proxy.$axios.post(api.passwordLogin, formValue.value).then(res => {
            /**
             * @type {{ user: {
             * username: string, id: number,
             * role: string, email: string },
             * accessToken: string, refreshToken: string,
             * accessTokenExpiry: string, refreshTokenExpiry: string
             * }}
             */
            const data = res.data
            const user = {
                id: data.user.id,
                username: data.user.username,
                role: data.user.role,
            }
            /**
             * @type {{refreshToken: string, accessToken: string, prefix: string, accessTokenExpiry: Date, refreshTokenExpiry: Date}}
             */
            const token = {
                refreshToken: data.refreshToken,
                accessToken: data.accessToken,
                prefix: "Bearer ",
                accessTokenExpiry: new Date(data.accessTokenExpiry),
                refreshTokenExpiry: new Date(data.refreshTokenExpiry),
            }

            userStore.loginUser(user, token, formValue.value.rememberMe, false)

            // check url
            if (!source) {
                router.push({
                    name: index
                })
                return
            }
            router.push(source.toString())
        }).catch(err => {
            const msg = err.tip
            notification.error({
                title: "请求错误",
                content: msg,
                meta: "登录错误",
                duration: 3000,
                keepAliveOnHover: true
            })
        })
    })
}

const onResetClick = () => {
    formValue.value = {
        identity: null,
        token: null,
        rememberMe: false
    }
    loginForm.value?.restoreValidation()
}

const handleToRegister = () => {
    router.push({
        name: register
    })
}
</script>
