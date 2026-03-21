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

package tech.lamprism.lampray.storage.materialization

import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity

data class PreparedBlobMaterialization private constructor(
    val existingBlob: StorageBlobEntity?,
    val checksum: String,
    val size: Long,
    val mimeType: String,
    val fileType: FileType,
    val primaryBackend: String,
    val primaryObjectKey: String,
    val placementsToPersist: Map<String, String>,
) {
    fun existingBlob(): StorageBlobEntity? = existingBlob

    fun checksum(): String = checksum

    fun size(): Long = size

    fun mimeType(): String = mimeType

    fun fileType(): FileType = fileType

    fun primaryBackend(): String = primaryBackend

    fun primaryObjectKey(): String = primaryObjectKey

    fun placementsToPersist(): Map<String, String> = placementsToPersist

    companion object {
        @JvmStatic
        fun existing(
            blobEntity: StorageBlobEntity,
            size: Long,
            placementsToPersist: Map<String, String>,
        ): PreparedBlobMaterialization {
            return PreparedBlobMaterialization(
                blobEntity,
                blobEntity.checksumSha256,
                size,
                blobEntity.mimeType,
                blobEntity.fileType,
                blobEntity.primaryBackend,
                blobEntity.primaryObjectKey,
                placementsToPersist.toMap(),
            )
        }

        @JvmStatic
        fun newBlob(
            checksum: String,
            size: Long,
            mimeType: String,
            fileType: FileType,
            primaryBackend: String,
            primaryObjectKey: String,
            placementsToPersist: Map<String, String>,
        ): PreparedBlobMaterialization {
            return PreparedBlobMaterialization(
                null,
                checksum,
                size,
                mimeType,
                fileType,
                primaryBackend,
                primaryObjectKey,
                placementsToPersist.toMap(),
            )
        }
    }
}
