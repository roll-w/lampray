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
import {refreshToken} from "@/request/api_urls.js";

axios.defaults.withCredentials = true

export function createConfig(isJson = false) {
    const config = {
        headers: {}
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

const refreshExpired = (userStore) => {
    if (userStore.token && userStore.token.refreshTokenExpiry) {
        return new Date().getTime() > new Date(userStore.token.refreshTokenExpiry).getTime()
    }
    return true
}

const accessExpired = (userStore) => {
    if (userStore.token && userStore.token.accessTokenExpiry) {
        return new Date().getTime() > new Date(userStore.token.accessTokenExpiry).getTime()
    }
    return true
}

export function createAxios(onLoginExpired = () => {
                            },
                            onUserBlocked = () => {
                            }) {
    const instance = axios.create({
        withCredentials: true,
    })
    instance.interceptors.request.use(
        async config => {
            if (config.url === refreshToken) {
                // Skip adding token for refresh token request
                return config
            }
            const userStore = useUserStore()
            const rawToken = userStore.getToken
            if (accessExpired(userStore) && !refreshExpired(userStore)) {
                const promise = instance.post(refreshToken, {
                    refreshToken: rawToken.refreshToken
                });
                // TODO: avoid multiple refresh token requests
                await promise.then(resp => {
                    userStore.refreshToken({
                        accessToken: resp.data.accessToken,
                        accessTokenExpiry: new Date(resp.data.accessTokenExpiry),
                        refreshToken: rawToken.refreshToken,
                        refreshTokenExpiry: rawToken.refreshTokenExpiry,
                        prefix: rawToken.prefix
                    })
                    config.headers["Authorization"] = userStore.token.prefix + userStore.token.accessToken
                }).catch((error) => {
                    return Promise.reject(error.response.data)
                })
            }

            if (userStore.isLogin) {
                config.headers["Authorization"] = userStore.token.prefix + userStore.token.accessToken
            }
            return config
        }, error => {
            return Promise.reject(error.response.data)
        }
    )

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

