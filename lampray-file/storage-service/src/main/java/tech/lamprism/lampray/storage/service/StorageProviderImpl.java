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

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreRegistry;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageProviderImpl implements StorageProvider {
    private static final int BUFFER_SIZE = 8192;

    private final StorageTopology storageTopology;
    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageFileRepository storageFileRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final ResourceIdGenerator resourceIdGenerator;
    private final StorageGroupRouter storageGroupRouter;
    private final StorageAccessModePolicy storageAccessModePolicy;
    private final StorageAccessResolver storageAccessResolver;
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final StorageUploadValidator storageUploadValidator;
    private final StorageBlobMaterializer storageBlobMaterializer;
    private final List<StorageMaterializationHook> storageMaterializationHooks;
    private final TransactionTemplate transactionTemplate;

    public StorageProviderImpl(StorageTopology storageTopology,
                               StorageRuntimeConfig runtimeSettings,
                               BlobStoreRegistry blobStoreRegistry,
                               StorageFileRepository storageFileRepository,
                               StorageUploadSessionRepository storageUploadSessionRepository,
                               ResourceIdGenerator resourceIdGenerator,
                               StorageGroupRouter storageGroupRouter,
                               StorageAccessModePolicy storageAccessModePolicy,
                               StorageAccessResolver storageAccessResolver,
                               BlobObjectKeyFactory blobObjectKeyFactory,
                               StorageUploadValidator storageUploadValidator,
                               StorageBlobMaterializer storageBlobMaterializer,
                               PlatformTransactionManager transactionManager,
                               List<StorageMaterializationHook> storageMaterializationHooks) {
        this.storageTopology = storageTopology;
        this.runtimeSettings = runtimeSettings;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageFileRepository = storageFileRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.resourceIdGenerator = resourceIdGenerator;
        this.storageGroupRouter = storageGroupRouter;
        this.storageAccessModePolicy = storageAccessModePolicy;
        this.storageAccessResolver = storageAccessResolver;
        this.blobObjectKeyFactory = blobObjectKeyFactory;
        this.storageUploadValidator = storageUploadValidator;
        this.storageBlobMaterializer = storageBlobMaterializer;
        this.storageMaterializationHooks = List.copyOf(storageMaterializationHooks);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage saveFile(InputStream inputStream) throws IOException {
        StorageUploadSession uploadSession = createUploadSession(
                new StorageUploadRequest(
                        runtimeSettings.defaultGroup(),
                        "upload.bin",
                        null,
                        "application/octet-stream",
                        null
                ),
                null
        );
        try {
            return uploadFileContent(uploadSession.getUploadId(), inputStream, null);
        } catch (IOException | RuntimeException exception) {
            expirePendingUploadSession(uploadSession.getUploadId());
            throw exception;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        String groupName = resolveGroupName(request.getGroupName());
        StorageGroupConfig groupSettings = storageTopology.getGroup(groupName);
        String fileName = storageUploadValidator.normalizeFileName(request.getFileName());
        String mimeType = storageUploadValidator.requireMimeType(request.getMimeType());
        FileType fileType = FileType.fromMimeType(mimeType);
        storageUploadValidator.validateUploadRequest(request, groupSettings, fileType);

        String checksum = storageUploadValidator.normalizeChecksum(request.getChecksumSha256());
        String uploadId = newId();
        String fileId = newId();
        StorageWritePlan writePlan = selectWritePlan(groupName);
        String primaryBackend = writePlan.primaryBackend();
        BlobStore primaryBlobStore = requireBlobStore(primaryBackend);
        StorageUploadMode uploadMode = storageAccessModePolicy.resolveUploadMode(request, checksum, primaryBlobStore);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.pendingUploadExpireSeconds());

        StorageAccessRequest directRequest = null;
        String objectKey = null;
        if (uploadMode == StorageUploadMode.DIRECT) {
            long declaredSize = Objects.requireNonNull(request.getSize(), "Direct uploads require a declared size.");
            objectKey = blobObjectKeyFactory.createKey(Objects.requireNonNull(checksum));
            directRequest = primaryBlobStore.createDirectUpload(
                    new BlobWriteRequest(
                            objectKey,
                            declaredSize,
                            mimeType,
                            buildBlobMetadata(checksum),
                            checksum
                    ),
                    Duration.ofSeconds(runtimeSettings.directAccessTtlSeconds())
            );
        }

        StorageUploadSessionEntity uploadSessionEntity = StorageUploadSessionEntity.builder()
                .setUploadId(uploadId)
                .setFileId(fileId)
                .setGroupName(groupName)
                .setFileName(fileName)
                .setFileSize(request.getSize())
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setChecksumSha256(checksum)
                .setOwnerUserId(userId)
                .setPrimaryBackend(primaryBackend)
                .setObjectKey(objectKey)
                .setUploadMode(uploadMode)
                .setStatus(UploadSessionStatus.PENDING)
                .setExpiresAt(expiresAt)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.save(uploadSessionEntity));

        return new StorageUploadSession(
                uploadId,
                uploadMode,
                fileName,
                groupName,
                fileId,
                directRequest,
                expiresAt
        );
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage uploadFileContent(String uploadId,
                                         InputStream inputStream,
                                         Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = requireActiveUploadSession(uploadId, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.PROXY) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires completion after direct upload: " + uploadId);
        }

        StorageGroupConfig groupSettings = storageTopology.getGroup(uploadSession.getGroupName());
        TempUpload tempUpload = writeTempUpload(inputStream, groupSettings.getMaxSizeBytes());
        try {
            storageUploadValidator.validateUploadedContent(uploadSession, tempUpload, groupSettings);
            return finalizeProxyUpload(uploadSession, tempUpload);
        } finally {
            Files.deleteIfExists(tempUpload.path());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileStorage completeUpload(String uploadId,
                                      Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = requireUploadSession(uploadId);
        if (uploadSession.getStatus() == UploadSessionStatus.COMPLETED) {
            return requireFileEntity(uploadSession.getFileId()).lock();
        }
        requireActiveUploadSession(uploadSession, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.DIRECT) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires proxy upload: " + uploadId);
        }

        String checksum = storageUploadValidator.normalizeChecksum(uploadSession.getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct uploads require a checksum.");
        }

        BlobStore primaryBlobStore = requireBlobStore(uploadSession.getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(uploadSession.getObjectKey()));
        StorageGroupConfig groupSettings = storageTopology.getGroup(uploadSession.getGroupName());
        storageUploadValidator.validateUploadedObject(uploadSession, uploadedObject, groupSettings);

        String actualChecksum = verifyUploadedChecksum(primaryBlobStore, uploadedObject, checksum);
        return finalizeDirectUpload(uploadSession, actualChecksum, uploadedObject);
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        return storageAccessResolver.resolveDownload(fileId, userId);
    }

    private FileStorage finalizeProxyUpload(StorageUploadSessionEntity uploadSession,
                                            TempUpload tempUpload) throws IOException {
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializer.prepareBlobMaterialization(
                uploadSession.getGroupName(),
                uploadSession.getMimeType(),
                uploadSession.getFileType(),
                tempUpload.size(),
                tempUpload.checksumSha256(),
                tempUpload.path(),
                uploadSession.getPrimaryBackend(),
                null
        );
        return persistMaterializedUpload(uploadSession, preparedBlob);
    }

    private FileStorage finalizeDirectUpload(StorageUploadSessionEntity uploadSession,
                                             String checksum,
                                             BlobObject uploadedObject) throws IOException {
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializer.prepareBlobMaterialization(
                uploadSession.getGroupName(),
                uploadSession.getMimeType(),
                uploadSession.getFileType(),
                uploadedObject.size(),
                checksum,
                null,
                uploadSession.getPrimaryBackend(),
                uploadedObject
        );
        return persistMaterializedUpload(uploadSession, preparedBlob);
    }

    private FileStorage persistMaterializedUpload(StorageUploadSessionEntity uploadSession,
                                                  PreparedBlobMaterialization preparedBlob) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = storageBlobMaterializer.persistBlobMaterialization(preparedBlob);
            return createFileEntity(uploadSession, blobEntity, preparedBlob.size());
        }));
    }

    private FileStorage createFileEntity(StorageUploadSessionEntity uploadSession,
                                         StorageBlobEntity blobEntity,
                                         long size) {
        OffsetDateTime now = OffsetDateTime.now();
        StorageFileEntity fileEntity = StorageFileEntity.builder()
                .setFileId(uploadSession.getFileId())
                .setBlobId(blobEntity.getBlobId())
                .setGroupName(uploadSession.getGroupName())
                .setOwnerUserId(uploadSession.getOwnerUserId())
                .setFileName(uploadSession.getFileName())
                .setFileSize(size)
                .setMimeType(uploadSession.getMimeType())
                .setFileType(uploadSession.getFileType())
                .setVisibility(storageTopology.getGroup(uploadSession.getGroupName()).getVisibility())
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);

        uploadSession.setStatus(UploadSessionStatus.COMPLETED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
        FileStorage fileStorage = savedFileEntity.lock();
        notifyMaterializationHooks(fileStorage, blobEntity.getBlobId(), uploadSession.getGroupName(), uploadSession.getOwnerUserId());
        return fileStorage;
    }

    private TempUpload writeTempUpload(InputStream inputStream,
                                       Long maxSizeBytes) throws IOException {
        Path tempFile = Files.createTempFile("lampray-upload-", ".bin");
        MessageDigest digest = newSha256Digest();
        long size = 0;
        try {
            try (InputStream source = inputStream;
                 var outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = source.read(buffer)) != -1) {
                    size += read;
                    if (maxSizeBytes != null && size > maxSizeBytes) {
                        throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                                "Uploaded file size exceeds the configured group limit.");
                    }
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                }
            }
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(tempFile);
            throw exception;
        }
        return new TempUpload(tempFile, size, toHex(digest.digest()));
    }

    private Map<String, String> buildBlobMetadata(String checksumSha256) {
        if (checksumSha256 == null) {
            return Map.of();
        }
        return Map.of("checksum-sha256", checksumSha256);
    }

    private String resolveGroupName(String requestedGroupName) {
        if (!StringUtils.hasText(requestedGroupName)) {
            return runtimeSettings.defaultGroup();
        }
        return requestedGroupName.trim();
    }

    private StorageUploadSessionEntity requireActiveUploadSession(String uploadId,
                                                                  Long userId) {
        return requireActiveUploadSession(requireUploadSession(uploadId), userId);
    }

    private StorageUploadSessionEntity requireActiveUploadSession(StorageUploadSessionEntity uploadSession,
                                                                  Long userId) {
        if (uploadSession.getStatus() != UploadSessionStatus.PENDING) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session is not pending: " + uploadSession.getUploadId());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadSession.getExpiresAt().isBefore(now)) {
            transactionTemplate.executeWithoutResult(status -> expireUploadSession(uploadSession, now));
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session has expired: " + uploadSession.getUploadId());
        }
        if (uploadSession.getOwnerUserId() != null && !uploadSession.getOwnerUserId().equals(userId)) {
            throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                    "You are not allowed to use this upload session.");
        }
        return uploadSession;
    }

    private StorageUploadSessionEntity requireUploadSession(String uploadId) {
        return storageUploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Upload session not found: " + uploadId));
    }

    private void expirePendingUploadSession(String uploadId) {
        transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.findById(uploadId)
                .filter(uploadSession -> uploadSession.getStatus() == UploadSessionStatus.PENDING)
                .ifPresent(uploadSession -> expireUploadSession(uploadSession, OffsetDateTime.now())));
    }

    private void expireUploadSession(StorageUploadSessionEntity uploadSession,
                                     OffsetDateTime now) {
        uploadSession.setStatus(UploadSessionStatus.EXPIRED);
        uploadSession.setUpdateTime(now);
        storageUploadSessionRepository.save(uploadSession);
    }

    private StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId));
    }

    private StorageWritePlan selectWritePlan(String groupName) {
        try {
            return storageGroupRouter.selectWritePlan(groupName);
        } catch (IllegalStateException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        }
    }

    private BlobStore requireBlobStore(String backendName) {
        return blobStoreRegistry.find(backendName)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "Storage backend is not available: " + backendName));
    }

    private void notifyMaterializationHooks(FileStorage fileStorage,
                                            String blobId,
                                            String groupName,
                                            Long ownerUserId) {
        StorageMaterializationContext context = new StorageMaterializationContext(
                fileStorage,
                blobId,
                groupName,
                ownerUserId
        );
        for (StorageMaterializationHook storageMaterializationHook : storageMaterializationHooks) {
            storageMaterializationHook.afterMaterialized(context);
        }
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

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }

    private String verifyUploadedChecksum(BlobStore blobStore,
                                          BlobObject uploadedObject,
                                          String expectedChecksum) throws IOException {
        String actualChecksum = uploadedObject.checksumSha256();
        if (!StringUtils.hasText(actualChecksum)) {
            String metadataChecksum = uploadedObject.metadata().get("checksum-sha256");
            if (StringUtils.hasText(metadataChecksum)) {
                actualChecksum = storageUploadValidator.normalizeChecksum(metadataChecksum);
            }
        }
        if (!StringUtils.hasText(actualChecksum)) {
            actualChecksum = calculateChecksum(blobStore, uploadedObject.key());
        }
        storageUploadValidator.validateChecksumMatch(expectedChecksum, actualChecksum);
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

}
