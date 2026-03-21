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

import tech.lamprism.lampray.storage.StorageBackendType
import tech.lamprism.lampray.storage.store.BlobStoreCapability

data class StorageBackendView(
    val backendName: String,
    val backendType: StorageBackendType?,
    val active: Boolean,
    val capabilities: Set<BlobStoreCapability>,
    val groupWeights: Map<String, Int>,
    val endpoint: String?,
    val publicEndpoint: String?,
    val region: String?,
    val bucket: String?,
    val rootPrefix: String?,
    val rootPath: String?,
    val nativeChecksumEnabled: Boolean,
    val pathStyleAccess: Boolean,
    val primaryBlobCount: Long,
    val placementCount: Long,
    val uniqueBytes: Long,
    val physicalBytes: Long,
) {
    fun backendName(): String = backendName

    fun backendType(): StorageBackendType? = backendType

    fun active(): Boolean = active

    fun capabilities(): Set<BlobStoreCapability> = capabilities

    fun groupWeights(): Map<String, Int> = groupWeights

    fun endpoint(): String? = endpoint

    fun publicEndpoint(): String? = publicEndpoint

    fun region(): String? = region

    fun bucket(): String? = bucket

    fun rootPrefix(): String? = rootPrefix

    fun rootPath(): String? = rootPath

    fun nativeChecksumEnabled(): Boolean = nativeChecksumEnabled

    fun pathStyleAccess(): Boolean = pathStyleAccess

    fun primaryBlobCount(): Long = primaryBlobCount

    fun placementCount(): Long = placementCount

    fun uniqueBytes(): Long = uniqueBytes

    fun physicalBytes(): Long = physicalBytes
}
