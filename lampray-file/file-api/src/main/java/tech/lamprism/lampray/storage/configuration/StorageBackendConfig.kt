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

/**
 * @author RollW
 */
data class StorageBackendConfig(
    val name: String,
    val type: StorageBackendType,
    val endpoint: String?,
    val publicEndpoint: String?,
    val region: String?,
    val bucket: String?,
    val rootPrefix: String,
    val pathStyleAccess: Boolean,
    val accessKey: String?,
    val secretKey: String?,
    val rootPath: String?,
)
