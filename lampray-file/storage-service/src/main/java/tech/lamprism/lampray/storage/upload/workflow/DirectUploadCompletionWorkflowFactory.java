package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.upload.workflow.direct.DirectUploadCompletionWorkflowSteps;

import java.util.List;

@Service
public class DirectUploadCompletionWorkflowFactory {
    private final DirectUploadCompletionWorkflowSteps directUploadCompletionWorkflowSteps;

    public DirectUploadCompletionWorkflowFactory(DirectUploadCompletionWorkflowSteps directUploadCompletionWorkflowSteps) {
        this.directUploadCompletionWorkflowSteps = directUploadCompletionWorkflowSteps;
    }

    public DirectUploadCompletionWorkflow create() {
        return new DirectUploadCompletionWorkflow(List.of(
                directUploadCompletionWorkflowSteps::resolvePlan,
                directUploadCompletionWorkflowSteps::resolveUploadedObject,
                directUploadCompletionWorkflowSteps::validateUploadedObject,
                directUploadCompletionWorkflowSteps::recoverAndVerifyChecksum,
                directUploadCompletionWorkflowSteps::prepareMaterialization,
                directUploadCompletionWorkflowSteps::persistUpload
        ));
    }
}
