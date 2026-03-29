package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.UploadSessionStatus;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionEntityFactory {
    public StorageUploadSessionEntity createPendingSession(String uploadId,
                                                          String fileId,
                                                          String groupName,
                                                          String fileName,
                                                          Long fileSize,
                                                          String mimeType,
                                                          FileType fileType,
                                                          String checksum,
                                                          Long ownerUserId,
                                                          String primaryBackend,
                                                          String objectKey,
                                                          StorageUploadMode uploadMode,
                                                          OffsetDateTime expiresAt,
                                                          OffsetDateTime now) {
        return StorageUploadSessionEntity.builder()
                .setUploadId(uploadId)
                .setFileId(fileId)
                .setGroupName(groupName)
                .setFileName(fileName)
                .setFileSize(fileSize)
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setChecksumSha256(checksum)
                .setOwnerUserId(ownerUserId)
                .setPrimaryBackend(primaryBackend)
                .setObjectKey(objectKey)
                .setUploadMode(uploadMode)
                .setStatus(UploadSessionStatus.PENDING)
                .setExpiresAt(expiresAt)
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
    }
}
