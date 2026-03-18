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

package tech.lamprism.lampray.storage.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import tech.lamprism.lampray.storage.StorageBackendType
import tech.lamprism.lampray.storage.StorageVisibility
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend
import tech.lamprism.lampray.storage.configuration.StorageGroupDownloadPolicy
import tech.lamprism.lampray.storage.configuration.StorageGroupLoadBalanceMode
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.configuration.StorageTopology
import tech.lamprism.lampray.storage.store.BlobStoreRegistration
import tech.lamprism.lampray.storage.store.DynamicBlobStoreRegistry
import tech.lamprism.lampray.storage.support.TestBlobStore

/**
 * @author RollW
 */
class TopologyStorageGroupRouterTest {
    @Test
    fun `weighted round robin includes dynamic backend bindings`() {
        val topology = StorageTopology(
            defaultGroup = "upload",
            backends = mapOf(
                "a" to localBackend("a"),
                "b" to localBackend("b"),
            ),
            groups = mapOf(
                "upload" to StorageGroupConfig(
                    name = "upload",
                    backends = listOf(StorageGroupBackend("a"), StorageGroupBackend("b")),
                    visibility = StorageVisibility.PRIVATE,
                    downloadPolicy = StorageGroupDownloadPolicy.HYBRID,
                    placementMode = StorageGroupPlacementMode.MIRROR,
                    loadBalanceMode = StorageGroupLoadBalanceMode.WEIGHTED_ROUND_ROBIN,
                    maxSizeBytes = null,
                    allowedFileTypes = emptySet(),
                )
            )
        )
        val registry = DynamicBlobStoreRegistry(
            listOf(
                BlobStoreRegistration(TestBlobStore("a"), emptyMap()),
                BlobStoreRegistration(TestBlobStore("b"), emptyMap()),
                BlobStoreRegistration(TestBlobStore("c"), mapOf("upload" to 3)),
            )
        )

        val router = TopologyStorageGroupRouter(topology, registry)

        val sequence = (1..5).map { router.selectWritePlan("upload").primaryBackend() }

        assertEquals(listOf("a", "b", "c", "c", "c"), sequence)
        assertEquals(listOf("b", "c"), router.selectWritePlan("upload").mirrorBackends())
    }

    @Test
    fun `ordered fallback prefers first configured backend`() {
        val topology = StorageTopology(
            defaultGroup = "download",
            backends = mapOf("a" to localBackend("a"), "b" to localBackend("b")),
            groups = mapOf(
                "download" to StorageGroupConfig(
                    name = "download",
                    backends = listOf(StorageGroupBackend("a"), StorageGroupBackend("b")),
                    visibility = StorageVisibility.PUBLIC,
                    downloadPolicy = StorageGroupDownloadPolicy.HYBRID,
                    placementMode = StorageGroupPlacementMode.SINGLE,
                    loadBalanceMode = StorageGroupLoadBalanceMode.ORDERED,
                    maxSizeBytes = null,
                    allowedFileTypes = emptySet(),
                )
            )
        )
        val registry = DynamicBlobStoreRegistry(
            listOf(
                BlobStoreRegistration(TestBlobStore("a"), emptyMap()),
                BlobStoreRegistration(TestBlobStore("b"), emptyMap()),
            )
        )

        val router = TopologyStorageGroupRouter(topology, registry)

        assertEquals("a", router.selectWritePlan("download").primaryBackend())
        assertEquals("a", router.selectReadBackend("download", setOf("a", "b")))
    }

    @Test
    fun `dynamic only group can route without static backends`() {
        val topology = StorageTopology(
            defaultGroup = "dynamic",
            backends = emptyMap(),
            groups = mapOf(
                "dynamic" to StorageGroupConfig(
                    name = "dynamic",
                    backends = emptyList(),
                    visibility = StorageVisibility.PRIVATE,
                    downloadPolicy = StorageGroupDownloadPolicy.HYBRID,
                    placementMode = StorageGroupPlacementMode.SINGLE,
                    loadBalanceMode = StorageGroupLoadBalanceMode.ORDERED,
                    maxSizeBytes = null,
                    allowedFileTypes = emptySet(),
                )
            )
        )
        val registry = DynamicBlobStoreRegistry(
            listOf(
                BlobStoreRegistration(TestBlobStore("dyn-a"), mapOf("dynamic" to 1)),
            )
        )

        val router = TopologyStorageGroupRouter(topology, registry)

        assertEquals("dyn-a", router.selectWritePlan("dynamic").primaryBackend())
    }

    @Test
    fun `read routing rejects placements without active backends`() {
        val topology = StorageTopology(
            defaultGroup = "download",
            backends = emptyMap(),
            groups = mapOf(
                "download" to StorageGroupConfig(
                    name = "download",
                    backends = emptyList(),
                    visibility = StorageVisibility.PUBLIC,
                    downloadPolicy = StorageGroupDownloadPolicy.HYBRID,
                    placementMode = StorageGroupPlacementMode.SINGLE,
                    loadBalanceMode = StorageGroupLoadBalanceMode.ORDERED,
                    maxSizeBytes = null,
                    allowedFileTypes = emptySet(),
                )
            )
        )
        val router = TopologyStorageGroupRouter(topology, DynamicBlobStoreRegistry(emptyList()))

        kotlin.test.assertFailsWith<IllegalStateException> {
            router.selectReadBackend("download", setOf("missing-backend"))
        }
    }

    private fun localBackend(name: String) = StorageBackendConfig(
        name = name,
        type = StorageBackendType.LOCAL,
        endpoint = null,
        publicEndpoint = null,
        nativeChecksumEnabled = false,
        region = null,
        bucket = null,
        rootPrefix = "blob",
        pathStyleAccess = false,
        accessKey = null,
        secretKey = null,
        rootPath = "/tmp/$name",
    )
}
