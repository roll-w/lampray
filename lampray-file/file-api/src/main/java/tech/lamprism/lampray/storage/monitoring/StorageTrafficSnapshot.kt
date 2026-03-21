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

package tech.lamprism.lampray.storage.monitoring

data class StorageTrafficSnapshot(
    val uploadBytes: Long,
    val uploadCount: Long,
    val downloadBytes: Long,
    val downloadCount: Long,
    val directUploadRequestCount: Long,
    val directUploadDeclaredBytes: Long,
    val directDownloadRequestCount: Long,
) {
    fun uploadBytes(): Long = uploadBytes

    fun uploadCount(): Long = uploadCount

    fun downloadBytes(): Long = downloadBytes

    fun downloadCount(): Long = downloadCount

    fun directUploadRequestCount(): Long = directUploadRequestCount

    fun directUploadDeclaredBytes(): Long = directUploadDeclaredBytes

    fun directDownloadRequestCount(): Long = directDownloadRequestCount

    companion object {
        @JvmStatic
        fun empty(): StorageTrafficSnapshot = StorageTrafficSnapshot(0, 0, 0, 0, 0, 0, 0)
    }
}
