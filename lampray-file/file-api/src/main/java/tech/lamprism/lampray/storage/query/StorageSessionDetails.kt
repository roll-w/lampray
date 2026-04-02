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

package tech.lamprism.lampray.storage.query

import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageUploadMode
import tech.lamprism.lampray.storage.StorageUploadSessionState

import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class StorageSessionDetails(
    val uploadId: String,
    val fileId: String,
    val groupName: String,
    val fileName: String,
    val fileSize: Long?,
    val mimeType: String?,
    val fileType: FileType?,
    val contentChecksum: String?,
    val ownerUserId: Long?,
    val primaryBackend: String,
    val objectKey: String?,
    val mode: StorageUploadMode,
    val trackedState: StorageUploadSessionState,
    val expiresAt: OffsetDateTime,
    val createTime: OffsetDateTime,
    val updateTime: OffsetDateTime,
    val file: StorageFileView?,
)
