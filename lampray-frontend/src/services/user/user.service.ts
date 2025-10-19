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

import type {
    CreateUserRequest,
    LoginResponse,
    ResendRegisterActivateRequest,
    UserCommonDetailsVo,
    UserDetailsVo,
    UserLoginRequest,
    UserRegisterRequest,
    UserVo
} from "@/services/user/user.type.ts";
import type {HttpResponseBody, PageableRequest} from "@/services/common.type.ts";
import type {AxiosInstance, AxiosResponse, RawAxiosRequestConfig} from 'axios';

export const userService = (axios: AxiosInstance) => {
    return {
        async getCurrentUser(options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<UserVo>>> {
            const mergedOptions = {...options};
            return await axios.get<HttpResponseBody<UserVo>>(
                '/api/v1/user', mergedOptions
            );
        },
        async getUserInfo(userId: string | number, options: RawAxiosRequestConfig = {}):
            Promise<AxiosResponse<HttpResponseBody<UserCommonDetailsVo>>> {
            const mergedOptions = {...options};
            const path = `/api/v1/users/{userId}`
                .replace(`{userId}`, encodeURIComponent(String(userId)));
            return await axios.get<HttpResponseBody<UserCommonDetailsVo>>(
                path, mergedOptions
            );
        },
        async updateUser(userId: string | number, data: Partial<UserVo>, options: RawAxiosRequestConfig = {}): Promise<void> {
            await new Promise(resolve => setTimeout(resolve, 500));
            // TODO
        },
    }
}

export const userManageService = (axios: AxiosInstance) => {
    return {
        async listUsers(pageableRequest: PageableRequest, options: RawAxiosRequestConfig = {}):
            Promise<AxiosResponse<HttpResponseBody<UserDetailsVo[]>>> {
            const mergedOptions = {...options};
            if (!mergedOptions.params) {
                mergedOptions.params = {};
            }
            mergedOptions.params.page = pageableRequest.page;
            mergedOptions.params.size = pageableRequest.size;
            return await axios.get<HttpResponseBody<UserDetailsVo[]>>(
                '/api/v1/admin/users', mergedOptions
            );
        },

        async getUserDetails(userId: string | number, options: RawAxiosRequestConfig = {}):
            Promise<AxiosResponse<HttpResponseBody<UserDetailsVo>>> {
            const mergedOptions = {...options};
            const path = `/api/v1/admin/users/{userId}`
                .replace(`{userId}`, encodeURIComponent(String(userId)));
            return await axios.get<HttpResponseBody<UserDetailsVo>>(
                path, mergedOptions
            );
        },

        /**
         * Create a new user, return user ID
         */
        async createUser(request: CreateUserRequest, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<number>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers['Content-Type'] = 'application/json';
            } else {
                mergedOptions.headers = {
                    'Content-Type': 'application/json'
                };
            }
            return await axios.post<HttpResponseBody<number>>(
                '/api/v1/admin/users', request, mergedOptions
            );
        },
    }
}


export const loginRegisterService = (axios: AxiosInstance) => {
    return {
        async registerUser(userRegisterRequest: UserRegisterRequest, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers['Content-Type'] = 'application/json';
            } else {
                mergedOptions.headers = {
                    'Content-Type': 'application/json'
                };
            }
            return await axios.post<HttpResponseBody<void>>(
                '/api/v1/user/register', userRegisterRequest, mergedOptions
            );
        },

        async loginByPassword(userLoginRequest: UserLoginRequest, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<LoginResponse>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers['Content-Type'] = 'application/json';
            } else {
                mergedOptions.headers = {
                    'Content-Type': 'application/json'
                };
            }
            return await axios.post<HttpResponseBody<LoginResponse>>(
                '/api/v1/user/login/password', userLoginRequest, mergedOptions
            );
        },

        async activateUser(token: string, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers['Content-Type'] = 'application/json';
            } else {
                mergedOptions.headers = {
                    'Content-Type': 'application/json'
                };
            }
            const path = `/api/v1/user/register/token/{token}`
                .replace(`{token}`, encodeURIComponent(String(token)));
            return await axios.post<HttpResponseBody<void>>(
                path, undefined, mergedOptions
            );
        },

        async resendActivationEmail(request: ResendRegisterActivateRequest,
                                    options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers['Content-Type'] = 'multipart/form-data';
            } else {
                mergedOptions.headers = {
                    'Content-Type': 'multipart/form-data'
                };
            }
            return await axios.post<HttpResponseBody<void>>(
                '/api/v1/user/register/token', request, mergedOptions
            );
        }
    }
}

