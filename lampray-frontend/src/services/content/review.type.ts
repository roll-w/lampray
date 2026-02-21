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

import type {ContentType} from "./content.type";
import type {ContentLocationRange, StructuralText} from "@/components/structuraltext/types.ts";

export enum ReviewStatus {
    PENDING = "PENDING",
    APPROVED = "APPROVED",
    REJECTED = "REJECTED",
    CANCELED = "CANCELED"
}

export enum ReviewVerdict {
    PENDING = "PENDING",
    NEEDS_REVISION = "NEEDS_REVISION",
    REJECTED = "REJECTED",
    APPROVED = "APPROVED"
}

export enum ReviewCategory {
    POLICY_VIOLATION = "POLICY_VIOLATION",
    SENSITIVE_CONTENT = "SENSITIVE_CONTENT",
    COPYRIGHT = "COPYRIGHT",
    OTHER = "OTHER"
}

export enum ReviewSeverity {
    CRITICAL = "CRITICAL",
    MAJOR = "MAJOR",
    MINOR = "MINOR",
    INFO = "INFO"
}

export enum ReviewTaskStatus {
    PENDING = "PENDING",
    APPROVED = "APPROVED",
    REJECTED = "REJECTED",
    RETURNED = "RETURNED",
    CANCELED = "CANCELED"
}

export enum ReviewMark {
    NORMAL = "NORMAL",
    UPDATE = "UPDATE",
    REPORT = "REPORT"
}



export interface ReviewerSource {
    isAutomatic: boolean;
    reviewerName: string;
}

export interface ReviewFeedbackEntry {
    category: ReviewCategory;
    severity: ReviewSeverity;
    message: string;
    locationRange?: ContentLocationRange;
    suggestion?: string;
    reviewerSource: ReviewerSource;
}

export interface ReviewFeedback {
    verdict: ReviewVerdict;
    entries: ReviewFeedbackEntry[];
    summary?: string;
}

export interface ReviewTaskView {
    taskId: string;
    reviewJobId: string;
    status: ReviewTaskStatus;
    reviewerId: number;
    feedback?: ReviewFeedback;
    createTime: string;
    updateTime: string;
}

export interface ReviewJobView {
    id: number | string;
    status: ReviewStatus;
    contentType: ContentType;
    contentId: number | string;
    reviewMark: ReviewMark;
    createTime: string;
    updateTime: string;
}

export interface ReviewJobDetailsView {
    id: number | string;
    status: ReviewStatus;
    contentType: ContentType;
    contentId: number | string;
    reviewMark: ReviewMark;
    createTime: string;
    updateTime: string;
    tasks: ReviewTaskView[];
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
    verdict: ReviewVerdict;
    entries: ReviewFeedbackEntry[];
    summary?: string;
}
