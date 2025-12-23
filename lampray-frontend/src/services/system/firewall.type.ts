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

export interface FilterEntryVo {
    identifier: string;
    type: FilterIdentifierType;
    mode: FilterMode;
    expiration: string;  // ISO datetime string from backend
    reason: string;
}

export interface AddFilterEntryRequest {
    identifier: string;
    type: FilterIdentifierType;
    mode: FilterMode;
    expirationSeconds: number;  // Duration in seconds, -1 for permanent
    reason: string;
}

export interface UpdateFilterEntryRequest {
    identifier: string;
    type: FilterIdentifierType;
    mode: FilterMode;
    expirationSeconds: number;  // Duration in seconds, -1 for permanent
    reason: string;
}

export const FilterIdentifierType = {
    IP: "IP",
    USER: "USER",
} as const;

export type FilterIdentifierType = typeof FilterIdentifierType[keyof typeof FilterIdentifierType];

export const FilterMode = {
    ALLOW: "ALLOW",
    DENY: "DENY",
} as const;

export type FilterMode = typeof FilterMode[keyof typeof FilterMode];

export interface FirewallInfo {
    name: string;
    description: string;
    type: string;
    status: string;
    priority: number;
}

