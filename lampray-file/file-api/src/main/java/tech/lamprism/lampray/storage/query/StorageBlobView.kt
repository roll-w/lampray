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

import java.time.OffsetDateTime

data class StorageBlobView(
    val blobId: String,
    val checksumSha256: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    val primaryBackend: String,
    val primaryObjectKey: String,
    val createTime: OffsetDateTime,
    val updateTime: OffsetDateTime,
    val placements: List<StorageBlobPlacementView>,
) {
    fun blobId(): String = blobId

    fun checksumSha256(): String = checksumSha256

    fun fileSize(): Long = fileSize

    fun mimeType(): String = mimeType

    fun fileType(): FileType = fileType

    fun primaryBackend(): String = primaryBackend

    fun primaryObjectKey(): String = primaryObjectKey

    fun createTime(): OffsetDateTime = createTime

    fun updateTime(): OffsetDateTime = updateTime

    fun placements(): List<StorageBlobPlacementView> = placements
}
