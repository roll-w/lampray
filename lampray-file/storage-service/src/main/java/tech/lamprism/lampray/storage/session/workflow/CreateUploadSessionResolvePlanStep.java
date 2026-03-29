package tech.lamprism.lampray.storage.session.workflow;

import org.apache.commons.lang3.StringUtils;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
final class CreateUploadSessionResolvePlanStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageWritePlanResolver storageWritePlanResolver;

    CreateUploadSessionResolvePlanStep(StorageRuntimeConfig runtimeSettings,
                                       StorageWritePlanResolver storageWritePlanResolver) {
        this.runtimeSettings = runtimeSettings;
        this.storageWritePlanResolver = storageWritePlanResolver;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        String groupName = resolveGroupName(context.getRequest().getGroupName());
        StorageWritePlan writePlan = storageWritePlanResolver.select(groupName);
        context.getState().setGroupName(groupName);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }

    private String resolveGroupName(String requestedGroupName) {
        String normalizedGroupName = StringUtils.trimToNull(requestedGroupName);
        return normalizedGroupName != null ? normalizedGroupName : runtimeSettings.getDefaultGroup();
    }
}
