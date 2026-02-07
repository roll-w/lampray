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

import type {User, UserData, Token} from '@/stores/user'

type BroadcastMessageType = 'login' | 'logout' | 'refresh-token' | 'update-user-data'

interface BroadcastMessage {
    type: BroadcastMessageType
    data?: {
        user?: User
        token?: Token
        userData?: UserData
        remember?: boolean
        block?: boolean
    }
}

const CHANNEL_NAME = 'lampray-user-sync'

export interface UserBroadcast {
    dispose: () => void
    broadcastLogin: (user: User, token: Token, remember: boolean, block: boolean) => void
    broadcastLogout: () => void
    broadcastToken: (token: Token) => void
    broadcastUserData: (userData: UserData) => void
}

export function useUserBroadcast(
    setters: {
        setLogin: (user: User, token: Token, remember: boolean, block: boolean) => void
        setLogout: () => void
        setToken: (token: Token) => void
        setUserData: (userData: UserData) => void
    }
): UserBroadcast {
    const channel = createBroadcastChannel()

    if (channel) {
        channel.onmessage = (event: MessageEvent<BroadcastMessage>) => {
            const {type, data} = event.data
            handleMessage(type, data)
        }
    }

    function createBroadcastChannel(): BroadcastChannel | null {
        if (typeof window === 'undefined' || !('BroadcastChannel' in window)) {
            return null
        }
        return new BroadcastChannel(CHANNEL_NAME)
    }

    function handleMessage(type: BroadcastMessageType, data?: BroadcastMessage['data']) {
        switch (type) {
            case 'login':
                if (data?.user && data?.token) {
                    setters.setLogin(
                        data.user,
                        data.token,
                        data.remember ?? false,
                        data.block ?? false
                    )
                }
                break
            case 'logout':
                setters.setLogout()
                break
            case 'refresh-token':
                if (data?.token) {
                    setters.setToken(data.token)
                }
                break
            case 'update-user-data':
                if (data?.userData) {
                    setters.setUserData(data.userData)
                }
                break
        }
    }

    function post(type: BroadcastMessageType, data?: BroadcastMessage['data']) {
        channel?.postMessage({type, data})
    }

    function broadcastLogin(user: User, token: Token, remember: boolean, block: boolean) {
        post('login', {user, token, remember, block})
    }

    function broadcastLogout() {
        post('logout')
    }

    function broadcastToken(token: Token) {
        post('refresh-token', {token})
    }

    function broadcastUserData(userData: UserData) {
        post('update-user-data', {userData})
    }

    function dispose() {
        channel?.close()
    }

    return {
        dispose,
        broadcastLogin,
        broadcastLogout,
        broadcastToken,
        broadcastUserData
    }
}

export type UserBroadcastInstance = ReturnType<typeof useUserBroadcast>
