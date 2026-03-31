package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;

/**
 * @author RollW
 */
@Component
final class DirectUploadCompletionResolvePlanStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    private final StorageWritePlanResolver storageWritePlanResolver;

    DirectUploadCompletionResolvePlanStep(StorageWritePlanResolver storageWritePlanResolver) {
        this.storageWritePlanResolver = storageWritePlanResolver;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        String checksum = VALIDATION_RULES.normalizeChecksum(context.getUploadSession().getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Direct uploads require a checksum.");
        }
        StorageWritePlan writePlan = storageWritePlanResolver.restore(
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getPrimaryBackend()
        );
        context.getState().setExpectedChecksum(checksum);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }
}
