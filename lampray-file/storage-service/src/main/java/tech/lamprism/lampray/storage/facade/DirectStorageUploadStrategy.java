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

package tech.lamprism.lampray.storage.facade;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.policy.ChecksumNormalizer;
import tech.lamprism.lampray.storage.policy.ChecksumValidator;
import tech.lamprism.lampray.storage.policy.UploadedObjectValidator;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Component
public class DirectStorageUploadStrategy implements StorageUploadStrategy {
    private static final int BUFFER_SIZE = 8192;

    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageGroupRouter storageGroupRouter;
    private final UploadedObjectValidator uploadedObjectValidator;
    private final ChecksumNormalizer checksumNormalizer;
    private final ChecksumValidator checksumValidator;
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFilePersistenceService storageFilePersistenceService;

    public DirectStorageUploadStrategy(BlobStoreRegistry blobStoreRegistry,
                                       StorageGroupRouter storageGroupRouter,
                                       UploadedObjectValidator uploadedObjectValidator,
                                       ChecksumNormalizer checksumNormalizer,
                                       ChecksumValidator checksumValidator,
                                       StorageBlobMaterializationService storageBlobMaterializationService,
                                       StorageFilePersistenceService storageFilePersistenceService) {
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageGroupRouter = storageGroupRouter;
        this.uploadedObjectValidator = uploadedObjectValidator;
        this.checksumNormalizer = checksumNormalizer;
        this.checksumValidator = checksumValidator;
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFilePersistenceService = storageFilePersistenceService;
    }

    @Override
    public StorageUploadMode mode() {
        return StorageUploadMode.DIRECT;
    }

    @Override
    public FileStorage completeUpload(StorageUploadSessionEntity uploadSession) throws IOException {
        String checksum = checksumNormalizer.normalize(uploadSession.getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct uploads require a checksum.");
        }

        BlobStore primaryBlobStore = requireBlobStore(uploadSession.getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(uploadSession.getObjectKey()));
        StorageGroupConfig groupSettings = restoreWritePlan(uploadSession).getGroupSettings();
        uploadedObjectValidator.validate(uploadSession, uploadedObject, groupSettings);

        String actualChecksum = verifyUploadedChecksum(primaryBlobStore, uploadedObject, checksum);
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forUploadedObject(
                        restoreWritePlan(uploadSession),
                        uploadSession.getMimeType(),
                        uploadSession.getFileType(),
                        uploadedObject.getSize(),
                        actualChecksum,
                        uploadedObject
                )
        );
        return storageFilePersistenceService.persistSessionUpload(uploadSession, preparedBlob);
    }

    private StorageWritePlan restoreWritePlan(StorageUploadSessionEntity uploadSession) {
        try {
            return storageGroupRouter.restoreWritePlan(uploadSession.getGroupName(), uploadSession.getPrimaryBackend());
        } catch (IllegalStateException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        }
    }

    private BlobStore requireBlobStore(String backendName) {
        return blobStoreRegistry.find(backendName)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Storage backend is not available: " + backendName));
    }

    private String verifyUploadedChecksum(BlobStore blobStore,
                                          BlobObject uploadedObject,
                                          String expectedChecksum) throws IOException {
        String actualChecksum = uploadedObject.getChecksumSha256();
        if (!StringUtils.hasText(actualChecksum)) {
            String metadataChecksum = uploadedObject.getMetadata().get("checksum-sha256");
            if (StringUtils.hasText(metadataChecksum)) {
                actualChecksum = checksumNormalizer.normalize(metadataChecksum);
            }
        }
        if (!StringUtils.hasText(actualChecksum)) {
            actualChecksum = calculateChecksum(blobStore, uploadedObject.getKey());
        }
        checksumValidator.validateMatch(expectedChecksum, actualChecksum);
        return actualChecksum;
    }

    private String calculateChecksum(BlobStore blobStore,
                                     String objectKey) throws IOException {
        MessageDigest digest = newSha256Digest();
        try (InputStream inputStream = blobStore.openDownload(objectKey).openStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String toHex(byte[] digest) {
        StringBuilder stringBuilder = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            stringBuilder.append(Character.forDigit((current >> 4) & 0xF, 16));
            stringBuilder.append(Character.forDigit(current & 0xF, 16));
        }
        return stringBuilder.toString();
    }
}
