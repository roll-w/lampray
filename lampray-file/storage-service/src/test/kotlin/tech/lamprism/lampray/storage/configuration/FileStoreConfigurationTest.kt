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

import tech.lamprism.lampray.storage.StorageBackendType
import tech.lamprism.lampray.storage.monitoring.StorageTrafficRecorder
import tech.lamprism.lampray.storage.store.BlobStore
import tech.lamprism.lampray.storage.store.BlobStoreFactory
import tech.lamprism.lampray.storage.support.TestBlobStore
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * @author RollW
 */
class FileStoreConfigurationTest {
    @Test
    fun `duplicate factory type fails fast`() {
        val configuration = FileStoreConfiguration()
        val topology = StorageTopology(
            defaultGroup = "upload",
            backends = mapOf(
                "local-a" to StorageBackendConfig(
                    name = "local-a",
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
                    rootPath = "/tmp/a",
                )
            ),
            groups = emptyMap(),
        )

        assertFailsWith<IllegalStateException> {
            configuration.blobStoreFactoryProvider(
                listOf(
                    TestFactory("first"),
                    TestFactory("second"),
                )
            )
        }
    }

    @Test
    fun `missing factory for backend type fails fast`() {
        val configuration = FileStoreConfiguration()
        val topology = StorageTopology(
            defaultGroup = "upload",
            backends = mapOf(
                "local-a" to StorageBackendConfig(
                    name = "local-a",
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
                    rootPath = "/tmp/a",
                )
            ),
            groups = emptyMap(),
        )

        assertFailsWith<IllegalArgumentException> {
            configuration.blobStoreRegistry(
                topology,
                configuration.blobStoreFactoryProvider(emptyList()),
                StorageTrafficRecorder(),
            )
        }
    }

    private class TestFactory(
        private val backendName: String,
    ) : BlobStoreFactory {
        override fun getBackendType(): StorageBackendType = StorageBackendType.LOCAL

        override fun create(config: StorageBackendConfig): BlobStore = TestBlobStore(backendName)
    }
}
