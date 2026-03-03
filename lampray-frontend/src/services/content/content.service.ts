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

import type {AxiosInstance, AxiosResponse, RawAxiosRequestConfig} from "axios";
import type {HttpResponseBody} from "@/services/common.type.ts";
import type {UrlContentType} from "@/services/content/content.type.ts";

export const contentService = (axios: AxiosInstance) => {
    return {
        async getContents<T>(contentType: UrlContentType,
                             options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<T[]>>> {
            const path = `/api/v1/{contentType}`
                .replace(`{contentType}`, encodeURIComponent(contentType));
            return await axios.get<HttpResponseBody<T[]>>(
                path,
                options
            );
        },

        async getUserContents<T>(userId: number | string,
                                 contentType: UrlContentType,
                                 options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<T[]>>> {
            const path = `/api/v1/users/{userId}/{contentType}`
                .replace(`{userId}`, encodeURIComponent(String(userId)))
                .replace(`{contentType}`, encodeURIComponent(contentType));
            return await axios.get<HttpResponseBody<T[]>>(
                path,
                options
            );
        },

        async getUserContent<T>(userId: number | string,
                                contentType: UrlContentType,
                                contentId: number | string,
                                options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<T>>> {
            const path = `/api/v1/users/{userId}/{contentType}/{contentId}`
                .replace(`{userId}`, encodeURIComponent(String(userId)))
                .replace(`{contentType}`, encodeURIComponent(contentType))
                .replace(`{contentId}`, encodeURIComponent(String(contentId)));
            return await axios.get<HttpResponseBody<T>>(
                path,
                options
            );
        },
    }
}
