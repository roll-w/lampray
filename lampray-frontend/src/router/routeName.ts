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

export const RouteName = {
    USER_HOME: "user-home",
    LOGIN: "login",
    REGISTER: "register",

    REGISTER_TIPS: "register-tips",
    REGISTER_ACTIVATE: "register-activate",

    USER_PROFILE: "user-profile",
    USER_SETTINGS: "user-settings",

    USER_SPACE: "user-space",

    ADMIN_HOME: "admin-home",
    ADMIN_USER_LIST: "admin-user-list",
    ADMIN_USER_DETAIL: "admin-user-detail",

    ADMIN_SYSTEM_SETTINGS: "admin-system-settings",

    NOT_FOUND: "not-found",
    BLOCKED: "blocked",
} as const;

export type RouteName = typeof RouteName[keyof typeof RouteName];
