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

import org.springframework.dao.DataIntegrityViolationException;
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
import tech.lamprism.lampray.storage.configuration.StorageGroupPlacementMode;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
public class StorageProviderImpl implements StorageProvider {
    private static final int BUFFER_SIZE = 8192;

    private final StorageTopology storageTopology;
    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreRegistry blobStoreRegistry;
    private final StorageFileRepository storageFileRepository;
    private final StorageBlobRepository storageBlobRepository;
    private final StorageBlobPlacementRepository storageBlobPlacementRepository;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final ResourceIdGenerator resourceIdGenerator;
    private final StorageGroupRouter storageGroupRouter;
    private final StorageAccessModeResolver storageAccessModeResolver;
    private final StorageAccessService storageAccessService;
    private final BlobObjectKeyFactory blobObjectKeyFactory;
    private final List<StorageMaterializationHook> storageMaterializationHooks;
    private final TransactionTemplate transactionTemplate;

    public StorageProviderImpl(StorageTopology storageTopology,
                               StorageRuntimeConfig runtimeSettings,
                               BlobStoreRegistry blobStoreRegistry,
                               StorageFileRepository storageFileRepository,
                               StorageBlobRepository storageBlobRepository,
                               StorageBlobPlacementRepository storageBlobPlacementRepository,
                               StorageUploadSessionRepository storageUploadSessionRepository,
                               ResourceIdGenerator resourceIdGenerator,
                               StorageGroupRouter storageGroupRouter,
                               StorageAccessModeResolver storageAccessModeResolver,
                               StorageAccessService storageAccessService,
                               BlobObjectKeyFactory blobObjectKeyFactory,
                               PlatformTransactionManager transactionManager,
                               List<StorageMaterializationHook> storageMaterializationHooks) {
        this.storageTopology = storageTopology;
        this.runtimeSettings = runtimeSettings;
        this.blobStoreRegistry = blobStoreRegistry;
        this.storageFileRepository = storageFileRepository;
        this.storageBlobRepository = storageBlobRepository;
        this.storageBlobPlacementRepository = storageBlobPlacementRepository;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.resourceIdGenerator = resourceIdGenerator;
        this.storageGroupRouter = storageGroupRouter;
        this.storageAccessModeResolver = storageAccessModeResolver;
        this.storageAccessService = storageAccessService;
        this.blobObjectKeyFactory = blobObjectKeyFactory;
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
        String fileName = normalizeFileName(request.getFileName());
        String mimeType = requireMimeType(request.getMimeType());
        FileType fileType = FileType.fromMimeType(mimeType);
        validateUploadRequest(request, groupSettings, fileType);

        String checksum = normalizeChecksum(request.getChecksumSha256());
        String uploadId = newId();
        String fileId = newId();
        StorageWritePlan writePlan = selectWritePlan(groupName);
        String primaryBackend = writePlan.primaryBackend();
        BlobStore primaryBlobStore = requireBlobStore(primaryBackend);
        StorageUploadMode uploadMode = storageAccessModeResolver.resolveUploadMode(request, checksum, primaryBlobStore);
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
            validateUploadedContent(uploadSession, tempUpload, groupSettings);
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

        String checksum = normalizeChecksum(uploadSession.getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Direct uploads require a checksum.");
        }

        BlobStore primaryBlobStore = requireBlobStore(uploadSession.getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(uploadSession.getObjectKey()));
        StorageGroupConfig groupSettings = storageTopology.getGroup(uploadSession.getGroupName());
        if (groupSettings.getMaxSizeBytes() != null && uploadedObject.size() > groupSettings.getMaxSizeBytes()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size exceeds the configured group limit.");
        }
        if (uploadSession.getFileSize() != null && uploadSession.getFileSize() != uploadedObject.size()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size does not match declared size.");
        }

        String actualChecksum = verifyUploadedChecksum(primaryBlobStore, uploadedObject, checksum);
        return finalizeDirectUpload(uploadSession, actualChecksum, uploadedObject);
    }

    @Override
    public StorageDownloadResult resolveDownload(String fileId,
                                                 Long userId) throws IOException {
        return storageAccessService.resolveDownload(fileId, userId);
    }

    private FileStorage finalizeProxyUpload(StorageUploadSessionEntity uploadSession,
                                            TempUpload tempUpload) throws IOException {
        PreparedBlobMaterialization preparedBlob = prepareBlobMaterialization(
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
        PreparedBlobMaterialization preparedBlob = prepareBlobMaterialization(
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

    private PreparedBlobMaterialization prepareBlobMaterialization(String groupName,
                                                                  String mimeType,
                                                                  FileType fileType,
                                                                  long size,
                                                                  String checksum,
                                                                  Path tempPath,
                                                                  String primaryBackend,
                                                                  BlobObject existingUploadedObject) throws IOException {
        StorageGroupConfig groupSettings = storageTopology.getGroup(groupName);
        Optional<StorageBlobEntity> existingBlob = runtimeSettings.deduplicationEnabled()
                ? storageBlobRepository.findByChecksumSha256(checksum)
                : Optional.empty();
        if (existingBlob.isPresent()) {
            StorageBlobEntity blobEntity = existingBlob.get();
            return PreparedBlobMaterialization.existing(
                    blobEntity,
                    size,
                    ensureRequiredPlacements(
                            blobEntity,
                            groupSettings,
                            mimeType,
                            size,
                            checksum,
                            tempPath,
                            existingUploadedObject,
                            primaryBackend,
                            groupName
                    )
            );
        }

        String primaryObjectKey = existingUploadedObject != null
                ? existingUploadedObject.key()
                : blobObjectKeyFactory.createKey(checksum);
        Map<String, String> materializedPlacements = new LinkedHashMap<>();
        try {
            if (existingUploadedObject == null) {
                putTempToBackend(primaryBackend, primaryObjectKey, tempPath, size, mimeType, checksum);
            }
            materializedPlacements.put(primaryBackend, primaryObjectKey);

            if (groupSettings.getPlacementMode() == StorageGroupPlacementMode.MIRROR) {
                materializeMirrorPlacements(
                        materializedPlacements,
                        primaryBackend,
                        primaryObjectKey,
                        resolveMirrorBackends(groupName, primaryBackend),
                        size,
                        mimeType,
                        checksum,
                        tempPath
                );
            }
        } catch (IOException | RuntimeException exception) {
            cleanupMaterializedPlacements(
                    materializedPlacements,
                    existingUploadedObject != null ? primaryBackend : null,
                    existingUploadedObject != null ? primaryObjectKey : null
            );
            throw exception;
        }
        return PreparedBlobMaterialization.newBlob(
                checksum,
                size,
                mimeType,
                fileType,
                primaryBackend,
                primaryObjectKey,
                materializedPlacements
        );
    }

    private FileStorage persistMaterializedUpload(StorageUploadSessionEntity uploadSession,
                                                  PreparedBlobMaterialization preparedBlob) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = persistBlobMaterialization(preparedBlob);
            return createFileEntity(uploadSession, blobEntity, preparedBlob.size());
        }));
    }

    private StorageBlobEntity persistBlobMaterialization(PreparedBlobMaterialization preparedBlob) {
        StorageBlobEntity blobEntity = preparedBlob.existingBlob();
        if (blobEntity == null) {
            OffsetDateTime now = OffsetDateTime.now();
            StorageBlobEntity candidate = StorageBlobEntity.builder()
                    .setBlobId(newId())
                    .setChecksumSha256(preparedBlob.checksum())
                    .setFileSize(preparedBlob.size())
                    .setMimeType(preparedBlob.mimeType())
                    .setFileType(preparedBlob.fileType())
                    .setPrimaryBackend(preparedBlob.primaryBackend())
                    .setPrimaryObjectKey(preparedBlob.primaryObjectKey())
                    .setCreateTime(now)
                    .setUpdateTime(now)
                    .build();
            try {
                blobEntity = storageBlobRepository.save(candidate);
            } catch (DataIntegrityViolationException exception) {
                blobEntity = storageBlobRepository.findByChecksumSha256(preparedBlob.checksum())
                        .orElseThrow(() -> exception);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, String> entry : preparedBlob.placementsToPersist().entrySet()) {
            savePlacement(blobEntity.getBlobId(), entry.getKey(), entry.getValue(), now);
        }
        return blobEntity;
    }

    private Map<String, String> ensureRequiredPlacements(StorageBlobEntity blobEntity,
                                                         StorageGroupConfig groupSettings,
                                                         String mimeType,
                                                         long size,
                                                         String checksum,
                                                         Path tempPath,
                                                         BlobObject existingUploadedObject,
                                                         String primaryBackend,
                                                         String groupName) throws IOException {
        Map<String, String> placementsToPersist = new LinkedHashMap<>();
        Set<String> requiredBackends = new LinkedHashSet<>();
        requiredBackends.add(primaryBackend);
        if (groupSettings.getPlacementMode() == StorageGroupPlacementMode.MIRROR) {
            requiredBackends.addAll(resolveMirrorBackends(groupName, primaryBackend));
        }

        String sourceBackend = existingUploadedObject != null ? primaryBackend : blobEntity.getPrimaryBackend();
        String sourceObjectKey = existingUploadedObject != null ? existingUploadedObject.key() : blobEntity.getPrimaryObjectKey();

        try {
            for (String backendName : requiredBackends) {
                if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobEntity.getBlobId(), backendName).isPresent()) {
                    continue;
                }

                String objectKey = backendName.equals(sourceBackend)
                        ? sourceObjectKey
                        : blobObjectKeyFactory.createKey(checksum);
                if (backendName.equals(sourceBackend)) {
                    placementsToPersist.put(backendName, objectKey);
                    continue;
                }

                if (tempPath != null) {
                    putTempToBackend(backendName, objectKey, tempPath, size, mimeType, checksum);
                    placementsToPersist.put(backendName, objectKey);
                    continue;
                }

                replicateBetweenBackends(sourceBackend, sourceObjectKey, backendName, objectKey, size, mimeType, checksum);
                placementsToPersist.put(backendName, objectKey);
            }
        } catch (IOException | RuntimeException exception) {
            cleanupMaterializedPlacements(placementsToPersist, sourceBackend, sourceObjectKey);
            throw exception;
        }
        return placementsToPersist;
    }

    private void materializeMirrorPlacements(Map<String, String> materializedPlacements,
                                             String primaryBackend,
                                             String primaryObjectKey,
                                             List<String> mirrorBackends,
                                             long size,
                                             String mimeType,
                                             String checksum,
                                             Path tempPath) throws IOException {
        for (String mirrorBackend : mirrorBackends) {
            if (materializedPlacements.containsKey(mirrorBackend)) {
                continue;
            }
            String objectKey = blobObjectKeyFactory.createKey(checksum);
            if (tempPath != null) {
                putTempToBackend(mirrorBackend, objectKey, tempPath, size, mimeType, checksum);
                materializedPlacements.put(mirrorBackend, objectKey);
                continue;
            }
            replicateBetweenBackends(primaryBackend, primaryObjectKey, mirrorBackend, objectKey, size, mimeType, checksum);
            materializedPlacements.put(mirrorBackend, objectKey);
        }
    }

    private void replicateBetweenBackends(String sourceBackend,
                                          String sourceObjectKey,
                                          String targetBackend,
                                          String targetObjectKey,
                                          long size,
                                          String mimeType,
                                          String checksum) throws IOException {
        Path tempFile = Files.createTempFile("lampray-replica-", ".bin");
        try {
            try (OutputStream outputStream = Files.newOutputStream(tempFile, StandardOpenOption.TRUNCATE_EXISTING)) {
                requireBlobStore(sourceBackend).openDownload(sourceObjectKey)
                        .transferTo(outputStream);
            }
            putTempToBackend(targetBackend, targetObjectKey, tempFile, size, mimeType, checksum);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void putTempToBackend(String backendName,
                                  String objectKey,
                                  Path tempPath,
                                  long size,
                                  String mimeType,
                                  String checksumSha256) throws IOException {
        try (InputStream inputStream = Files.newInputStream(Objects.requireNonNull(tempPath))) {
            requireBlobStore(backendName).store(
                    new BlobWriteRequest(
                            objectKey,
                            size,
                            mimeType,
                            buildBlobMetadata(checksumSha256),
                            checksumSha256
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
        FileStorage fileStorage = savedFileEntity.lock();
        notifyMaterializationHooks(fileStorage, blobEntity.getBlobId(), uploadSession.getGroupName(), uploadSession.getOwnerUserId());
        return fileStorage;
    }

    private void savePlacement(String blobId,
                               String backendName,
                                String objectKey,
                                OffsetDateTime now) {
        if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, backendName).isPresent()) {
            return;
        }
        StorageBlobPlacementEntity placementEntity = StorageBlobPlacementEntity.builder()
                .setPlacementId(newId())
                .setBlobId(blobId)
                .setBackendName(backendName)
                .setObjectKey(objectKey)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
        try {
            storageBlobPlacementRepository.save(placementEntity);
        } catch (DataIntegrityViolationException exception) {
            if (storageBlobPlacementRepository.findByBlobIdAndBackendName(blobId, backendName).isPresent()) {
                return;
            }
            throw exception;
        }
    }

    private void validateUploadRequest(StorageUploadRequest request,
                                       StorageGroupConfig groupSettings,
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
                                         TempUpload tempUpload,
                                         StorageGroupConfig groupSettings) {
        if (groupSettings.getMaxSizeBytes() != null && tempUpload.size() > groupSettings.getMaxSizeBytes()) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file size exceeds the configured group limit.");
        }
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
            transactionTemplate.executeWithoutResult(status -> storageUploadSessionRepository.save(uploadSession));
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
                .ifPresent(uploadSession -> {
                    uploadSession.setStatus(UploadSessionStatus.EXPIRED);
                    uploadSession.setUpdateTime(OffsetDateTime.now());
                    storageUploadSessionRepository.save(uploadSession);
                }));
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

    private List<String> resolveMirrorBackends(String groupName,
                                               String primaryBackend) {
        return storageGroupRouter.resolveActiveBackends(groupName).stream()
                .filter(candidate -> !candidate.equals(primaryBackend))
                .toList();
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
                actualChecksum = normalizeChecksum(metadataChecksum);
            }
        }
        if (!StringUtils.hasText(actualChecksum)) {
            actualChecksum = calculateChecksum(blobStore, uploadedObject.key());
        }
        if (!expectedChecksum.equals(actualChecksum)) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT,
                    "Uploaded file checksum does not match declared checksum.");
        }
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

    private void cleanupMaterializedPlacements(Map<String, String> placements,
                                               String protectedBackend,
                                               String protectedObjectKey) {
        for (Map.Entry<String, String> entry : placements.entrySet()) {
            boolean isProtected = protectedBackend != null
                    && protectedBackend.equals(entry.getKey())
                    && Objects.equals(protectedObjectKey, entry.getValue());
            if (isProtected) {
                continue;
            }
            try {
                BlobStore blobStore = requireBlobStore(entry.getKey());
                blobStore.delete(entry.getValue());
            } catch (IOException ignored) {
            }
        }
    }

    private record PreparedBlobMaterialization(StorageBlobEntity existingBlob,
                                               String checksum,
                                               long size,
                                               String mimeType,
                                               FileType fileType,
                                               String primaryBackend,
                                               String primaryObjectKey,
                                               Map<String, String> placementsToPersist) {
        private static PreparedBlobMaterialization existing(StorageBlobEntity blobEntity,
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

        private static PreparedBlobMaterialization newBlob(String checksum,
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
}
