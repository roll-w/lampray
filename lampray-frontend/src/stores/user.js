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

export const tokenKey = window.btoa("L2w9t0k3n")
export const userKey = "user"
export const userDataKey = "user_data"

export const useUserStore = defineStore('user', {
    state: () => ({
        /** @type {{ username: string, id: number, role: string }} */
        user: {},
        /** @type {{refreshToken: string, accessToken: string, prefix: string, accessTokenExpiry: Date, refreshTokenExpiry: Date}} */
        token: {},
        /** @type {{ avatar: string, nickname: string, setup: boolean }} */
        userData: {},
        remember: false,
        /** @type {boolean} */
        block: false,
    }),
    getters: {
        /** @return {boolean} */
        isLogin: state => state.token !== "",
        /** @return {{ username: string, id: number, role: string }} */
        getUser: state => state.user,
        /** @return {{refreshToken: string, accessToken: string, prefix: string, accessTokenExpiry: Date, refreshTokenExpiry: Date}} */
        getToken: state => state.token,
        /** @return {{ avatar: string, nickname: string, setup: boolean }} */
        getUserData: state => state.userData,

        /** @return {boolean} */
        isBlocked: state => state.block,

        canAccessAdmin: state => state.user.role && state.user.role !== "USER",
    },
    actions: {
        /**
         * @param {{ username: string, id: number, role: string }} user
         * @param {{refreshToken: string, accessToken: string, prefix: string, accessTokenExpiry: Date, refreshTokenExpiry: Date}} token
         * @param {boolean} remember
         * @param {boolean} block
         */
        loginUser(user, token, remember, block) {
            this.user = user
            this.token = token
            this.remember = remember
            this.block = block
        },

        logout() {
            this.user = {}
            this.token = {}
            this.userData = {}
            this.remember = false
            this.block = false

            localStorage.removeItem(tokenKey)
            localStorage.removeItem(userKey)
            localStorage.removeItem(userDataKey)

            sessionStorage.removeItem(tokenKey)
            sessionStorage.removeItem(userKey)
            sessionStorage.removeItem(userDataKey)
        },

        /**
         * @param {{ avatar: string, nickname: string }} userData
         */
        setUserData(userData) {
            this.userData = userData
        }
    }
})
