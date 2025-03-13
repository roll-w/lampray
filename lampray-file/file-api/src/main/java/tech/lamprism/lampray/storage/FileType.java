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

package tech.lamprism.lampray.storage;

/**
 * @author RollW
 */
public enum FileType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    TEXT,
    COMPRESSED,
    OTHER,
    ;

    private static final String[] DOCUMENT_TYPES = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
    };

    private static final String[] COMPRESSED_TYPES = {
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/x-gzip",
            "application/x-bzip2",
    };

    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return OTHER;
        }
        if (mimeType.startsWith("image")) {
            return IMAGE;
        }
        if (mimeType.startsWith("video")) {
            return VIDEO;
        }
        if (mimeType.startsWith("audio")) {
            return AUDIO;
        }
        if (mimeType.startsWith("text")) {
            return TEXT;
        }
        for (String type : DOCUMENT_TYPES) {
            if (type.equals(mimeType)) {
                return DOCUMENT;
            }
        }
        for (String type : COMPRESSED_TYPES) {
            if (type.equals(mimeType)) {
                return COMPRESSED;
            }
        }
        return OTHER;
    }

    public static FileType from(String nameIgnoreCase) {
        for (FileType value : values()) {
            if (value.name().equalsIgnoreCase(nameIgnoreCase)) {
                return value;
            }
        }
        return null;
    }
}
