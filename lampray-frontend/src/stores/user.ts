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

import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import {UserRole} from '@/services/user/user.type'
import {type BroadcastCallbacks, type UserBroadcastInstance, useUserBroadcast} from '@/composables/useUserBroadcast'

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

function tryParse<T>(value: string | null): T | null {
    if (!value) return null
    try {
        return JSON.parse(value) as T
    } catch {
        return null
    }
}

function getStorage(remember: boolean) {
    return remember ? localStorage : sessionStorage
}

export const useUserStore = defineStore('user', () => {
    const user = ref<User | null>(null)
    const token = ref<Token | null>(null)
    const userData = ref<UserData | null>(null)
    const remember = ref(false)
    const block = ref(false)

    const isLogin = computed(() => {
        if (!token.value) return false
        const now = new Date().getTime()
        return (
            token.value.accessToken &&
            token.value.refreshToken &&
            now < token.value.refreshTokenExpiry.getTime()
        )
    })

    const isBlocked = computed(() => block.value)

    const hasAdminRole = computed(() => {
        return user.value?.role && user.value.role !== UserRole.USER
    })

    let broadcast: UserBroadcastInstance | null = null

    function setLogin(newUser: User, newToken: Token, shouldRemember: boolean, isBlocked: boolean) {
        user.value = newUser
        token.value = newToken
        remember.value = shouldRemember
        block.value = isBlocked

        const storage = getStorage(shouldRemember)
        storage.setItem(userKey, JSON.stringify(newUser))
        storage.setItem(tokenKey, JSON.stringify(newToken))
        if (userData.value) {
            storage.setItem(userDataKey, JSON.stringify(userData.value))
        }
    }

    function setLogout() {
        user.value = null
        token.value = null
        userData.value = null
        remember.value = false
        block.value = false

        localStorage.removeItem(tokenKey)
        localStorage.removeItem(userKey)
        localStorage.removeItem(userDataKey)

        sessionStorage.removeItem(tokenKey)
        sessionStorage.removeItem(userKey)
        sessionStorage.removeItem(userDataKey)
    }

    function setToken(newToken: Token) {
        token.value = newToken
        const storage = getStorage(remember.value)
        storage.setItem(tokenKey, JSON.stringify(newToken))
    }

    function setUserDataFull(newUserData: UserData) {
        userData.value = newUserData
        const storage = getStorage(remember.value)
        storage.setItem(userDataKey, JSON.stringify(newUserData))
    }

    function loginUser(newUser: User, newToken: Token, shouldRemember: boolean, isBlocked: boolean) {
        setLogin(newUser, newToken, shouldRemember, isBlocked)
        broadcast?.broadcastLogin(newUser, newToken, shouldRemember, isBlocked)
    }

    function refreshToken(newToken: Token) {
        setToken(newToken)
        broadcast?.broadcastToken(newToken)
    }

    function logout() {
        setLogout()
        broadcast?.broadcastLogout()
    }

    function setUserData(partialData: Partial<UserData>) {
        const newUserData = {...userData.value, ...partialData} as UserData
        setUserDataFull(newUserData)
        broadcast?.broadcastUserData(newUserData)
    }

    function setBlock(value: boolean) {
        block.value = value
    }

    function load() {
        const localUser = localStorage.getItem(userKey)
        const localToken = localStorage.getItem(tokenKey)
        const localUserData = localStorage.getItem(userDataKey)

        if (localUser && localToken) {
            const parsedUser = tryParse<User>(localUser)
            const parsedToken = tryParse<Token>(localToken)
            if (!parsedUser || !parsedToken) return

            parsedToken.accessTokenExpiry = new Date(parsedToken.accessTokenExpiry)
            parsedToken.refreshTokenExpiry = new Date(parsedToken.refreshTokenExpiry)

            user.value = parsedUser
            token.value = parsedToken
            if (localUserData !== null) {
                userData.value = tryParse<UserData>(localUserData)
            }
            remember.value = true
            return
        }

        const sessionUser = sessionStorage.getItem(userKey)
        const sessionToken = sessionStorage.getItem(tokenKey)
        const sessionUserData = sessionStorage.getItem(userDataKey)

        if (sessionUser && sessionToken) {
            const parsedUser = tryParse<User>(sessionUser)
            const parsedToken = tryParse<Token>(sessionToken)
            if (!parsedUser || !parsedToken) return

            parsedToken.accessTokenExpiry = new Date(parsedToken.accessTokenExpiry)
            parsedToken.refreshTokenExpiry = new Date(parsedToken.refreshTokenExpiry)

            user.value = parsedUser
            token.value = parsedToken
            if (sessionUserData) {
                userData.value = tryParse<UserData>(sessionUserData)
            }
            remember.value = false
        }
    }

    /**
     * Initialize the broadcast channel with navigation callbacks.
     * This should be called once during app bootstrap (e.g., in main.ts).
     *
     * @param callbacks - Navigation callbacks for cross-tab events
     */
    function initBroadcast(callbacks?: BroadcastCallbacks): void {
        if (broadcast) {
            broadcast.dispose()
        }
        broadcast = useUserBroadcast(
            {setLogin, setLogout, setToken, setUserData: setUserDataFull},
            {callbacks, autoDispose: false}
        )
    }

    function dispose() {
        broadcast?.dispose()
        broadcast = null
    }

    return {
        user: computed(() => user.value),
        token: computed(() => token.value),
        userData: computed(() => userData.value),
        remember: computed(() => remember.value),
        block: computed(() => block.value),
        isLogin,
        isBlocked,
        hasAdminRole,
        loginUser,
        logout,
        refreshToken,
        setUserData,
        load,
        initBroadcast,
        dispose,
        setBlock,
    }
})

export type UserStore = ReturnType<typeof useUserStore>
