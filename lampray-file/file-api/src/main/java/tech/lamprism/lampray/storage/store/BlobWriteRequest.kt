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

/**
 * @author RollW
 */
data class BlobWriteRequest(
    val key: String,
    val size: Long,
    val contentType: String?,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(key.isNotBlank()) { "key must not be blank" }
        require(size >= 0) { "size must not be negative" }
    }

    val normalizedMetadata: Map<String, String> = metadata.takeIf { it.isNotEmpty() }?.toMap() ?: emptyMap()

    fun key(): String = key

    fun size(): Long = size

    fun contentType(): String? = contentType

    fun metadata(): Map<String, String> = normalizedMetadata
}
