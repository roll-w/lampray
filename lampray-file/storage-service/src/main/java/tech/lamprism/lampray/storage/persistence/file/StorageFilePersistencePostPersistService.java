package tech.lamprism.lampray.storage.persistence.file;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.materialization.hook.StorageMaterializationContext;
import tech.lamprism.lampray.storage.materialization.hook.StorageMaterializationHookNotifier;

/**
 * @author RollW
 */
@Service
public class StorageFilePersistencePostPersistService {
    private final StorageMaterializationHookNotifier storageMaterializationHookNotifier;

    public StorageFilePersistencePostPersistService(StorageMaterializationHookNotifier storageMaterializationHookNotifier) {
        this.storageMaterializationHookNotifier = storageMaterializationHookNotifier;
    }

    public void afterSessionUploadPersisted(PersistedMaterialization persistedMaterialization,
                                            String groupName,
                                            Long ownerUserId) {
        notifyHooks(persistedMaterialization.getFileStorage(), persistedMaterialization.getBlobId(), groupName, ownerUserId);
    }

    public void afterTrustedUploadPersisted(PersistedMaterialization persistedMaterialization,
                                            String groupName,
                                            Long ownerUserId) {
        notifyHooks(persistedMaterialization.getFileStorage(), persistedMaterialization.getBlobId(), groupName, ownerUserId);
    }

    private void notifyHooks(FileStorage fileStorage,
                             String blobId,
                             String groupName,
                             Long ownerUserId) {
        StorageMaterializationContext context = new StorageMaterializationContext(fileStorage, blobId, groupName, ownerUserId);
        storageMaterializationHookNotifier.notifyAfterMaterialized(context);
    }
}
