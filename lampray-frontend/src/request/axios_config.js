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

import axios from 'axios'
import {useUserStore} from "@/stores/user";

axios.defaults.withCredentials = true

export function createConfig(isJson = false) {
    const userStore = useUserStore()
    const config = {
        headers: {}
    }
    if (userStore.isLogin) {
        config.headers["Authorization"] = userStore.token.prefix + userStore.token.accessToken
    }
    if (isJson) {
        config.headers["Content-Type"] = "application/json"
    }
    return config
}

const tokenErrorCodes = [
    "A1001",
    "A1002"
]

const blockErrorCodes = [
    "A1011",
]

export function createAxios(onLoginExpired = () => {
                            },
                            onUserBlocked = () => {
                            }) {
    const instance = axios.create({
        withCredentials: true,
    })
    instance.interceptors.response.use(
        response => {
            console.log(response)


            if (response.data.errorCode !== "00000") {
                return Promise.reject(response.data)
            }
            return response.data
        }, error => {
            console.log(error)
            if (isInError(error.response.data.errorCode || "00000", blockErrorCodes)) {
                onUserBlocked()
                return Promise.reject({
                    tip: "账号已被封禁",
                    message: "账号已被封禁",
                    errorCode: response.data.errorCode,
                    status: 403
                })
            }
            if (isInError(error.response.data.errorCode || "00000", tokenErrorCodes)) {
                onLoginExpired()
                return Promise.reject({
                    tip: "登录过期",
                    message: "登录过期",
                    errorCode: error.response.data.errorCode,
                    status: 401
                })
            }
            return Promise.reject(error.response.data)
        }
    )
    return instance
}

function isInError(errorCode = '00000', errorCodes = tokenErrorCodes) {
    return errorCodes.includes(errorCode)
}

