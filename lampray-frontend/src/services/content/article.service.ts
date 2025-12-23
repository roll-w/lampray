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
import type {ArticleCreateRequest, ArticleInfoView} from "@/services/content/article.type.ts";

export const articleService = (axios: AxiosInstance) => {
    return {
        async createArticle(request: ArticleCreateRequest, options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<ArticleInfoView>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers["Content-Type"] = "application/json";
            } else {
                mergedOptions.headers = {
                    "Content-Type": "application/json"
                };
            }
            return await axios.post<HttpResponseBody<ArticleInfoView>>(
                "/api/v1/articles", request, mergedOptions
            );
        },
    }
}
