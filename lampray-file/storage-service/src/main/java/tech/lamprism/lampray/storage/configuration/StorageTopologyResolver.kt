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

import org.springframework.stereotype.Component
import tech.lamprism.lampray.setting.ConfigReader
import tech.lamprism.lampray.storage.FileType
import tech.lamprism.lampray.storage.StorageBackendType
import tech.lamprism.lampray.storage.StorageVisibility

/**
 * @author RollW
 */
@Component
class StorageTopologyResolver(
    private val configReader: ConfigReader,
    private val runtimeSettings: StorageRuntimeSettings,
) {
    fun resolve(): StorageTopology {
        val backendNames = configReader[StorageConfigKeys.STORAGE_BACKENDS, emptySet()] ?: emptySet()
        if (backendNames.isEmpty()) {
            return defaultTopology()
        }

        val backends = backendNames.associateWith(::resolveBackend)
        val configuredGroups = configReader[StorageConfigKeys.STORAGE_GROUPS, emptySet()] ?: emptySet()
        val groupNames = configuredGroups.ifEmpty { setOf(runtimeSettings.defaultGroup()) }
        val groups = groupNames.associateWith { resolveGroup(it, backends) }
        val defaultGroup = runtimeSettings.defaultGroup()
        require(groups.containsKey(defaultGroup)) { "Default storage group not configured: $defaultGroup" }
        return StorageTopology(defaultGroup, backends, groups)
    }

    private fun defaultTopology(): StorageTopology {
        val backend = StorageBackendSettings(
            name = "local-default",
            type = StorageBackendType.LOCAL,
            endpoint = null,
            region = null,
            bucket = null,
            rootPrefix = "blob",
            pathStyleAccess = false,
            accessKey = null,
            secretKey = null,
            rootPath = "temp/storage",
        )
        val group = StorageGroupSettings(
            name = runtimeSettings.defaultGroup(),
            primaryBackend = backend.name,
            replicaBackends = emptySet(),
            visibility = StorageVisibility.PRIVATE,
            downloadPolicy = StorageGroupDownloadPolicy.PROXY,
            redundancyMode = StorageGroupRedundancyMode.SINGLE,
            maxSizeBytes = null,
            allowedFileTypes = emptySet(),
        )
        return StorageTopology(group.name, mapOf(backend.name to backend), mapOf(group.name to group))
    }

    private fun resolveBackend(name: String): StorageBackendSettings {
        val prefix = "storage.backend.$name."
        val type = StorageBackendType.from(configReader[prefix + "type"])
            ?: throw IllegalArgumentException("Storage backend type is required for $name")
        return when (type) {
            StorageBackendType.LOCAL -> StorageBackendSettings(
                name = name,
                type = type,
                endpoint = null,
                region = null,
                bucket = null,
                rootPrefix = configReader[prefix + "root-prefix"] ?: "blob",
                pathStyleAccess = false,
                accessKey = null,
                secretKey = null,
                rootPath = configReader[prefix + "root-path"] ?: "temp/storage/$name",
            )

            StorageBackendType.S3 -> StorageBackendSettings(
                name = name,
                type = type,
                endpoint = configReader[prefix + "endpoint"],
                region = configReader[prefix + "region"] ?: "us-east-1",
                bucket = configReader[prefix + "bucket"]
                    ?: throw IllegalArgumentException("Storage backend bucket is required for $name"),
                rootPrefix = configReader[prefix + "root-prefix"] ?: "blob",
                pathStyleAccess = configReader[prefix + "path-style"]?.toBooleanStrictOrNull() ?: true,
                accessKey = configReader[prefix + "access-key"],
                secretKey = configReader[prefix + "secret-key"],
                rootPath = null,
            )
        }
    }

    private fun resolveGroup(
        name: String,
        backends: Map<String, StorageBackendSettings>,
    ): StorageGroupSettings {
        val prefix = "storage.group.$name."
        val primaryBackend = configReader[prefix + "primary-backend"] ?: backends.keys.firstOrNull()
        ?: throw IllegalArgumentException("No storage backend available for group $name")
        require(backends.containsKey(primaryBackend)) {
            "Group $name references unknown backend $primaryBackend"
        }
        val replicaBackends = readStringSet(prefix + "replica-backends")
            .filter { it != primaryBackend }
            .onEach {
                require(backends.containsKey(it)) { "Group $name references unknown replica backend $it" }
            }
            .toSet()
        return StorageGroupSettings(
            name = name,
            primaryBackend = primaryBackend,
            replicaBackends = replicaBackends,
            visibility = StorageVisibility.from(configReader[prefix + "visibility"]) ?: StorageVisibility.PRIVATE,
            downloadPolicy = StorageGroupDownloadPolicy.from(configReader[prefix + "download-mode"]),
            redundancyMode = StorageGroupRedundancyMode.from(configReader[prefix + "redundancy"]),
            maxSizeBytes = configReader[prefix + "max-size-bytes"]?.toLongOrNull(),
            allowedFileTypes = readStringSet(prefix + "allowed-file-types")
                .mapNotNull(FileType::from)
                .toSet(),
        )
    }

    private fun readStringSet(key: String): Set<String> {
        val raw = configReader[key] ?: return emptySet()
        return raw.removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { it.trim().trim('"', '\'') }
            .filter { it.isNotBlank() }
            .toSet()
    }
}
