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

data class StorageMonitoringOverview(
    val totalFiles: Long,
    val logicalBytes: Long,
    val totalBlobs: Long,
    val uniqueBytes: Long,
    val totalPlacements: Long,
    val physicalBytes: Long,
    val backendCount: Int,
    val groupCount: Int,
    val sessionCounts: Map<String, Long>,
    val backendTraffic: StorageTrafficSnapshot,
) {
    fun totalFiles(): Long = totalFiles

    fun logicalBytes(): Long = logicalBytes

    fun totalBlobs(): Long = totalBlobs

    fun uniqueBytes(): Long = uniqueBytes

    fun totalPlacements(): Long = totalPlacements

    fun physicalBytes(): Long = physicalBytes

    fun backendCount(): Int = backendCount

    fun groupCount(): Int = groupCount

    fun sessionCounts(): Map<String, Long> = sessionCounts

    fun backendTraffic(): StorageTrafficSnapshot = backendTraffic
}
