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

import type {StructuralText} from "@/components/structuraltext/types.ts";

export const ContentAccessAuthType = {
    PUBLIC: "PUBLIC",
    PASSWORD: "PASSWORD",
    PRIVATE: "PRIVATE",
    USER: "USER",
    USER_GROUP: "USER_GROUP",
} as const;

export type ContentAccessAuthType = typeof ContentAccessAuthType[keyof typeof ContentAccessAuthType];

export interface ArticleCreateRequest {
    title: string;
    content: StructuralText;
}

export interface ArticleInfoView {
    id: number;
    userId: number;
    title: string;
    cover?: string;
    createTime: string;
    updateTime: string;
    accessAuthType: ContentAccessAuthType;
}

export interface ArticleDetailsView {
    id: number;
    title: string;
    content: StructuralText;
    userId: number;
    createTime: string;
    updateTime: string;
    accessAuthType: ContentAccessAuthType;
}
