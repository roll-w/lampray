package tech.lamprism.lampray.storage.session.workflow;

import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class CreateUploadSessionNormalizeAndValidateRequestStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private static final StorageContentRules CONTENT_RULES = StorageContentRules.INSTANCE;
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        StorageUploadRequest request = context.getRequest();
        StorageGroupConfig groupSettings = Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings");
        String fileName = VALIDATION_RULES.normalizeFileName(request.getFileName());
        String mimeType = CONTENT_RULES.requireMimeType(request.getMimeType());
        FileType fileType = CONTENT_RULES.resolveFileType(mimeType);
        VALIDATION_RULES.validateUploadRequest(request, groupSettings, fileType);

        context.getState().setFileName(fileName);
        context.getState().setMimeType(mimeType);
        context.getState().setFileType(fileType);
        context.getState().setChecksum(VALIDATION_RULES.normalizeChecksum(request.getChecksumSha256()));
    }
}
