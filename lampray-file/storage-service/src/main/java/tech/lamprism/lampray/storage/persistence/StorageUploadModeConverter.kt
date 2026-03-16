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

package tech.lamprism.lampray.storage.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tech.lamprism.lampray.storage.StorageUploadMode

/**
 * @author RollW
 */
@Converter
class StorageUploadModeConverter : AttributeConverter<StorageUploadMode, String> {
    override fun convertToDatabaseColumn(attribute: StorageUploadMode?): String? {
        return attribute?.name
    }

    override fun convertToEntityAttribute(dbData: String?): StorageUploadMode? {
        return when {
            dbData == null -> null
            dbData.equals("proxy", ignoreCase = true) -> StorageUploadMode.PROXY
            dbData.equals("direct", ignoreCase = true) -> StorageUploadMode.DIRECT
            dbData.equals("presigned_put", ignoreCase = true) -> StorageUploadMode.DIRECT
            else -> throw IllegalArgumentException("Unknown storage upload mode: $dbData")
        }
    }
}
