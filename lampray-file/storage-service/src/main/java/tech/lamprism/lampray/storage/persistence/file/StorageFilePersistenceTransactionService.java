package tech.lamprism.lampray.storage.persistence.file;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileEntity;
import tech.lamprism.lampray.storage.persistence.StorageFileRepository;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.session.StorageUploadSessionService;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class StorageFilePersistenceTransactionService {
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFileEntityFactory storageFileEntityFactory;
    private final StorageFileRepository storageFileRepository;
    private final StorageUploadSessionService storageUploadSessionService;
    private final TransactionTemplate transactionTemplate;

    public StorageFilePersistenceTransactionService(StorageBlobMaterializationService storageBlobMaterializationService,
                                                    StorageFileEntityFactory storageFileEntityFactory,
                                                    StorageFileRepository storageFileRepository,
                                                    StorageUploadSessionService storageUploadSessionService,
                                                    PlatformTransactionManager transactionManager) {
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFileEntityFactory = storageFileEntityFactory;
        this.storageFileRepository = storageFileRepository;
        this.storageUploadSessionService = storageUploadSessionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PersistedMaterialization persistSessionUpload(StorageUploadSessionEntity uploadSession,
                                                         PreparedBlobMaterialization preparedBlob) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = storageBlobMaterializationService.persistBlobMaterialization(preparedBlob);
            OffsetDateTime now = OffsetDateTime.now();
            StorageFileEntity fileEntity = storageFileEntityFactory.createSessionFile(
                    uploadSession,
                    blobEntity.getBlobId(),
                    preparedBlob,
                    now
            );
            StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
            storageUploadSessionService.markCompleted(uploadSession, now);
            return new PersistedMaterialization(savedFileEntity.lock(), blobEntity.getBlobId());
        }));
    }

    public PersistedMaterialization persistTrustedUpload(String groupName,
                                                         String fileName,
                                                         String mimeType,
                                                         FileType fileType,
                                                         Long ownerUserId,
                                                         PreparedBlobMaterialization preparedBlob) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            StorageBlobEntity blobEntity = storageBlobMaterializationService.persistBlobMaterialization(preparedBlob);
            OffsetDateTime now = OffsetDateTime.now();
            StorageFileEntity fileEntity = storageFileEntityFactory.createTrustedFile(
                    groupName,
                    fileName,
                    mimeType,
                    fileType,
                    ownerUserId,
                    blobEntity.getBlobId(),
                    preparedBlob.getSize(),
                    now
            );
            StorageFileEntity savedFileEntity = storageFileRepository.save(fileEntity);
            return new PersistedMaterialization(savedFileEntity.lock(), blobEntity.getBlobId());
        }));
    }
}
