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

import type {ContentType} from "./content.type";
import type {StructuralText} from "@/components/structuraltext/types.ts";

export enum ReviewStatuses {
    FINISHED = "FINISHED",
    PASSED = "PASSED",
    REVIEWED = "REVIEWED",
    UNFINISHED = "UNFINISHED",
    REJECTED = "REJECTED",
    CANCELED = "CANCELED",
    ALL = "ALL",
}


export enum ReviewStatus {
    NOT_REVIEWED = "NOT_REVIEWED",
    REVIEWED = "REVIEWED",
    REJECTED = "REJECTED",
    CANCELED = "CANCELED"
}

export interface ReviewJobView {
    id: number | string;
    contentId: number | string;
    contentType: ContentType;
    reviewer: number;
    status: ReviewStatus;
    assignedTime: string;
    reviewTime: string;
}

export interface ReviewJobContentView {
    contentId: number | string;
    contentType: ContentType;
    title?: string;
    content: StructuralText;
    userId: number;
    createTime: string;
    updateTime: string;
}

export interface ReviewRequest {
    pass: boolean;
    reason: string;
}

