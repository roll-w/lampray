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

import {defineStore} from 'pinia'
import {UserRole} from "@/services/user/user.type.ts";

export type UserStore = ReturnType<typeof useUserStore>

export interface User {
    username: string
    id: number
    role: UserRole
}

export interface UserData {
    avatar: string
    nickname: string
    setup: boolean
}

export interface Token {
    refreshToken: string
    accessToken: string
    prefix: string
    accessTokenExpiry: Date
    refreshTokenExpiry: Date
}

export const tokenKey = 'token'
export const userKey = 'user'
export const userDataKey = 'user_data'

export const useUserStore = defineStore('user', {
    state: (): {
        user: User | null
        token: Token | null
        userData: UserData | null
        remember: boolean
        block: boolean
    } => ({
        user: null,
        token: null,
        userData: null,
        remember: false,
        block: false,
    }),
    getters: {
        isLogin: (state) => {
            if (!state.token) return false
            const now = new Date().getTime()
            return (
                state.token!.accessToken &&
                state.token!.refreshToken &&
                now < state.token!.refreshTokenExpiry.getTime()
            )
        },

        getUser: (state) => state.user,
        getToken: (state) => state.token,
        getUserData: (state) => state.userData,
        isBlocked: (state) => state.block,

        hasAdminRole: (state) => state.user?.role && state.user.role !== UserRole.USER,
    },
    actions: {
        loginUser(
            user: User,
            token: Token,
            remember: boolean,
            block: boolean
        ) {
            this.user = user
            this.token = token
            this.remember = remember
            this.block = block

            const storage = this.remember ? localStorage : sessionStorage
            storage.setItem(userKey, JSON.stringify(this.user))
            storage.setItem(tokenKey, JSON.stringify(this.token))
            if (this.userData) {
                storage.setItem(userDataKey, JSON.stringify(this.userData))
            }
        },

        refreshToken(token: Token) {
            this.token = token
            const storage = this.remember ? localStorage : sessionStorage
            storage.setItem(tokenKey, JSON.stringify(this.token))
        },

        logout() {
            this.$reset()

            localStorage.removeItem(tokenKey)
            localStorage.removeItem(userKey)
            localStorage.removeItem(userDataKey)

            sessionStorage.removeItem(tokenKey)
            sessionStorage.removeItem(userKey)
            sessionStorage.removeItem(userDataKey)
        },

        setUserData(userData: Partial<UserData>) {
            this.userData = {...this.userData, ...userData} as UserData
            const storage = this.remember ? localStorage : sessionStorage
            storage.setItem(userDataKey, JSON.stringify(userData))
        },

        load() {
            const localUser = localStorage.getItem(userKey)
            const localToken = localStorage.getItem(tokenKey)
            const localUserData = localStorage.getItem(userDataKey)

            if (localUser && localToken) {
                this.user = tryParse<User>(localUser)
                const rawToken = tryParse<Token>(localToken)
                if (!rawToken) {
                    return
                }
                rawToken.accessTokenExpiry = new Date(rawToken.accessTokenExpiry)
                rawToken.refreshTokenExpiry = new Date(rawToken.refreshTokenExpiry)
                this.token = rawToken
                if (localUserData !== null) {
                    this.userData = tryParse<UserData>(localUserData)
                }
                this.remember = true
                return
            }

            const sessionUser = sessionStorage.getItem(userKey)
            const sessionToken = sessionStorage.getItem(tokenKey)
            const sessionUserData = sessionStorage.getItem(userDataKey)
            if (sessionUser && sessionToken) {
                this.user = tryParse<User>(sessionUser)
                const rawToken = tryParse<Token>(sessionToken)
                if (!rawToken) {
                    return;
                }
                rawToken.accessTokenExpiry = new Date(rawToken.accessTokenExpiry)
                rawToken.refreshTokenExpiry = new Date(rawToken.refreshTokenExpiry)
                this.token = rawToken
                if (sessionUserData) {
                    this.userData = tryParse<UserData>(sessionUserData)
                }
                this.remember = false
            }
        }
    },
})

const tryParse = <T>(value: string | null): T | null => {
    if (!value) {
        return null;
    }
    try {
        return JSON.parse(value) as T;
    } catch {
        return null;
    }
}