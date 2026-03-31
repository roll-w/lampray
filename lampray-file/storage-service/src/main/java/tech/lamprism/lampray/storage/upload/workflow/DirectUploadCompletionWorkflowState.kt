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

package tech.lamprism.lampray.storage.upload.workflow

import tech.lamprism.lampray.storage.FileStorage
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization
import tech.lamprism.lampray.storage.routing.StorageWritePlan
import tech.lamprism.lampray.storage.store.BlobObject
import tech.lamprism.lampray.storage.store.BlobStore

/**
 * @author RollW
 */
data class DirectUploadCompletionWorkflowState(
    var writePlan: StorageWritePlan? = null,
    var groupSettings: StorageGroupConfig? = null,
    var primaryBlobStore: BlobStore? = null,
    var uploadedObject: BlobObject? = null,
    var expectedChecksum: String? = null,
    var actualChecksum: String? = null,
    var preparedBlob: PreparedBlobMaterialization? = null,
    var result: FileStorage? = null,
)
