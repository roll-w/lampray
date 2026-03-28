package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.backend.BlobStoreRegistry;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.store.BlobStore;

import java.io.IOException;

@Service
public class UploadObjectCleaner {
    private final BlobStoreRegistry blobStoreRegistry;

    public UploadObjectCleaner(BlobStoreRegistry blobStoreRegistry) {
        this.blobStoreRegistry = blobStoreRegistry;
    }

    public boolean cleanup(StorageUploadSessionEntity uploadSession) throws IOException {
        BlobStore blobStore = blobStoreRegistry.find(uploadSession.getPrimaryBackend()).orElse(null);
        if (blobStore == null) {
            return false;
        }
        String objectKey = uploadSession.getObjectKey();
        if (!blobStore.exists(objectKey)) {
            return true;
        }
        return blobStore.delete(objectKey);
    }
}
