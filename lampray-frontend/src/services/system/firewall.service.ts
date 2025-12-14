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

import type {AxiosInstance, AxiosResponse, RawAxiosRequestConfig} from "axios";
import type {HttpResponseBody} from "@/services/common.type.ts";
import type {
    AddFilterEntryRequest,
    FilterEntryVo,
    FirewallInfo,
    UpdateFilterEntryRequest
} from "@/services/system/firewall.type.ts";

export const firewallService = (axios: AxiosInstance) => {
    return {
        async listFirewalls(options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<FirewallInfo[]>>> {
            const mergedOptions = {...options};
            return await axios.get<HttpResponseBody<FirewallInfo[]>>(
                "/api/v1/admin/firewalls", mergedOptions
            );
        },

        async getFilterTable(options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<FilterEntryVo[]>>> {
            const mergedOptions = {...options};
            return await axios.get<HttpResponseBody<FilterEntryVo[]>>(
                "/api/v1/admin/firewalls/filter-table", mergedOptions
            );
        },

        async addFilterEntry(request: AddFilterEntryRequest, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers["Content-Type"] = "application/json";
            } else {
                mergedOptions.headers = {
                    "Content-Type": "application/json"
                };
            }
            return await axios.patch<HttpResponseBody<void>>(
                "/api/v1/admin/firewalls/filter-table", request, mergedOptions
            );
        },

        async updateFilterEntry(request: UpdateFilterEntryRequest, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers["Content-Type"] = "application/json";
            } else {
                mergedOptions.headers = {
                    "Content-Type": "application/json"
                };
            }
            return await axios.put<HttpResponseBody<void>>(
                "/api/v1/admin/firewalls/filter-table", request, mergedOptions
            );
        },

        async removeFilterEntry(identifier: string, type: string, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            if (!mergedOptions.params) {
                mergedOptions.params = {};
            }
            mergedOptions.params.identifier = identifier;
            mergedOptions.params.type = type;
            return await axios.delete<HttpResponseBody<void>>(
                "/api/v1/admin/firewalls/filter-table", mergedOptions
            );
        },

        async clearFilterTable(options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<void>>> {
            const mergedOptions = {...options};
            return await axios.post<HttpResponseBody<void>>(
                "/api/v1/admin/firewalls/filter-table/clear", null, mergedOptions
            );
        }
    }
}

