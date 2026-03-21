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

package tech.lamprism.lampray.storage.service

import tech.lamprism.lampray.storage.StorageDownloadMode
import tech.lamprism.lampray.storage.StorageUploadMode
import tech.lamprism.lampray.storage.StorageUploadRequest
import tech.lamprism.lampray.storage.StorageVisibility
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.configuration.StorageGroupDownloadPolicy
import tech.lamprism.lampray.storage.configuration.StorageGroupLoadBalanceMode
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig
import tech.lamprism.lampray.storage.policy.DefaultStorageTransferPolicy
import tech.lamprism.lampray.storage.store.BlobStoreCapability
import tech.lamprism.lampray.storage.support.MapConfigReader
import tech.lamprism.lampray.storage.support.TestBlobStore
import tech.lamprism.lampray.storage.support.testFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author RollW
 */
class DefaultStorageTransferPolicyTest {
    @Test
    fun `large upload with checksum uses direct mode when backend supports it`() {
        val resolver = DefaultStorageTransferPolicy(runtimeSettings())
        val request = StorageUploadRequest(
            groupName = "upload",
            fileName = "video.mp4",
            size = 32L * 1024L * 1024L,
            mimeType = "video/mp4",
            checksumSha256 = "abc",
        )

        val mode = resolver.resolveUploadMode(
            request,
            "f".repeat(64),
            TestBlobStore("s3", setOf(BlobStoreCapability.DIRECT_UPLOAD)),
        )

        assertEquals(StorageUploadMode.DIRECT, mode)
    }

    @Test
    fun `small or unsupported upload falls back to proxy`() {
        val resolver = DefaultStorageTransferPolicy(runtimeSettings())
        val request = StorageUploadRequest(
            groupName = "upload",
            fileName = "avatar.png",
            size = 1024,
            mimeType = "image/png",
            checksumSha256 = null,
        )

        val mode = resolver.resolveUploadMode(
            request,
            null,
            TestBlobStore("local"),
        )

        assertEquals(StorageUploadMode.PROXY, mode)
    }

    @Test
    fun `download policy respects capability and threshold`() {
        val resolver = DefaultStorageTransferPolicy(runtimeSettings())
        val hybridGroup = StorageGroupConfig(
            name = "download",
            backends = emptyList(),
            visibility = StorageVisibility.PUBLIC,
            downloadPolicy = StorageGroupDownloadPolicy.HYBRID,
            placementMode = StorageGroupPlacementMode.SINGLE,
            loadBalanceMode = StorageGroupLoadBalanceMode.ORDERED,
            maxSizeBytes = null,
            allowedFileTypes = emptySet(),
        )

        val smallMode = resolver.resolveDownloadMode(
            testFileStorage(1024),
            hybridGroup,
            TestBlobStore("s3", setOf(BlobStoreCapability.DIRECT_DOWNLOAD)),
        )
        val largeMode = resolver.resolveDownloadMode(
            testFileStorage(64L * 1024L * 1024L),
            hybridGroup,
            TestBlobStore("s3", setOf(BlobStoreCapability.DIRECT_DOWNLOAD)),
        )

        assertEquals(StorageDownloadMode.PROXY, smallMode)
        assertEquals(StorageDownloadMode.DIRECT, largeMode)
    }

    private fun runtimeSettings(): StorageRuntimeConfig = StorageRuntimeConfig(
        MapConfigReader(
            mapOf(
                "storage.presign.enabled" to true,
                "storage.upload.proxy-threshold-bytes" to 8L * 1024L * 1024L,
                "storage.download.proxy-threshold-bytes" to 16L * 1024L * 1024L,
            )
        )
    )
}
