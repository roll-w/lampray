package tech.lamprism.lampray.storage.persistence.file.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.persistence.file.workflow.step.PersistTrustedUploadWorkflowSteps;

import java.util.List;

@Service
public class PersistTrustedUploadWorkflowFactory {
    private final PersistTrustedUploadWorkflowSteps persistTrustedUploadWorkflowSteps;

    public PersistTrustedUploadWorkflowFactory(PersistTrustedUploadWorkflowSteps persistTrustedUploadWorkflowSteps) {
        this.persistTrustedUploadWorkflowSteps = persistTrustedUploadWorkflowSteps;
    }

    public PersistTrustedUploadWorkflow create() {
        return new PersistTrustedUploadWorkflow(List.of(
                persistTrustedUploadWorkflowSteps::persistInTransaction,
                persistTrustedUploadWorkflowSteps::runPostPersist,
                persistTrustedUploadWorkflowSteps::publishResult
        ));
    }
}
