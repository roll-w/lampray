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

package tech.lamprism.lampray.storage.service;

import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;

import java.util.Objects;

/**
 * Describes the intent for materializing a blob into one or more backends.
 *
 * @author RollW
 */
record BlobMaterializationRequest(StorageWritePlan writePlan,
                                  String mimeType,
                                  FileType fileType,
                                  long size,
                                  String checksum,
                                  BlobMaterializationSource source) {
    public BlobMaterializationRequest {
        writePlan = Objects.requireNonNull(writePlan, "writePlan must not be null");
        mimeType = Objects.requireNonNull(mimeType, "mimeType must not be null");
        fileType = Objects.requireNonNull(fileType, "fileType must not be null");
        checksum = Objects.requireNonNull(checksum, "checksum must not be null");
        source = Objects.requireNonNull(source, "source must not be null");
    }

    public String groupName() {
        return writePlan.groupConfig().getName();
    }

    public String primaryBackend() {
        return writePlan.primaryBackend();
    }
}
