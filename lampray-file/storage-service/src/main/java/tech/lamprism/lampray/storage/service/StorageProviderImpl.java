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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageDownloadMode;
import tech.lamprism.lampray.storage.StorageDownloadResult;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.StorageProvider;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.StorageUrlProvider;
import tech.lamprism.lampray.storage.StorageVisibility;
import tech.lamprism.lampray.storage.builtin.BuiltinStorageRegistry;
import tech.lamprism.lampray.storage.configuration.StorageGroupDownloadPolicy;
import tech.lamprism.lampray.storage.configuration.StorageGroupRedundancyMode;
import tech.lamprism.lampray.storage.configuration.StorageGroupSettings;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeSettings;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobPlacementRepository;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;
import tech.lamprism.lampray.storage.store.BlobDownloadRequest;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.store.BlobStoreRegistry;
import tech.lamprism.lampray.storage.store.BlobWriteRequest;
import tech.lamprism.lampray.storage.store.DirectDownloadSupport;
import tech.lamprism.lampray.storage.store.DirectUploadSupport;
import tech.lamprism.lampray.web.ExternalEndpointProvider;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author RollW
 */
@Service
@Transactional(readOnly = true)
public class StorageProviderImpl implements StorageProvider, StorageUrlProvider {
    private static final int BUFFER_SIZE = 8192;

    private final StorageTopology storageTopology;
    private final StorageRuntimeSettings runtimeSettings;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final ExternalEndpointProvider externalEndpointProvider;
    private final BuiltinStorageRegistry builtinStorageRegistry;
    private final ResourceIdGenerator resourceIdGenerator;

    public StorageProviderImpl(StorageTopology storageTopology,
                               StorageRuntimeSettings runtimeSettings,
                               BlobStoreRegistry blobStoreRegistry,
                               StorageFileRepository storageFileRepository,
                               StorageBlobRepository storageBlobRepository,
                               StorageBlobPlacementRepository storageBlobPlacementRepository,
                               StorageUploadSessionRepository storageUploadSessionRepository,
                               ExternalEndpointProvider externalEndpointProvider,
                               BuiltinStorageRegistry builtinStorageRegistry,
                               ResourceIdGenerator resourceIdGenerator) {
        this.storageTopology = storageTopology;
        this.runtimeSettings = runtimeSettings;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.externalEndpointProvider = externalEndpointProvider;
        this.builtinStorageRegistry = builtinStorageRegistry;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    @Override
    @Transactional
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
        return uploadFileContent(uploadSession.getUploadId(), inputStream, null);
    }

    @Override
    @Transactional
    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        String groupName = resolveGroupName(request.getGroupName());
        StorageGroupSettings groupSettings = storageTopology.getGroup(groupName);
        String fileName = normalizeFileName(request.getFileName());
        String mimeType = requireMimeType(request.getMimeType());
        FileType fileType = FileType.fromMimeType(mimeType);
        validateUploadRequest(request, groupSettings, fileType);

        String checksum = normalizeChecksum(request.getChecksumSha256());
        String uploadId = newId();
        String fileId = newId();
        String primaryBackend = groupSettings.getPrimaryBackend();
        BlobStore primaryBlobStore = blobStoreRegistry.get(primaryBackend);
        StorageUploadMode uploadMode = determineUploadMode(request, checksum, primaryBlobStore);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.pendingUploadExpireSeconds());

        StorageAccessRequest directRequest = null;
        String objectKey = null;
        if (uploadMode == StorageUploadMode.DIRECT) {
            long declaredSize = Objects.requireNonNull(request.getSize(), "Direct uploads require a declared size.");
            objectKey = buildBlobObjectKey(Objects.requireNonNull(checksum));
            directRequest = requireDirectUploadSupport(primaryBlobStore).createDirectUpload(
                    new BlobWriteRequest(
                            objectKey,
                            declaredSize,
                            mimeType,
                            uploadObjectMetadata(fileId, checksum)
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
        storageUploadSessionRepository.save(uploadSessionEntity);

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
    @Transactional
    public FileStorage uploadFileContent(String uploadId,
                                         InputStream inputStream,
                                         Long userId) throws IOException {
        StorageUploadSessionEntity uploadSession = requireActiveUploadSession(uploadId, userId);
        if (uploadSession.getUploadMode() != StorageUploadMode.PROXY) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Upload session requires completion after direct upload: " + uploadId);
        }

        TempUpload tempUpload = writeTempUpload(inputStream);
        try {
            validateUploadedContent(uploadSession, tempUpload);
            return finalizeProxyUpload(uploadSession, tempUpload);
        } finally {
            Files.deleteIfExists(tempUpload.path());
        }
    }

    @Override
    @Transactional
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

        String checksum = normalizeChecksum(uploadSession.getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct uploads require a checksum.");
        }

        BlobStore primaryBlobStore = blobStoreRegistry.get(uploadSession.getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(uploadSession.getObjectKey()));
        if (uploadSession.getFileSize() != null && uploadSession.getFileSize() != uploadedObject.size()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size does not match declared size.");
        }

        return finalizeDirectUpload(uploadSession, checksum, uploadedObject);
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        if (builtinStorageRegistry.contains(fileId)) {
            BuiltinStorageRegistry.BuiltinStorageResource builtinResource = builtinStorageRegistry.get(fileId);
            return new StorageDownloadResult(
                    builtinResource.fileStorage(),
                    StorageDownloadMode.PROXY,
                    null,
                    builtinResource.content()
            );
        }

        StorageFileEntity fileEntity = requireFileEntity(fileId);
        ensureDownloadAuthorized(fileEntity, userId);
        StorageGroupSettings groupSettings = storageTopology.getGroup(fileEntity.getGroupName());
        StorageBlobPlacementEntity placementEntity = resolvePlacement(fileEntity.getBlobId(), groupSettings);
        BlobStore blobStore = blobStoreRegistry.get(placementEntity.getBackendName());
        FileStorage fileStorage = fileEntity.lock();
        StorageDownloadMode mode = determineDownloadMode(fileEntity, groupSettings, blobStore);
        if (mode == StorageDownloadMode.DIRECT) {
            StorageAccessRequest directRequest = requireDirectDownloadSupport(blobStore).createDirectDownload(
                    new BlobDownloadRequest(
                            placementEntity.getObjectKey(),
                            fileStorage.getFileName(),
                            fileStorage.getMimeType()
                    ),
                    Duration.ofSeconds(runtimeSettings.directAccessTtlSeconds())
            );
            return new StorageDownloadResult(fileStorage, StorageDownloadMode.DIRECT, directRequest, null);
        }
        return new StorageDownloadResult(
                fileStorage,
                StorageDownloadMode.PROXY,
                null,
                blobStore.openDownload(placementEntity.getObjectKey())
        );
    }

    @Override
    public String getUrlOfStorage(String id) {
        return externalEndpointProvider.getExternalApiEndpoint() + "/api/v1/files/" + id;
    }

    private FileStorage finalizeProxyUpload(StorageUploadSessionEntity uploadSession,
                                            TempUpload tempUpload) throws IOException {
        StorageBlobEntity blobEntity = resolveOrCreateBlob(
                uploadSession.getGroupName(),
                uploadSession.getMimeType(),
                uploadSession.getFileType(),
                tempUpload.size(),
                tempUpload.checksumSha256(),
                tempUpload.path(),
                uploadSession.getPrimaryBackend(),
                null
        );
        return createFileEntity(uploadSession, blobEntity, tempUpload.size());
    }

    private FileStorage finalizeDirectUpload(StorageUploadSessionEntity uploadSession,
                                             String checksum,
                                             BlobObject uploadedObject) throws IOException {
        StorageBlobEntity blobEntity = resolveOrCreateBlob(
                uploadSession.getGroupName(),
                uploadSession.getMimeType(),
                uploadSession.getFileType(),
                uploadedObject.size(),
                checksum,
                null,
                uploadSession.getPrimaryBackend(),
                uploadedObject
        );
        return createFileEntity(uploadSession, blobEntity, uploadedObject.size());
    }

    private StorageBlobEntity resolveOrCreateBlob(String groupName,
                                                  String mimeType,
                                                  FileType fileType,
                                                  long size,
                                                  String checksum,
                                                  Path tempPath,
                                                  String primaryBackend,
                                                  BlobObject existingUploadedObject) throws IOException {
        StorageGroupSettings groupSettings = storageTopology.getGroup(groupName);
        Optional<StorageBlobEntity> existingBlob = runtimeSettings.deduplicationEnabled()
                ? storageBlobRepository.findByChecksumSha256(checksum)
                : Optional.empty();
        if (existingBlob.isPresent()) {
            StorageBlobEntity blobEntity = existingBlob.get();
            ensureRequiredPlacements(blobEntity, groupSettings, mimeType, size, checksum, tempPath, existingUploadedObject,
                    primaryBackend);
            return blobEntity;
        }

        String primaryObjectKey = existingUploadedObject != null
                ? existingUploadedObject.key()
                : buildBlobObjectKey(checksum);
        if (existingUploadedObject == null) {
            putTempToBackend(primaryBackend, primaryObjectKey, tempPath, size, mimeType);
        }

        OffsetDateTime now = OffsetDateTime.now();
        StorageBlobEntity blobEntity = StorageBlobEntity.builder()
                .setBlobId(newId())
                .setChecksumSha256(checksum)
                .setFileSize(size)
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setPrimaryBackend(primaryBackend)
                .setPrimaryObjectKey(primaryObjectKey)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        StorageBlobEntity savedBlobEntity = storageBlobRepository.save(blobEntity);
        savePlacement(savedBlobEntity.getBlobId(), primaryBackend, primaryObjectKey, now);

        if (groupSettings.getRedundancyMode() == StorageGroupRedundancyMode.ASYNC_REPLICA) {
            replicateToReplicaBackends(savedBlobEntity, groupSettings.getReplicaBackends(), size, mimeType, checksum, tempPath);
        }
        return savedBlobEntity;
    }

    private void ensureRequiredPlacements(StorageBlobEntity blobEntity,
                                          StorageGroupSettings groupSettings,
                                          String mimeType,
                                          long size,
                                          String checksum,
                                          Path tempPath,
                                          BlobObject existingUploadedObject,
                                          String primaryBackend) throws IOException {
        Set<String> requiredBackends = new LinkedHashSet<>();
        requiredBackends.add(groupSettings.getPrimaryBackend());
        if (groupSettings.getRedundancyMode() == StorageGroupRedundancyMode.ASYNC_REPLICA) {
            requiredBackends.addAll(groupSettings.getReplicaBackends());
        }

        for (String backendName : requiredBackends) {
            if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobEntity.getBlobId(), backendName).isPresent()) {
                continue;
            }

            String objectKey = buildBlobObjectKey(checksum);
            if (existingUploadedObject != null && backendName.equals(primaryBackend)) {
                savePlacement(blobEntity.getBlobId(), backendName, objectKey, OffsetDateTime.now());
                continue;
            }

            if (tempPath != null) {
                putTempToBackend(backendName, objectKey, tempPath, size, mimeType);
                savePlacement(blobEntity.getBlobId(), backendName, objectKey, OffsetDateTime.now());
                continue;
            }

            replicateExistingBlob(blobEntity, backendName, objectKey, size, mimeType);
        }
    }

    private void replicateToReplicaBackends(StorageBlobEntity blobEntity,
                                            Collection<String> replicaBackends,
                                            long size,
                                            String mimeType,
                                            String checksum,
                                            Path tempPath) throws IOException {
        for (String replicaBackend : replicaBackends) {
            if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobEntity.getBlobId(), replicaBackend).isPresent()) {
                continue;
            }
            String objectKey = buildBlobObjectKey(checksum);
            if (tempPath != null) {
                putTempToBackend(replicaBackend, objectKey, tempPath, size, mimeType);
                savePlacement(blobEntity.getBlobId(), replicaBackend, objectKey, OffsetDateTime.now());
                continue;
            }
            replicateExistingBlob(blobEntity, replicaBackend, objectKey, size, mimeType);
        }
    }

    private void replicateExistingBlob(StorageBlobEntity blobEntity,
                                       String targetBackend,
                                       String targetObjectKey,
                                       long size,
                                       String mimeType) throws IOException {
        StorageBlobPlacementEntity sourcePlacement = storageBlobPlacementRepository.findByBlobIdAndBackendName(
                blobEntity.getBlobId(),
                blobEntity.getPrimaryBackend()
        ).orElseGet(() -> storageBlobPlacementRepository.findAllByBlobId(blobEntity.getBlobId()).stream()
                .findFirst()
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "No placement found for blob: " + blobEntity.getBlobId())));
        Path tempFile = Files.createTempFile("lampray-replica-", ".bin");
        try {
            try (var outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                blobStoreRegistry.get(sourcePlacement.getBackendName()).openDownload(sourcePlacement.getObjectKey())
                        .writeTo(outputStream);
            }
            putTempToBackend(targetBackend, targetObjectKey, tempFile, size, mimeType);
            savePlacement(blobEntity.getBlobId(), targetBackend, targetObjectKey, OffsetDateTime.now());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void putTempToBackend(String backendName,
                                  String objectKey,
                                  Path tempPath,
                                  long size,
                                  String mimeType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(Objects.requireNonNull(tempPath))) {
            blobStoreRegistry.get(backendName).store(
                    new BlobWriteRequest(
                            objectKey,
                            size,
                            mimeType,
                            Map.of()
                    ),
                    inputStream
            );
        }
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
        return savedFileEntity.lock();
    }

    private void savePlacement(String blobId,
                               String backendName,
                               String objectKey,
                               OffsetDateTime now) {
        StorageBlobPlacementEntity placementEntity = StorageBlobPlacementEntity.builder()
                .setPlacementId(newId())
                .setBlobId(blobId)
                .setBackendName(backendName)
                .setObjectKey(objectKey)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        storageBlobPlacementRepository.save(placementEntity);
    }

    private StorageDownloadMode determineDownloadMode(StorageFileEntity fileEntity,
                                                      StorageGroupSettings groupSettings,
                                                      BlobStore blobStore) {
        if (!runtimeSettings.directAccessEnabled() || !(blobStore instanceof DirectDownloadSupport)) {
            return StorageDownloadMode.PROXY;
        }
        if (groupSettings.getDownloadPolicy() == StorageGroupDownloadPolicy.PROXY) {
            return StorageDownloadMode.PROXY;
        }
        if (groupSettings.getDownloadPolicy() == StorageGroupDownloadPolicy.DIRECT) {
            return StorageDownloadMode.DIRECT;
        }
        return fileEntity.getFileSize() > runtimeSettings.downloadProxyThresholdBytes()
                ? StorageDownloadMode.DIRECT
                : StorageDownloadMode.PROXY;
    }

    private StorageUploadMode determineUploadMode(StorageUploadRequest request,
                                                  String checksum,
                                                  BlobStore blobStore) {
        if (!runtimeSettings.directAccessEnabled() || !(blobStore instanceof DirectUploadSupport)) {
            return StorageUploadMode.PROXY;
        }
        if (request.getSize() == null || request.getSize() <= runtimeSettings.uploadProxyThresholdBytes()) {
            return StorageUploadMode.PROXY;
        }
        return checksum != null ? StorageUploadMode.DIRECT : StorageUploadMode.PROXY;
    }

    private DirectUploadSupport requireDirectUploadSupport(BlobStore blobStore) {
        if (blobStore instanceof DirectUploadSupport directUploadSupport) {
            return directUploadSupport;
        }
        throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Storage backend does not support direct upload: " + blobStore.getBackendName());
    }

    private DirectDownloadSupport requireDirectDownloadSupport(BlobStore blobStore) {
        if (blobStore instanceof DirectDownloadSupport directDownloadSupport) {
            return directDownloadSupport;
        }
        throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                "Storage backend does not support direct download: " + blobStore.getBackendName());
    }

    private void ensureDownloadAuthorized(StorageFileEntity fileEntity,
                                          Long userId) {
        if (fileEntity.getVisibility() == StorageVisibility.PUBLIC) {
            return;
        }
        if (fileEntity.getVisibility() == StorageVisibility.INTERNAL && userId != null) {
            return;
        }
        if (fileEntity.getVisibility() == StorageVisibility.PRIVATE
                && userId != null
                && userId.equals(fileEntity.getOwnerUserId())) {
            return;
        }
        throw new StorageException(AuthErrorCode.ERROR_UNAUTHORIZED_USE,
                "You are not allowed to access this file.");
    }

    private void validateUploadRequest(StorageUploadRequest request,
                                       StorageGroupSettings groupSettings,
                                       FileType fileType) {
        if (request.getSize() != null && request.getSize() < 0) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File size cannot be negative.");
        }
        if (groupSettings.getMaxSizeBytes() != null
                && request.getSize() != null
                && request.getSize() > groupSettings.getMaxSizeBytes()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "File size exceeds the configured group limit.");
        }
        if (!groupSettings.getAllowedFileTypes().isEmpty() && !groupSettings.getAllowedFileTypes().contains(fileType)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "File type is not allowed for this storage group.");
        }
    }

    private void validateUploadedContent(StorageUploadSessionEntity uploadSession,
                                         TempUpload tempUpload) {
        if (uploadSession.getFileSize() != null && uploadSession.getFileSize() != tempUpload.size()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size does not match declared size.");
        }
        String expectedChecksum = normalizeChecksum(uploadSession.getChecksumSha256());
        if (expectedChecksum != null && !expectedChecksum.equals(tempUpload.checksumSha256())) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file checksum does not match declared checksum.");
        }
    }

    private TempUpload writeTempUpload(InputStream inputStream) throws IOException {
        Path tempFile = Files.createTempFile("lampray-upload-", ".bin");
        MessageDigest digest = newSha256Digest();
        long size = 0;
        try (InputStream source = inputStream;
             var outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = source.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
                outputStream.write(buffer, 0, read);
                size += read;
            }
        }
        return new TempUpload(tempFile, size, toHex(digest.digest()));
    }

    private String buildBlobObjectKey(String checksumSha256) {
        return checksumSha256.substring(0, 2) + "/" + checksumSha256.substring(2, 4) + "/" + checksumSha256;
    }

    private Map<String, String> uploadObjectMetadata(String fileId,
                                                     String checksumSha256) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("file-id", fileId);
        if (checksumSha256 != null) {
            metadata.put("checksum-sha256", checksumSha256);
        }
        return metadata;
    }

    private String resolveGroupName(String requestedGroupName) {
        if (!StringUtils.hasText(requestedGroupName)) {
            return runtimeSettings.defaultGroup();
        }
        return requestedGroupName.trim();
    }

    private String normalizeChecksum(String checksumSha256) {
        if (!StringUtils.hasText(checksumSha256)) {
            return null;
        }
        String normalized = checksumSha256.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != 64) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Checksum must be a 64-character SHA-256 hex string.");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            boolean numeric = current >= '0' && current <= '9';
            boolean alphabetic = current >= 'a' && current <= 'f';
            if (!numeric && !alphabetic) {
                throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                        "Checksum must be a lowercase SHA-256 hex string.");
            }
        }
        return normalized;
    }

    private String normalizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is required.");
        }
        String normalized = fileName.trim();
        int slashIndex = normalized.lastIndexOf('/');
        int backslashIndex = normalized.lastIndexOf('\\');
        int separatorIndex = Math.max(slashIndex, backslashIndex);
        if (separatorIndex >= 0 && separatorIndex < normalized.length() - 1) {
            normalized = normalized.substring(separatorIndex + 1);
        }
        if (normalized.isBlank() || ".".equals(normalized) || "..".equals(normalized)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is invalid.");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current == '\r' || current == '\n') {
                throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "File name is invalid.");
            }
        }
        return normalized;
    }

    private String requireMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "MIME type is required.");
        }
        return mimeType.trim();
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
        if (uploadSession.getExpiresAt().isBefore(OffsetDateTime.now())) {
            uploadSession.setStatus(UploadSessionStatus.EXPIRED);
            uploadSession.setUpdateTime(OffsetDateTime.now());
            storageUploadSessionRepository.save(uploadSession);
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

    private StorageFileEntity requireFileEntity(String fileId) {
        return storageFileRepository.findById(fileId)
                .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                        "File not found: " + fileId));
    }

    private StorageBlobPlacementEntity resolvePlacement(String blobId,
                                                        StorageGroupSettings groupSettings) {
        return storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, groupSettings.getPrimaryBackend())
                .orElseGet(() -> storageBlobPlacementRepository.findAllByBlobId(blobId).stream()
                        .findFirst()
                        .orElseThrow(() -> new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST,
                                "No blob placement found for blob: " + blobId)));
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

    private record TempUpload(Path path, long size, String checksumSha256) {
    }
}
