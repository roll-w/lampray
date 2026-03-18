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

package tech.lamprism.lampray.storage

/**
 * Options used to resolve a proxy or direct storage reference.
 *
 * @author RollW
 */
data class StorageReferenceRequest(
    val mode: StorageReferenceMode = StorageReferenceMode.PROXY,
    val directAccessMode: StorageDirectAccessMode = StorageDirectAccessMode.AUTO,
    val ttlSeconds: Long? = null,
    val fallbackToProxy: Boolean = true,
) {
    companion object {
        @JvmStatic
        fun proxy(): StorageReferenceRequest = StorageReferenceRequest(
            mode = StorageReferenceMode.PROXY,
        )

        @JvmStatic
        fun auto(): StorageReferenceRequest = StorageReferenceRequest(
            mode = StorageReferenceMode.AUTO,
        )

        @JvmStatic
        fun signed(ttlSeconds: Long?): StorageReferenceRequest = StorageReferenceRequest(
            mode = StorageReferenceMode.DIRECT,
            directAccessMode = StorageDirectAccessMode.SIGNED,
            ttlSeconds = ttlSeconds,
        )

        @JvmStatic
        fun publicDirect(): StorageReferenceRequest = StorageReferenceRequest(
            mode = StorageReferenceMode.DIRECT,
            directAccessMode = StorageDirectAccessMode.PUBLIC,
        )
    }
}
