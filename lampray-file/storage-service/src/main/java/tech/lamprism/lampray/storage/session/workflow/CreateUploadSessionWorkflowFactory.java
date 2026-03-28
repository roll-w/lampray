package tech.lamprism.lampray.storage.session.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.session.workflow.step.CreateUploadSessionWorkflowSteps;

import java.util.List;

@Service
public class CreateUploadSessionWorkflowFactory {
    private final CreateUploadSessionWorkflowSteps createUploadSessionWorkflowSteps;

    public CreateUploadSessionWorkflowFactory(CreateUploadSessionWorkflowSteps createUploadSessionWorkflowSteps) {
        this.createUploadSessionWorkflowSteps = createUploadSessionWorkflowSteps;
    }

    public CreateUploadSessionWorkflow create() {
        return new CreateUploadSessionWorkflow(List.of(
                createUploadSessionWorkflowSteps::resolvePlan,
                createUploadSessionWorkflowSteps::normalizeAndValidateRequest,
                createUploadSessionWorkflowSteps::resolveUploadMode,
                createUploadSessionWorkflowSteps::createDirectUploadIfNeeded,
                createUploadSessionWorkflowSteps::persistUploadSession,
                createUploadSessionWorkflowSteps::buildResult
        ));
    }
}
