package tech.lamprism.lampray.storage.materialization.hook;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class StorageMaterializationHookNotifier {
    private final List<StorageMaterializationHook> storageMaterializationHooks;

    public StorageMaterializationHookNotifier(List<StorageMaterializationHook> storageMaterializationHooks) {
        this.storageMaterializationHooks = List.copyOf(storageMaterializationHooks);
    }

    public void notifyAfterMaterialized(StorageMaterializationContext context) {
        for (StorageMaterializationHook storageMaterializationHook : storageMaterializationHooks) {
            storageMaterializationHook.afterMaterialized(context);
        }
    }
}
