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
