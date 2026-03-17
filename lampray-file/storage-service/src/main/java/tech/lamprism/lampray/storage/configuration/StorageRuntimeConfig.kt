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

/**
 * @author RollW
 */
@Component
class StorageRuntimeConfig(
    private val configReader: ConfigReader,
) {
    fun defaultGroup(): String =
        configReader[StorageConfigKeys.DEFAULT_GROUP, "default"]

    fun deduplicationEnabled(): Boolean =
        configReader[StorageConfigKeys.DEDUPLICATION_ENABLED, true]

    fun directAccessEnabled(): Boolean =
        configReader[StorageConfigKeys.DIRECT_ACCESS_ENABLED, true]

    fun directAccessTtlSeconds(): Long =
        configReader[StorageConfigKeys.DIRECT_ACCESS_TTL_SECONDS, 300L]

    fun uploadProxyThresholdBytes(): Long =
        configReader[StorageConfigKeys.UPLOAD_PROXY_THRESHOLD_BYTES, 8L * 1024L * 1024L]

    fun downloadProxyThresholdBytes(): Long =
        configReader[StorageConfigKeys.DOWNLOAD_PROXY_THRESHOLD_BYTES, 16L * 1024L * 1024L]

    fun pendingUploadExpireSeconds(): Long =
        configReader[StorageConfigKeys.PENDING_UPLOAD_EXPIRE_SECONDS, 1800L]
}
