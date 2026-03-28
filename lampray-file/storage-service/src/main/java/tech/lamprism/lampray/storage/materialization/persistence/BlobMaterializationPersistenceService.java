package tech.lamprism.lampray.storage.materialization.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.placement.BlobPlacementPersistenceService;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;
import tech.lamprism.lampray.storage.persistence.StorageBlobRepository;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class BlobMaterializationPersistenceService {
    private final StorageBlobRepository storageBlobRepository;
    private final BlobPlacementPersistenceService blobPlacementPersistenceService;
    private final ResourceIdGenerator resourceIdGenerator;

    public BlobMaterializationPersistenceService(StorageBlobRepository storageBlobRepository,
                                                BlobPlacementPersistenceService blobPlacementPersistenceService,
                                                ResourceIdGenerator resourceIdGenerator) {
        this.storageBlobRepository = storageBlobRepository;
        this.blobPlacementPersistenceService = blobPlacementPersistenceService;
        this.resourceIdGenerator = resourceIdGenerator;
    }

    public StorageBlobEntity persist(PreparedBlobMaterialization preparedBlob) {
        StorageBlobEntity blobEntity = preparedBlob.getExistingBlob();
        if (blobEntity == null) {
            OffsetDateTime now = OffsetDateTime.now();
            StorageBlobEntity candidate = StorageBlobEntity.builder()
                    .setBlobId(newBlobId())
                    .setChecksumSha256(preparedBlob.getChecksum())
                    .setFileSize(preparedBlob.getSize())
                    .setMimeType(preparedBlob.getMimeType())
                    .setFileType(preparedBlob.getFileType())
                    .setPrimaryBackend(preparedBlob.getPrimaryBackend())
                    .setPrimaryObjectKey(preparedBlob.getPrimaryObjectKey())
                    .setCreateTime(now)
                    .setUpdateTime(now)
                    .build();
            try {
                blobEntity = storageBlobRepository.save(candidate);
            } catch (DataIntegrityViolationException exception) {
                blobEntity = storageBlobRepository.findByChecksumSha256(preparedBlob.getChecksum())
                        .orElseThrow(() -> exception);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, String> entry : preparedBlob.getPlacementsToPersist().entrySet()) {
            blobPlacementPersistenceService.persistIfAbsent(blobEntity.getBlobId(), entry.getKey(), entry.getValue(), now);
        }
        return blobEntity;
    }

    private String newBlobId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }
}
