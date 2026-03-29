package tech.lamprism.lampray.storage.session.workflow;

import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.session.DirectUploadProvision;
import tech.lamprism.lampray.storage.session.DirectUploadRequestCreator;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
final class CreateUploadSessionCreateDirectUploadIfNeededStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageRuntimeConfig runtimeSettings;
    private final DirectUploadRequestCreator directUploadRequestCreator;

    CreateUploadSessionCreateDirectUploadIfNeededStep(StorageRuntimeConfig runtimeSettings,
                                                      DirectUploadRequestCreator directUploadRequestCreator) {
        this.runtimeSettings = runtimeSettings;
        this.directUploadRequestCreator = directUploadRequestCreator;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) throws IOException {
        if (context.getState().getUploadMode() != StorageUploadMode.DIRECT) {
            return;
        }
        long declaredSize = Objects.requireNonNull(context.getRequest().getSize(), "Direct uploads require a declared size.");
        DirectUploadProvision provision = directUploadRequestCreator.create(
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getPrimaryBackend(), "primaryBackend"),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                context.getState().getChecksum(),
                declaredSize,
                Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore"),
                runtimeSettings.getDirectAccessTtlSeconds()
        );
        context.getState().setObjectKey(provision.getObjectKey());
        context.getState().setDirectRequest(provision.getAccessRequest());
    }
}
