/*
 * Copyright (C) 2023-2026 RollW
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

import type {AxiosInstance, AxiosResponse} from "axios"
import axios from "axios"
import type {Token, UserStore} from "@/stores/user.ts";
import type {HttpResponseBody} from "@/services/common.type.ts";

// Error codes configuration
const ERROR_CODES = {
    TOKEN: ['A1001', 'A1002'],
    BLOCK: ['A1011']
} as const

const refreshTokenUrl = '/api/v1/auth/token:refresh';
const rememberMeHeader = 'X-Lampray-Remember-Me';

interface TokenRefreshResponse {
    accessToken: string
    accessTokenExpiry: string
    refreshTokenExpiry: string
}

interface AxiosLifecycleCallbacks {
    onLoginExpired?: () => void
    onUserBlocked?: () => void
}


// Global refresh promise to prevent multiple concurrent refresh requests
let refreshPromise: Promise<Token> | null = null

/**
 * Check if token is expired
 */
const isTokenExpired = (expiry: Date | null | undefined): boolean => {
    if (!expiry) return true
    return Date.now() > expiry.getTime()
}

/**
 * Get authorization header value
 */
const getAuthHeader = (token: Token | null): string | null => {
    if (!token?.accessToken) return null
    return `${token.prefix}${token.accessToken}`
}

/**
 * Refresh access token
 */
const performTokenRefresh = async (
    instance: AxiosInstance,
    userStore: UserStore
): Promise<Token> => {
    const currentToken = userStore.token

    if (!currentToken || isTokenExpired(currentToken.refreshTokenExpiry)) {
        throw new Error('Refresh token expired')
    }

    const response = await instance.post<HttpResponseBody<TokenRefreshResponse>>(
        refreshTokenUrl,
        undefined,
        {
            headers: {
                [rememberMeHeader]: String(userStore.remember)
            }
        }
    )
    const data = response.data.data!
    const refreshedRefreshTokenExpiry = new Date(data.refreshTokenExpiry)

    const newToken: Token = {
        accessToken: data.accessToken,
        accessTokenExpiry: new Date(data.accessTokenExpiry),
        refreshTokenExpiry: refreshedRefreshTokenExpiry,
        prefix: currentToken.prefix
    }
    userStore.refreshToken(newToken)

    return newToken
}

/**
 * Handle token refresh with concurrent request protection
 */
const handleTokenRefresh = async (
    instance: AxiosInstance,
    userStore: UserStore
): Promise<void> => {
    if (refreshPromise) {
        await refreshPromise
        return
    }

    refreshPromise = performTokenRefresh(instance, userStore).finally(() => {
        refreshPromise = null
    })

    await refreshPromise
}

/**
 * Check if error code is in specific error list
 */
const isErrorCodeMatch = (errorCode: string | undefined, errorList: readonly string[]): boolean => {
    return errorList.includes(errorCode || '00000')
}

/**
 * Create axios instance with interceptors
 */
export function createAxios(
    userStore: UserStore,
    callbacks: AxiosLifecycleCallbacks = {}
): AxiosInstance {
    const {
        onLoginExpired = () => {
        },
        onUserBlocked = () => {
        }
    } = callbacks

    const instance = axios.create({
        withCredentials: true
    })

    // Request interceptor
    instance.interceptors.request.use(
        async (config) => {
            // Skip auth for refresh token request
            config.headers["Accept-Language"] = document.documentElement.lang || "en,en-US;q=0.9,zh;q=0.8,zh-CN;q=0.7"
            if (config.url === refreshTokenUrl) {
                return config
            }

            if (!userStore.isLogin) {
                return config
            }

            const token = userStore.token

            // Check if access token needs refresh
            if (
                token &&
                isTokenExpired(token.accessTokenExpiry) &&
                !isTokenExpired(token.refreshTokenExpiry)
            ) {
                try {
                    await handleTokenRefresh(instance, userStore)
                } catch (error) {
                    console.warn('Token refresh failed.')
                    return Promise.reject(error)
                }
            }

            // Add authorization header
            const authHeader = getAuthHeader(userStore.token)
            if (authHeader) {
                config.headers["Authorization"] = authHeader
            }

            return config
        },
        (error) => Promise.reject<AxiosResponse<HttpResponseBody<void>>>(error)
    )

    // Response interceptor
    instance.interceptors.response.use(
        (response: AxiosResponse) => {
            if (!response.data || !response.data.errorCode) {
                return response
            }

            if (response.data.errorCode !== '00000') {
                return Promise.reject<AxiosResponse<HttpResponseBody<void>>>(response)
            }
            return response
        },
        (error) => {
            const errorData: HttpResponseBody<void> = error.response?.data || error
            const errorCode = errorData.errorCode || '00000'

            // Handle user blocked error
            if (isErrorCodeMatch(errorCode, ERROR_CODES.BLOCK)) {
                onUserBlocked()
                return Promise.reject<AxiosResponse<HttpResponseBody<void>>>({
                    ...error, response: {data: errorData}
                })
            }

            // Handle token error
            if (isErrorCodeMatch(errorCode, ERROR_CODES.TOKEN)) {
                onLoginExpired()
                return Promise.reject<AxiosResponse<HttpResponseBody<void>>>({
                    ...error, response: {data: errorData}
                })
            }

            return Promise.reject<AxiosResponse<HttpResponseBody<void>>>({
                ...error, response: {data: errorData}
            })
        }
    )

    return instance
}
