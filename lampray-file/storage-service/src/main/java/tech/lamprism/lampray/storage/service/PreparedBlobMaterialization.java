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
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;

import java.util.Map;

public record PreparedBlobMaterialization(StorageBlobEntity existingBlob,
                                          String checksum,
                                          long size,
                                          String mimeType,
                                          FileType fileType,
                                          String primaryBackend,
                                          String primaryObjectKey,
                                          Map<String, String> placementsToPersist) {
    public static PreparedBlobMaterialization existing(StorageBlobEntity blobEntity,
                                                       long size,
                                                       Map<String, String> placementsToPersist) {
        return new PreparedBlobMaterialization(
                blobEntity,
                blobEntity.getChecksumSha256(),
                size,
                blobEntity.getMimeType(),
                blobEntity.getFileType(),
                blobEntity.getPrimaryBackend(),
                blobEntity.getPrimaryObjectKey(),
                Map.copyOf(placementsToPersist)
        );
    }

    public static PreparedBlobMaterialization newBlob(String checksum,
                                                      long size,
                                                      String mimeType,
                                                      FileType fileType,
                                                      String primaryBackend,
                                                      String primaryObjectKey,
                                                      Map<String, String> placementsToPersist) {
        return new PreparedBlobMaterialization(
                null,
                checksum,
                size,
                mimeType,
                fileType,
                primaryBackend,
                primaryObjectKey,
                Map.copyOf(placementsToPersist)
        );
    }
}
