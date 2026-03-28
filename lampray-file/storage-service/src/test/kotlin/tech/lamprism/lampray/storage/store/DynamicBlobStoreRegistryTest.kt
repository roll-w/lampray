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

package tech.lamprism.lampray.storage.store

import tech.lamprism.lampray.storage.backend.BlobStoreRegistration
import tech.lamprism.lampray.storage.backend.DynamicBlobStoreRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import tech.lamprism.lampray.storage.support.TestBlobStore

/**
 * @author RollW
 */
class DynamicBlobStoreRegistryTest {
    @Test
    fun `register and unregister backend`() {
        val initial = BlobStoreRegistration(TestBlobStore("local-a"), emptyMap())
        val registry = DynamicBlobStoreRegistry(listOf(initial))

        assertTrue(registry.contains("local-a"))
        assertEquals("local-a", registry.get("local-a").backendName)

        registry.register(BlobStoreRegistration(TestBlobStore("local-b"), mapOf("upload" to 3)))

        val registration = registry.registrations().firstOrNull { it.backendName == "local-b" }
        assertNotNull(registration)
        assertEquals(3, registration.groupWeights["upload"])

        val removed = registry.unregister("local-b")
        assertTrue(removed.isPresent)
        assertFalse(registry.contains("local-b"))
    }

    @Test
    fun `reject duplicate backend registration`() {
        val registry = DynamicBlobStoreRegistry(listOf(BlobStoreRegistration(TestBlobStore("local-a"), emptyMap())))

        assertFailsWith<IllegalArgumentException> {
            registry.register(BlobStoreRegistration(TestBlobStore("local-a"), emptyMap()))
        }
    }

    @Test
    fun `unregistered backend remains accessible for in flight work`() {
        val registry = DynamicBlobStoreRegistry(
            listOf(BlobStoreRegistration(TestBlobStore("local-a"), mapOf("upload" to 1)))
        )

        registry.unregister("local-a")

        assertFalse(registry.contains("local-a"))
        assertEquals("local-a", registry.get("local-a").backendName)
        assertTrue(registry.find("local-a").isPresent)
        assertTrue(registry.registrations().isEmpty())
    }

    @Test
    fun `retired backend name cannot be reused immediately`() {
        val registry = DynamicBlobStoreRegistry(
            listOf(BlobStoreRegistration(TestBlobStore("local-a"), emptyMap()))
        )

        registry.unregister("local-a")

        assertFailsWith<IllegalStateException> {
            registry.register(BlobStoreRegistration(TestBlobStore("local-a"), emptyMap()))
        }
    }
}
