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

import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementWriter;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;

import java.io.IOException;
import java.io.InputStream;

/**
 * Blob content source used during materialization.
 *
 * @author RollW
 */
public interface BlobMaterializationSource {
    /**
     * Resolves the primary object key for materialization.
     */
    String resolvePrimaryObjectKey(BlobObjectKeyFactory blobObjectKeyFactory,
                                   BlobMaterializationRequest request);

    /**
     * Opens the source content stream for compatibility checks.
     */
    InputStream openSourceStream(BlobStoreLocator blobStoreLocator) throws IOException;

    /**
     * Materializes the primary blob object.
     */
    void materializePrimary(BlobPlacementWriter writer,
                            BlobMaterializationRequest request,
                            String primaryObjectKey) throws IOException;

    /**
     * Materializes a replica placement.
     */
    void materializeReplica(BlobPlacementWriter writer,
                            BlobMaterializationRequest request,
                            String targetBackend,
                            String targetObjectKey,
                            String sourceBackend,
                            String sourceObjectKey) throws IOException;

    /**
     * Resolves the backend used as the source of truth.
     */
    String resolveSourceBackend(StorageBlobEntity blobEntity,
                                BlobMaterializationRequest request);

    /**
     * Resolves the source object key used for replication.
     */
    String resolveSourceObjectKey(StorageBlobEntity blobEntity,
                                  BlobMaterializationRequest request,
                                  String primaryObjectKey);

    /**
     * Indicates whether the primary placement must be preserved on failure.
     */
    boolean protectsPrimaryPlacement();
}
