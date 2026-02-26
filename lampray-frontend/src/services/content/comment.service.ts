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
import type {CommentRequest, CommentView} from "@/services/content/comment.type.ts";

export const commentService = (axios: AxiosInstance) => {
    return {
        async getArticleComments(articleId: number | string,
                                 options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<CommentView[]>>> {
            const path = `/api/v1/articles/{articleId}/comments`
                .replace(`{articleId}`, encodeURIComponent(String(articleId)));
            return await axios.get<HttpResponseBody<CommentView[]>>(
                path,
                options
            );
        },

        async createArticleComment(articleId: number | string,
                                   request: CommentRequest,
                                   options: RawAxiosRequestConfig = {}): Promise<AxiosResponse<HttpResponseBody<CommentView>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers["Content-Type"] = "application/json";
            } else {
                mergedOptions.headers = {
                    "Content-Type": "application/json"
                };
            }
            const path = `/api/v1/articles/{articleId}/comments`
                .replace(`{articleId}`, encodeURIComponent(String(articleId)));
            return await axios.post<HttpResponseBody<CommentView>>(
                path,
                request,
                mergedOptions
            );
        },
    }
}
