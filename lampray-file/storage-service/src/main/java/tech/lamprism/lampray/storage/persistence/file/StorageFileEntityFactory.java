package tech.lamprism.lampray.storage.persistence.file;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.configuration.StorageTopology;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;

import java.time.OffsetDateTime;

@Service
public class StorageFileEntityFactory {
    private final StorageTopology storageTopology;
    private final ResourceIdGenerator resourceIdGenerator;

    public StorageFileEntityFactory(StorageTopology storageTopology,
                                    ResourceIdGenerator resourceIdGenerator) {
        this.storageTopology = storageTopology;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    public StorageFileEntity createSessionFile(StorageUploadSessionEntity uploadSession,
                                               String blobId,
                                               PreparedBlobMaterialization preparedBlob,
                                               OffsetDateTime now) {
        return buildFileEntity(
                uploadSession.getFileId(),
                blobId,
                uploadSession.getGroupName(),
                uploadSession.getOwnerUserId(),
                uploadSession.getFileName(),
                preparedBlob.getSize(),
                preparedBlob.getMimeType(),
                preparedBlob.getFileType(),
                now
        );
    }

    public StorageFileEntity createTrustedFile(String groupName,
                                               String fileName,
                                               String mimeType,
                                               FileType fileType,
                                               Long ownerUserId,
                                               String blobId,
                                               long size,
                                               OffsetDateTime now) {
        return buildFileEntity(
                newId(),
                blobId,
                groupName,
                ownerUserId,
                fileName,
                size,
                mimeType,
                fileType,
                now
        );
    }

    private StorageFileEntity buildFileEntity(String fileId,
                                              String blobId,
                                              String groupName,
                                              Long ownerUserId,
                                              String fileName,
                                              long size,
                                              String mimeType,
                                              FileType fileType,
                                              OffsetDateTime now) {
        return StorageFileEntity.builder()
                .setFileId(fileId)
                .setBlobId(blobId)
                .setGroupName(groupName)
                .setOwnerUserId(ownerUserId)
                .setFileName(fileName)
                .setFileSize(size)
                .setMimeType(mimeType)
                .setFileType(fileType)
                .setVisibility(storageTopology.getGroup(groupName).getVisibility())
                .setCreateTime(now)
                .setUpdateTime(now)
                .build();
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
