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
import type {ReviewJobContentView, ReviewJobDetailsView, ReviewJobView, ReviewRequest, ReviewStatus} from "@/services/content/review.type.ts";

export const reviewService = (axios: AxiosInstance) => {
    return {
        /**
         * Get review jobs for current user
         */
        async getReviewJobs(
            statuses: ReviewStatus[] = [ReviewStatus.PENDING],
            options: RawAxiosRequestConfig = {}
        ): Promise<AxiosResponse<HttpResponseBody<ReviewJobView[]>>> {
            const mergedOptions = {...options};
            return await axios.get<HttpResponseBody<ReviewJobView[]>>(
                "/api/v1/reviews",
                {
                    ...mergedOptions,
                    params: {
                        statues: statuses,
                        ...mergedOptions.params
                    }
                }
            );
        },

        /**
         * Get specific review job by ID
         */
        async getReviewJob(
            jobId: number | string,
            options: RawAxiosRequestConfig = {}
        ): Promise<AxiosResponse<HttpResponseBody<ReviewJobDetailsView>>> {
            return await axios.get<HttpResponseBody<ReviewJobDetailsView>>(
                `/api/v1/reviews/${jobId}`,
                options
            );
        },

        /**
         * Get review job content
         */
        async getReviewContent(
            jobId: number | string,
            options: RawAxiosRequestConfig = {}
        ): Promise<AxiosResponse<HttpResponseBody<ReviewJobContentView>>> {
            return await axios.get<HttpResponseBody<ReviewJobContentView>>(
                `/api/v1/reviews/${jobId}/content`,
                options
            );
        },

        /**
         * Submit review decision
         */
        async makeReview(
            jobId: number | string,
            taskId: number | string,
            request: ReviewRequest,
            options: RawAxiosRequestConfig = {}
        ): Promise<AxiosResponse<HttpResponseBody<ReviewJobView>>> {
            const mergedOptions = {...options};
            if (mergedOptions.headers) {
                mergedOptions.headers["Content-Type"] = "application/json";
            } else {
                mergedOptions.headers = {
                    "Content-Type": "application/json"
                };
            }
            return await axios.post<HttpResponseBody<ReviewJobView>>(
                `/api/v1/reviews/${jobId}/tasks/${taskId}/review`,
                request,
                mergedOptions
            );
        }
    };
};

