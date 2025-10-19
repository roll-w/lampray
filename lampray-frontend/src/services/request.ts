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

interface TokenRefreshResponse {
    accessToken: string
    accessTokenExpiry: string
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
    const currentToken = userStore.getToken

    if (!currentToken?.refreshToken || isTokenExpired(currentToken.refreshTokenExpiry)) {
        throw new Error('Refresh token expired')
    }

    const response = await instance.post<HttpResponseBody<TokenRefreshResponse>>(
        refreshTokenUrl,
        {
            refreshToken: currentToken.refreshToken
        },
    )
    const data = response.data.data!

    const newToken: Token = {
        accessToken: data.accessToken,
        accessTokenExpiry: new Date(data.accessTokenExpiry),
        refreshToken: currentToken.refreshToken,
        refreshTokenExpiry: currentToken.refreshTokenExpiry,
        prefix: currentToken.prefix
    }
    userStore.refreshToken(newToken)
    return newToken
}

/**
 * Handle token refresh with concurrent request protection
 */
const handleTokenRefresh = async (instance: AxiosInstance, userStore: UserStore): Promise<void> => {
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
    onLoginExpired: () => void = () => {
    },
    onUserBlocked: () => void = () => {
    }
): AxiosInstance {
    const instance = axios.create({
        withCredentials: true
    })

    // Request interceptor
    instance.interceptors.request.use(
        async (config) => {
            // Skip auth for refresh token request
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
                    console.error('Token refresh failed:', error)
                    return Promise.reject(new Error('Token refresh failed'))
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
            if (response.data.errorCode !== '00000') {
                return Promise.reject<AxiosResponse<HttpResponseBody<void>>>(response)
            }
            console.log("Success response: ", response)
            return response
        },
        (error) => {
            const errorData: HttpResponseBody<void> = error.response?.data || error
            const errorCode = errorData.errorCode || '00000'

            console.log("Error response: ", errorData)

            // Handle user blocked error
            if (isErrorCodeMatch(errorCode, ERROR_CODES.BLOCK)) {
                onUserBlocked()
                return Promise.reject<AxiosResponse<HttpResponseBody<void>>>({
                    ...error, response: { data: errorData }
                })
            }

            // Handle token error
            if (isErrorCodeMatch(errorCode, ERROR_CODES.TOKEN)) {
                onLoginExpired()
                return Promise.reject<AxiosResponse<HttpResponseBody<void>>>({
                    ...error, response: { data: errorData }
                })
            }

            return Promise.reject<AxiosResponse<HttpResponseBody<void>>>({
                ...error, response: { data: errorData }
            })
        }
    )

    return instance
}
