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

package tech.lamprism.lampray.storage.materialization;

import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementWriter;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Materializes a blob from a staged temporary file.
 *
 * @author RollW
 */
public final class TempFileBlobSource implements BlobMaterializationSource {
    private final Path tempPath;

    public TempFileBlobSource(Path tempPath) {
        this.tempPath = Objects.requireNonNull(tempPath, "tempPath must not be null");
    }

    @Override
    public String resolvePrimaryObjectKey(BlobObjectKeyFactory blobObjectKeyFactory,
                                          String checksum) {
        return blobObjectKeyFactory.createKey(checksum);
    }

    @Override
    public void materializePrimary(BlobPlacementWriter writer,
                                   BlobMaterializationRequest request,
                                   String primaryObjectKey) throws IOException {
        writer.putTempToBackend(
                request.primaryBackend(),
                primaryObjectKey,
                tempPath,
                request.size(),
                request.mimeType(),
                request.checksum()
        );
    }

    @Override
    public void materializeReplica(BlobPlacementWriter writer,
                                   BlobMaterializationRequest request,
                                   String targetBackend,
                                   String targetObjectKey,
                                   String sourceBackend,
                                   String sourceObjectKey) throws IOException {
        writer.putTempToBackend(
                targetBackend,
                targetObjectKey,
                tempPath,
                request.size(),
                request.mimeType(),
                request.checksum()
        );
    }

    @Override
    public String resolveSourceBackend(StorageBlobEntity blobEntity,
                                       BlobMaterializationRequest request) {
        return blobEntity.getPrimaryBackend();
    }

    @Override
    public String resolveSourceObjectKey(StorageBlobEntity blobEntity,
                                         BlobMaterializationRequest request,
                                         String primaryObjectKey) {
        return blobEntity.getPrimaryObjectKey();
    }

    @Override
    public boolean protectsPrimaryPlacement() {
        return false;
    }
}
