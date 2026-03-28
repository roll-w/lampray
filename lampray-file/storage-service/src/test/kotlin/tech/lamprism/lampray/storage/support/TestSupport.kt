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

package tech.lamprism.lampray.storage.support

import tech.lamprism.lampray.setting.ConfigPath
import tech.lamprism.lampray.setting.ConfigReader
import tech.lamprism.lampray.setting.ConfigValue
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecification
import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageDownloadSource
import tech.lamprism.lampray.storage.store.BlobObject
import tech.lamprism.lampray.storage.store.BlobStore
import tech.lamprism.lampray.storage.store.BlobStoreCapability
import tech.lamprism.lampray.storage.store.BlobWriteRequest
import java.io.IOException
import java.io.InputStream
import java.time.OffsetDateTime

/**
 * @author RollW
 */
class MapConfigReader(
    private val values: Map<String, Any?> = emptyMap(),
) : ConfigReader {
    override val metadata: ConfigReader.Metadata = ConfigReader.Metadata("test", ConfigPath("test", SettingSource.LOCAL))

    override fun get(key: String): String? = values[key] as? String

    override fun get(key: String, defaultValue: String?): String? = values[key] as? String ?: defaultValue

    @Suppress("UNCHECKED_CAST")
    override fun <T, V> get(specification: SettingSpecification<T, V>): T? =
        values[specification.key.name] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T, V> get(specification: SettingSpecification<T, V>, defaultValue: T): T =
        values[specification.key.name] as T? ?: defaultValue

    override fun <T, V> getValue(specification: SettingSpecification<T, V>): ConfigValue<T, V> {
        val resolvedValue = get(specification)
        val targetSpecification = specification
        return object : ConfigValue<T, V> {
            override val value: T? = resolvedValue
            override val source: SettingSource = if (resolvedValue == null) SettingSource.NONE else SettingSource.LOCAL
            override val specification: SettingSpecification<T, V> = targetSpecification
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun list(specifications: List<SettingSpecification<*, *>>): List<ConfigValue<*, *>> =
        specifications.map { getValue(it as SettingSpecification<Any?, Any?>) }
}

/**
 * @author RollW
 */
class TestBlobStore(
    private val backendName: String,
    private val capabilities: Set<BlobStoreCapability> = emptySet(),
) : BlobStore {
    override fun getBackendName(): String = backendName

    override fun getCapabilities(): Set<BlobStoreCapability> = capabilities

    override fun store(request: BlobWriteRequest, inputStream: InputStream): BlobObject {
        throw UnsupportedOperationException("Not needed in test")
    }

    override fun openDownload(key: String): StorageDownloadSource {
        throw UnsupportedOperationException("Not needed in test")
    }

    override fun describe(key: String): BlobObject {
        throw UnsupportedOperationException("Not needed in test")
    }

    override fun exists(key: String): Boolean = false

    override fun delete(key: String): Boolean = false
}

fun testFileStorage(fileSize: Long): FileStorage = FileStorage(
    fileId = "file-1",
    fileName = "demo.bin",
    fileSize = fileSize,
    mimeType = "application/octet-stream",
    fileType = FileType.OTHER,
    createTime = OffsetDateTime.now(),
)
