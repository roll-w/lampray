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

import type {AxiosInstance, RawAxiosRequestConfig} from "axios";
import type {HttpResponseBody, PageableRequest} from "@/services/common.type.ts";
import type {SettingDetailsVo, SettingVo} from "@/services/system/system.type.ts";

export const systemSettingService = (axios: AxiosInstance) => {
    return {
        async listSettings(pageableRequest: PageableRequest, options: RawAxiosRequestConfig = {}): Promise<HttpResponseBody<SettingVo[]>> {
            const mergedOptions = {...options};
            if (!mergedOptions.params) {
                mergedOptions.params = {};
            }
            mergedOptions.params.page = pageableRequest.page;
            mergedOptions.params.size = pageableRequest.size;
            const response = await axios.get<HttpResponseBody<SettingVo[]>>(
                '/api/v1/admin/system/settings', mergedOptions
            );
            return response.data;
        },
        async setSetting(key: string, value: string | null, options: RawAxiosRequestConfig = {}): Promise<void> {
            const mergedOptions = {...options};
            const path = `/api/v1/admin/system/settings/{key}`
                .replace(`{key}`, encodeURIComponent(String(key)));
            await axios.put(
                path,
                {
                    value: value
                },
                mergedOptions
            );
        },
        async deleteSetting(key: string, options: RawAxiosRequestConfig = {}): Promise<void> {
            const mergedOptions = {...options};
            const path = `/api/v1/admin/system/settings/{key}`
                .replace(`{key}`, encodeURIComponent(String(key)));
            await axios.delete(
                path,
                mergedOptions
            );
        },
        async getSetting(key: string, options: RawAxiosRequestConfig = {}): Promise<HttpResponseBody<SettingDetailsVo>> {
            const mergedOptions = {...options};
            const path = `/api/v1/admin/system/settings/{key}`
                .replace(`{key}`, encodeURIComponent(String(key)));
            const response = await axios.get<HttpResponseBody<SettingDetailsVo>>(
                path, mergedOptions
            );
            return response.data;
        }
    }
}
