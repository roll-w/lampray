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

package tech.lamprism.lampray.storage.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import tech.lamprism.lampray.storage.StorageVisibility
import tech.lamprism.lampray.storage.support.MapConfigReader

/**
 * @author RollW
 */
class StorageTopologyResolverTest {
    @Test
    fun `resolve weighted backends into group topology`() {
        val configReader = MapConfigReader(
            mapOf(
                "storage.backends" to setOf("local-a", "local-b"),
                "storage.groups" to setOf("upload"),
                "storage.default-group" to "upload",
                "storage.backend.local-a.type" to "LOCAL",
                "storage.backend.local-a.root-path" to "/tmp/a",
                "storage.backend.local-b.type" to "LOCAL",
                "storage.backend.local-b.root-path" to "/tmp/b",
                "storage.group.upload.backends" to "[local-a:2, local-b]",
                "storage.group.upload.visibility" to "PUBLIC",
                "storage.group.upload.download-mode" to "DIRECT",
                "storage.group.upload.placement-mode" to "MIRROR",
                "storage.group.upload.load-balance" to "WEIGHTED_ROUND_ROBIN",
            )
        )

        val topology = StorageTopologyResolver(configReader, StorageRuntimeConfig(configReader)).resolve()
        val group = topology.getGroup("upload")

        assertEquals("upload", topology.defaultGroup)
        assertEquals(StorageVisibility.PUBLIC, group.visibility)
        assertEquals(StorageGroupDownloadPolicy.DIRECT, group.downloadPolicy)
        assertEquals(StorageGroupPlacementMode.MIRROR, group.placementMode)
        assertEquals(StorageGroupLoadBalanceMode.WEIGHTED_ROUND_ROBIN, group.loadBalanceMode)
        assertEquals(listOf(StorageGroupBackend("local-a", 2), StorageGroupBackend("local-b", 1)), group.backends)
    }

    @Test
    fun `explicit empty backend list keeps group dynamic only`() {
        val configReader = MapConfigReader(
            mapOf(
                "storage.backends" to emptySet<String>(),
                "storage.groups" to setOf("dynamic"),
                "storage.default-group" to "dynamic",
                "storage.group.dynamic.backends" to "[]",
            )
        )

        val topology = StorageTopologyResolver(configReader, StorageRuntimeConfig(configReader)).resolve()

        assertEquals(emptyList(), topology.getGroup("dynamic").backends)
    }

    @Test
    fun `resolve s3 public endpoint into backend config`() {
        val configReader = MapConfigReader(
            mapOf(
                "storage.backends" to setOf("s3-a"),
                "storage.default-group" to "default",
                "storage.backend.s3-a.type" to "S3",
                "storage.backend.s3-a.endpoint" to "https://internal-s3.example.com",
                "storage.backend.s3-a.public-endpoint" to "https://cdn.example.com/public",
                "storage.backend.s3-a.region" to "ap-shanghai",
                "storage.backend.s3-a.bucket" to "lampray-assets",
            )
        )

        val topology = StorageTopologyResolver(configReader, StorageRuntimeConfig(configReader)).resolve()

        assertEquals("https://cdn.example.com/public", topology.getBackend("s3-a").publicEndpoint)
    }

    @Test
    fun `blank s3 endpoint keeps native checksum enabled by default`() {
        val configReader = MapConfigReader(
            mapOf(
                "storage.backends" to setOf("s3-a"),
                "storage.default-group" to "default",
                "storage.backend.s3-a.type" to "S3",
                "storage.backend.s3-a.endpoint" to "   ",
                "storage.backend.s3-a.bucket" to "lampray-assets",
            )
        )

        val topology = StorageTopologyResolver(configReader, StorageRuntimeConfig(configReader)).resolve()
        val backend = topology.getBackend("s3-a")

        assertEquals(null, backend.endpoint)
        assertEquals(true, backend.nativeChecksumEnabled)
    }
}
