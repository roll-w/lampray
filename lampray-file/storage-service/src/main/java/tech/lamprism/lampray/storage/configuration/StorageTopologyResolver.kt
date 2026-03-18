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
import tech.lamprism.lampray.storage.configuration.StorageBackendConfig
import tech.lamprism.lampray.storage.configuration.StorageGroupBackend
import tech.lamprism.lampray.storage.configuration.StorageGroupDownloadPolicy
import tech.lamprism.lampray.storage.configuration.StorageGroupLoadBalanceMode
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.configuration.StorageTopology

/**
 * @author RollW
 */
@Component
class StorageTopologyResolver(
    private val configReader: ConfigReader,
    private val runtimeSettings: StorageRuntimeConfig,
) {
    fun resolve(): StorageTopology {
        val backendNames = configReader[StorageConfigKeys.STORAGE_BACKENDS, emptySet()] ?: emptySet()
        val configuredGroups = configReader[StorageConfigKeys.STORAGE_GROUPS, emptySet()] ?: emptySet()
        if (backendNames.isEmpty() && configuredGroups.isEmpty()) {
            return defaultTopology()
        }

        val backends = backendNames.associateWith(::resolveBackend)
        val groupNames = configuredGroups.ifEmpty { setOf(runtimeSettings.defaultGroup()) }
        val groups = groupNames.associateWith { resolveGroup(it, backends) }
        val defaultGroup = runtimeSettings.defaultGroup()
        require(groups.containsKey(defaultGroup)) { "Default storage group not configured: $defaultGroup" }
        return StorageTopology(defaultGroup, backends, groups)
    }

    private fun defaultTopology(): StorageTopology {
        val backend = StorageBackendConfig(
            name = "local-default",
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
            rootPath = "temp/storage",
        )
        val group = StorageGroupConfig(
            name = runtimeSettings.defaultGroup(),
            backends = listOf(StorageGroupBackend(backend.name)),
            visibility = StorageVisibility.PRIVATE,
            downloadPolicy = StorageGroupDownloadPolicy.PROXY,
            placementMode = StorageGroupPlacementMode.SINGLE,
            loadBalanceMode = StorageGroupLoadBalanceMode.ORDERED,
            maxSizeBytes = null,
            allowedFileTypes = emptySet(),
        )
        return StorageTopology(group.name, mapOf(backend.name to backend), mapOf(group.name to group))
    }

    private fun resolveBackend(name: String): StorageBackendConfig {
        // TODO(RollW): Replace manual prefix-based reads with strongly typed config binding.
        val prefix = "storage.backend.$name."
        val type = StorageBackendType.from(configReader[prefix + "type"])
            ?: throw IllegalArgumentException("Storage backend type is required for $name")
        val endpoint = configReader[prefix + "endpoint"]?.trim()?.takeIf { it.isNotEmpty() }
        val nativeChecksumEnabled = configReader[prefix + "native-checksum-enabled"]
            ?.toBooleanStrictOrNull()
            ?: (endpoint == null)
        return when (type) {
            StorageBackendType.LOCAL -> StorageBackendConfig(
                name = name,
                type = type,
                endpoint = null,
                publicEndpoint = null,
                nativeChecksumEnabled = false,
                region = null,
                bucket = null,
                rootPrefix = configReader[prefix + "root-prefix"] ?: "blob",
                pathStyleAccess = false,
                accessKey = null,
                secretKey = null,
                rootPath = configReader[prefix + "root-path"] ?: "temp/storage/$name",
            )

            StorageBackendType.S3 -> StorageBackendConfig(
                name = name,
                type = type,
                endpoint = endpoint,
                publicEndpoint = configReader[prefix + "public-endpoint"],
                nativeChecksumEnabled = nativeChecksumEnabled,
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
        backends: Map<String, StorageBackendConfig>,
    ): StorageGroupConfig {
        // TODO(RollW): Replace manual prefix-based reads with strongly typed config binding.
        val prefix = "storage.group.$name."
        val resolvedBackends = resolveGroupBackends(prefix, backends)
        return StorageGroupConfig(
            name = name,
            backends = resolvedBackends,
            visibility = StorageVisibility.from(configReader[prefix + "visibility"]) ?: StorageVisibility.PRIVATE,
            downloadPolicy = StorageGroupDownloadPolicy.from(configReader[prefix + "download-mode"]),
            placementMode = StorageGroupPlacementMode.from(
                configReader[prefix + "placement-mode"] ?: configReader[prefix + "redundancy"]
            ),
            loadBalanceMode = StorageGroupLoadBalanceMode.from(configReader[prefix + "load-balance"]),
            maxSizeBytes = configReader[prefix + "max-size-bytes"]?.toLongOrNull(),
            allowedFileTypes = readStringSet(prefix + "allowed-file-types")
                .mapNotNull(FileType::from)
                .toSet(),
        )
    }

    private fun resolveGroupBackends(
        prefix: String,
        backends: Map<String, StorageBackendConfig>,
    ): List<StorageGroupBackend> {
        val rawExplicitBackends = configReader[prefix + "backends"]
        if (rawExplicitBackends != null) {
            val explicitBackends = readWeightedBackends(prefix + "backends")
            explicitBackends.forEach {
                require(backends.containsKey(it.backendName)) {
                    "Group ${prefix.removePrefix("storage.group.").removeSuffix(".")} references unknown backend ${it.backendName}"
                }
            }
            return explicitBackends
        }

        val primaryBackend = configReader[prefix + "primary-backend"] ?: backends.keys.firstOrNull()
        ?: throw IllegalArgumentException("No storage backend available for group ${prefix.removePrefix("storage.group.").removeSuffix(".")}")
        require(backends.containsKey(primaryBackend)) {
            "Group ${prefix.removePrefix("storage.group.").removeSuffix(".")} references unknown backend $primaryBackend"
        }
        val members = mutableListOf(StorageGroupBackend(primaryBackend))
        readStringSet(prefix + "replica-backends")
            .filter { it != primaryBackend }
            .forEach {
                require(backends.containsKey(it)) {
                    "Group ${prefix.removePrefix("storage.group.").removeSuffix(".")} references unknown replica backend $it"
                }
                members.add(StorageGroupBackend(it))
            }
        return members
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

    private fun readWeightedBackends(key: String): List<StorageGroupBackend> {
        return readStringSet(key).map {
            val separatorIndex = it.lastIndexOf(':')
            if (separatorIndex <= 0 || separatorIndex == it.length - 1) {
                return@map StorageGroupBackend(it)
            }
            val backendName = it.substring(0, separatorIndex).trim()
            val weight = it.substring(separatorIndex + 1).trim().toIntOrNull() ?: 1
            StorageGroupBackend(backendName, weight)
        }
    }
}
