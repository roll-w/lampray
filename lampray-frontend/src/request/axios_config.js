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
import {useUserStore} from "@/stores/user"
import {refreshToken} from "@/request/api_urls.js"

axios.defaults.withCredentials = true

// Error codes configuration
const ERROR_CODES = {
    TOKEN: ["A1001", "A1002"],
    BLOCK: ["A1011"]
}

// Global refresh promise to prevent multiple concurrent refresh requests
let refreshPromise = null

/**
 * Create request configuration
 */
export function createConfig(isJson = false) {
    const config = {headers: {}}
    if (isJson) {
        config.headers["Content-Type"] = "application/json"
    }
    return config
}

/**
 * Check if token is expired
 */
const isTokenExpired = (expiry) => {
    if (!expiry) return true
    return Date.now() > new Date(expiry).getTime()
}

/**
 * Get authorization header value
 */
const getAuthHeader = (userStore) => {
    const token = userStore.token
    return token ? `${token.prefix}${token.accessToken}` : null
}

/**
 * Refresh access token
 */
const performTokenRefresh = async (instance, userStore) => {
    const currentToken = userStore.getToken

    if (!currentToken?.refreshToken || isTokenExpired(currentToken.refreshTokenExpiry)) {
        throw new Error('Refresh token expired')
    }

    const response = await instance.post(refreshToken, {
        refreshToken: currentToken.refreshToken
    })

    userStore.refreshToken({
        accessToken: response.data.accessToken,
        accessTokenExpiry: new Date(response.data.accessTokenExpiry),
        refreshToken: currentToken.refreshToken,
        refreshTokenExpiry: currentToken.refreshTokenExpiry,
        prefix: currentToken.prefix
    })

    return response.data
}

/**
 * Handle token refresh with concurrent request protection
 */
const handleTokenRefresh = async (instance, userStore) => {
    if (refreshPromise) {
        await refreshPromise
        return
    }

    refreshPromise = performTokenRefresh(instance, userStore)
        .finally(() => {
            refreshPromise = null
        })

    await refreshPromise
}

/**
 * Check if error code is in specific error list
 */
const isErrorCodeMatch = (errorCode, errorList) => {
    return errorList.includes(errorCode || "00000")
}

/**
 * Create axios instance with interceptors
 */
export function createAxios(onLoginExpired = () => {}, onUserBlocked = () => {}) {
    const instance = axios.create({
        withCredentials: true,
    })

    // Request interceptor
    instance.interceptors.request.use(
        async (config) => {
            // Skip token handling for refresh token requests
            if (config.url === refreshToken) {
                return config
            }

            const userStore = useUserStore()

            if (!userStore.isLogin) {
                return config
            }

            const token = userStore.token

            // Check if access token needs refresh
            if (isTokenExpired(token?.accessTokenExpiry) && !isTokenExpired(token?.refreshTokenExpiry)) {
                try {
                    await handleTokenRefresh(instance, userStore)
                } catch (error) {
                    console.error('Token refresh failed:', error)
                    return Promise.reject({
                        tip: "Login expired",
                        message: "Token refresh failed",
                        status: 401
                    })
                }
            }

            // Add authorization header
            const authHeader = getAuthHeader(userStore)
            if (authHeader) {
                config.headers["Authorization"] = authHeader
            }

            return config
        },
        (error) => Promise.reject(error.response?.data || error)
    )

    // Response interceptor
    instance.interceptors.response.use(
        (response) => {
            if (response.data.errorCode !== "00000") {
                return Promise.reject(response.data)
            }
            return response.data
        },
        (error) => {
            const errorData = error.response?.data
            const errorCode = errorData?.errorCode || "00000"

            // Handle user blocked error
            if (isErrorCodeMatch(errorCode, ERROR_CODES.BLOCK)) {
                onUserBlocked()
                return Promise.reject({
                    tip: "Account blocked",
                    message: "Account has been blocked",
                    errorCode: errorCode,
                    status: 403
                })
            }

            // Handle token error
            if (isErrorCodeMatch(errorCode, ERROR_CODES.TOKEN)) {
                onLoginExpired()
                return Promise.reject({
                    tip: "Login expired",
                    message: "Please login again",
                    errorCode: errorCode,
                    status: 401
                })
            }

            return Promise.reject(errorData || error)
        }
    )

    return instance
}
