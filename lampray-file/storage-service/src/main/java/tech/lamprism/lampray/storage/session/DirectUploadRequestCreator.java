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

package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.materialization.BlobObjectKeyFactory;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class DirectUploadRequestCreator {
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final StorageTrafficPublisher storageTrafficPublisher;

    public DirectUploadRequestCreator(BlobObjectKeyFactory blobObjectKeyFactory,
                                      StorageTrafficPublisher storageTrafficPublisher) {
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    public DirectUploadProvision create(String groupName,
                                        String primaryBackend,
                                        String mimeType,
                                        String checksum,
                                        long declaredSize,
                                        BlobStore primaryBlobStore,
                                        long ttlSeconds) throws IOException {
        String objectKey = blobObjectKeyFactory.createKey(Objects.requireNonNull(checksum));
        StorageAccessRequest accessRequest = primaryBlobStore.createDirectUpload(
                new BlobWriteRequest(
                        objectKey,
                        declaredSize,
                        mimeType,
                        BlobMetadataSupport.checksumMetadata(checksum),
                        checksum
                ),
                Duration.ofSeconds(ttlSeconds)
        );
        storageTrafficPublisher.publishDirectUploadRequest(groupName, primaryBackend, declaredSize);
        return new DirectUploadProvision(objectKey, accessRequest);
    }
}
