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
    val defaultGroup: String
        get() =
            configReader[StorageConfigKeys.DEFAULT_GROUP, "default"]

    val directAccessEnabled: Boolean
        get() =
            configReader[StorageConfigKeys.DIRECT_ACCESS_ENABLED, true]

    val directAccessTtlSeconds: Long
        get() =
            configReader[StorageConfigKeys.DIRECT_ACCESS_TTL_SECONDS, 300L]

    val uploadProxyThresholdBytes: Long
        get() =
            configReader[StorageConfigKeys.UPLOAD_PROXY_THRESHOLD_BYTES, 8L * 1024L * 1024L]

    val downloadProxyThresholdBytes: Long
        get() =
            configReader[StorageConfigKeys.DOWNLOAD_PROXY_THRESHOLD_BYTES, 16L * 1024L * 1024L]

    val pendingUploadExpireSeconds: Long
        get() =
            configReader[StorageConfigKeys.PENDING_UPLOAD_EXPIRE_SECONDS, 1800L]

    val cleanupOrphanUploadExpireSeconds: Long
        get() =
            configReader[StorageConfigKeys.CLEANUP_ORPHAN_UPLOAD_EXPIRE_SECONDS, 86400L]

    val cleanupExpiredUploadRetainSeconds: Long
        get() =
            configReader[StorageConfigKeys.CLEANUP_EXPIRED_UPLOAD_RETAIN_SECONDS, 86400L]

    val cleanupCompletedUploadRetainSeconds: Long
        get() =
            configReader[StorageConfigKeys.CLEANUP_COMPLETED_UPLOAD_RETAIN_SECONDS, 604800L]

    val cleanupDeletedBlobRetainSeconds: Long
        get() =
            configReader[StorageConfigKeys.CLEANUP_DELETED_BLOB_RETAIN_SECONDS, 0L]

    val cleanupIntervalSeconds: Long
        get() =
            configReader[StorageConfigKeys.CLEANUP_INTERVAL_SECONDS, 600L]
}
