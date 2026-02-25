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

import {onScopeDispose} from "vue";
import type {Token, User, UserData} from "@/stores/user";

export const BROADCAST_CHANNEL_NAME = "lampray-user-sync" as const;

export type BroadcastMessageType =
    | "login"
    | "logout"
    | "refresh-token"
    | "update-user-data";

interface BaseBroadcastMessage<T extends BroadcastMessageType> {
    readonly type: T
    readonly origin: string // Tab identifier to distinguish source
}

interface LoginBroadcastMessage extends BaseBroadcastMessage<"login"> {
    readonly data: {
        readonly user: User
        readonly token: Token
        readonly remember: boolean
        readonly block: boolean
    }
}

interface LogoutBroadcastMessage extends BaseBroadcastMessage<"logout"> {
    // No data needed for logout
}

interface RefreshTokenBroadcastMessage extends BaseBroadcastMessage<"refresh-token"> {
    readonly data: {
        readonly token: Token
    }
}

interface UpdateUserDataBroadcastMessage extends BaseBroadcastMessage<"update-user-data"> {
    readonly data: {
        readonly userData: UserData
    }
}

export type BroadcastMessage =
    | LoginBroadcastMessage
    | LogoutBroadcastMessage
    | RefreshTokenBroadcastMessage
    | UpdateUserDataBroadcastMessage

export interface BroadcastCallbacks {
    /**
     * Called when a login broadcast is received from another tab.
     * Use this to trigger navigation after cross-tab login.
     */
    onLogin?: (user: User, token: Token, remember: boolean, block: boolean) => void

    /**
     * Called when a logout broadcast is received from another tab.
     * Use this to trigger navigation after cross-tab logout.
     */
    onLogout?: () => void

    /**
     * Called when a token refresh broadcast is received.
     */
    onTokenRefresh?: (token: Token) => void

    /**
     * Called when a user data update broadcast is received.
     */
    onUserDataUpdate?: (userData: UserData) => void
}

export interface BroadcastSetters {
    setLogin: (user: User, token: Token, remember: boolean, block: boolean) => void
    setLogout: () => void
    setToken: (token: Token) => void
    setUserData: (userData: UserData) => void
}

/**
 * Generate a unique tab identifier for broadcast origin tracking.
 * This helps distinguish between local and remote broadcasts.
 */
function generateTabId(): string {
    // Use existing window.name if set, otherwise generate new ID
    if (typeof window !== "undefined" && window.name) {
        return window.name;
    }
    const id = `tab-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
    if (typeof window !== "undefined") {
        window.name = id;
    }
    return id;
}

/**
 * Check if BroadcastChannel API is available.
 */
function isBroadcastChannelSupported(): boolean {
    return typeof window !== "undefined" && "BroadcastChannel" in window;
}

/**
 * Create a BroadcastChannel instance if supported.
 */
function createChannel(): BroadcastChannel | null {
    if (!isBroadcastChannelSupported()) {
        return null;
    }
    return new BroadcastChannel(BROADCAST_CHANNEL_NAME);
}

function normalizeToken(token: Token): Token {
    return {
        ...token,
        accessTokenExpiry: new Date(token.accessTokenExpiry),
        refreshTokenExpiry: new Date(token.refreshTokenExpiry)
    };
}

export interface UserBroadcast {
    /**
     * Manually close the broadcast channel.
     * Usually not needed as onScopeDispose handles cleanup automatically.
     */
    readonly dispose: () => void

    /**
     * Broadcast login event to other tabs.
     */
    readonly broadcastLogin: (user: User, token: Token, remember: boolean, block: boolean) => void

    /**
     * Broadcast logout event to other tabs.
     */
    readonly broadcastLogout: () => void

    /**
     * Broadcast token refresh event to other tabs.
     */
    readonly broadcastToken: (token: Token) => void

    /**
     * Broadcast user data update event to other tabs.
     */
    readonly broadcastUserData: (userData: UserData) => void

    /**
     * Check if this instance originated the last broadcast.
     * Useful for avoiding self-triggered actions.
     */
    readonly isLocalOrigin: () => boolean
}

export interface UseUserBroadcastOptions {
    /**
     * Callbacks triggered when receiving broadcasts from other tabs.
     * These are called AFTER the setters update the local state.
     */
    callbacks?: BroadcastCallbacks

    /**
     * Whether to automatically dispose when the scope is invalidated.
     * @default true
     */
    autoDispose?: boolean
}

export function useUserBroadcast(
    setters: BroadcastSetters,
    options?: UseUserBroadcastOptions
): UserBroadcast {
    const {callbacks, autoDispose = true} = options ?? {};

    const channel = createChannel();
    const tabId = generateTabId();
    let lastOrigin: string | null = null;

    // Set up message handler
    if (channel) {
        channel.onmessage = (event: MessageEvent<BroadcastMessage>) => {
            const message = event.data;
            lastOrigin = message.origin;

            // Don't process our own broadcasts
            if (message.origin === tabId) {
                return;
            }

            handleMessage(message);
        }
    }

    /**
     * Handle incoming broadcast messages with type-safe discrimination.
     */
    function handleMessage(message: BroadcastMessage): void {
        switch (message.type) {
            case "login": {
                const {user, token, remember, block} = message.data;
                const normalizedToken = normalizeToken(token);
                setters.setLogin(user, normalizedToken, remember, block);
                callbacks?.onLogin?.(user, normalizedToken, remember, block);
                break;
            }
            case "logout": {
                setters.setLogout();
                callbacks?.onLogout?.();
                break;
            }
            case "refresh-token": {
                const {token} = message.data;
                const normalizedToken = normalizeToken(token);
                setters.setToken(normalizedToken);
                callbacks?.onTokenRefresh?.(normalizedToken);
                break;
            }
            case "update-user-data": {
                const {userData} = message.data;
                setters.setUserData(userData);
                callbacks?.onUserDataUpdate?.(userData);
                break;
            }
        }
    }

    /**
     * Post a message to the broadcast channel.
     */
    function post<T extends BroadcastMessage>(message: T): void {
        channel?.postMessage(message);
    }

    function broadcastLogin(user: User, token: Token, remember: boolean, block: boolean): void {
        post({
            type: "login",
            origin: tabId,
            data: {user, token, remember, block}
        });
    }

    function broadcastLogout(): void {
        post({
            type: "logout",
            origin: tabId
        });
    }

    function broadcastToken(token: Token): void {
        post({
            type: "refresh-token",
            origin: tabId,
            data: {token}
        });
    }

    function broadcastUserData(userData: UserData): void {
        post({
            type: "update-user-data",
            origin: tabId,
            data: {userData}
        });
    }

    function dispose(): void {
        channel?.close();
    }

    function isLocalOrigin(): boolean {
        return lastOrigin === tabId;
    }

    // Auto-dispose when scope is invalidated (e.g., component unmounted)
    if (autoDispose) {
        onScopeDispose(dispose, true);
    }

    return {
        dispose,
        broadcastLogin,
        broadcastLogout,
        broadcastToken,
        broadcastUserData,
        isLocalOrigin
    };
}

export type UserBroadcastInstance = ReturnType<typeof useUserBroadcast>;
