package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class DirectUploadCompletionResolveUploadedObjectStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private final BlobStoreLocator blobStoreLocator;

    DirectUploadCompletionResolveUploadedObjectStep(BlobStoreLocator blobStoreLocator) {
        this.blobStoreLocator = blobStoreLocator;
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore primaryBlobStore = blobStoreLocator.require(context.getUploadSession().getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(context.getUploadSession().getObjectKey()));
        context.getState().setPrimaryBlobStore(primaryBlobStore);
        context.getState().setUploadedObject(uploadedObject);
    }
}
