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
export interface UserRegisterRequest {
    username: string;
    password: string;
    email: string;
}

export interface ResendRegisterActivateRequest {
    email: string;
    username: string;
}

export interface UserLoginRequest {
    identity: string;
    token: string;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    accessTokenExpiry: string;
    refreshTokenExpiry: string;
    user: UserVo;
}

export interface UserVo {
    id: number;
    username: string;
    email: string;
    role: UserRole;
}

export interface UserCommonDetailsVo {
    userId: number;
    role: UserRole;
    username: string;
    email: string;
    nickname: string;
    avatar: string;
    cover: string;
    introduction: string;
    gender: Gender;
    birthday?: Birthday;
    website?: string;
    location?: string;
}

export interface CreateUserRequest {
    username: string;
    password: string;
    email: string;
    role: UserRole;
}

export interface UserDetailsVo {
    userId: number;
    role: UserRole;
    username: string;
    email: string;
    enabled: boolean;
    locked: boolean;
    canceled: boolean;
    createTime: string;
    updateTime: string;
    nickname: string;
    avatar: string;
    cover: string;
    introduction: string;
    gender: Gender;
    birthday?: Birthday;
    website: string;
    location: string;

}

export interface Birthday {
    year: number;
    month: number;
    day: number;
    birthday: string;
}

export const UserRole = {
    USER: 'USER',
    ADMIN: 'ADMIN',
    STAFF: 'STAFF',
    REVIEWER: 'REVIEWER',
    CUSTOMER_SERVICE: 'CUSTOMER_SERVICE',
    EDITOR: 'EDITOR'
} as const;

export type UserRole = typeof UserRole[keyof typeof UserRole];

export const Gender = {
    MALE: 'MALE',
    FEMALE: 'FEMALE',
    PRIVATE: 'PRIVATE'
} as const;

export type Gender = typeof Gender[keyof typeof Gender];


