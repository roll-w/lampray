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

package tech.lamprism.lampray.system.database

internal fun closeDatabaseResources(resources: Iterable<AutoCloseable>): Exception? {
    var failure: Exception? = null
    resources.toList().asReversed().forEach { resource ->
        try {
            resource.close()
        } catch (e: Exception) {
            if (failure == null) {
                failure = e
            } else {
                failure.addSuppressed(e)
            }
        }
    }
    return failure
}

internal fun addResourceCleanupSuppressed(resources: Iterable<AutoCloseable>, cause: Exception) {
    closeDatabaseResources(resources)?.let(cause::addSuppressed)
}
