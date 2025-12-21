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

export enum ContentType {
    ARTICLE = "ARTICLE",
    COMMENT = "COMMENT",
    POST = "POST",
    IMAGE = "IMAGE",
    USER_SPACE = "USER_SPACE",
}

/**
 * Get the i18n key for a content type
 */
export function getContentTypeI18nKey(contentType: ContentType | string): string {
    return `content.type.${contentType.toLowerCase()}`;
}

/**
 * Parse content type from string
 */
export function parseContentType(value: string): ContentType | null {
    const upperValue = value.toUpperCase();
    if (upperValue in ContentType) {
        return ContentType[upperValue as keyof typeof ContentType];
    }
    return null;
}

