package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.persistence.file.workflow.step.PersistSessionUploadWorkflowSteps;

import java.util.List;

@Service
public class PersistSessionUploadWorkflowFactory {
    private final PersistSessionUploadWorkflowSteps persistSessionUploadWorkflowSteps;

    public PersistSessionUploadWorkflowFactory(PersistSessionUploadWorkflowSteps persistSessionUploadWorkflowSteps) {
        this.persistSessionUploadWorkflowSteps = persistSessionUploadWorkflowSteps;
    }

    public PersistSessionUploadWorkflow create() {
        return new PersistSessionUploadWorkflow(List.of(
                persistSessionUploadWorkflowSteps::persistInTransaction,
                persistSessionUploadWorkflowSteps::runPostPersist,
                persistSessionUploadWorkflowSteps::publishResult
        ));
    }
}
