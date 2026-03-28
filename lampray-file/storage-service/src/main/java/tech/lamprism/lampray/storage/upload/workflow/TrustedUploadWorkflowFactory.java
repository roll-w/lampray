package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.upload.workflow.trusted.TrustedUploadWorkflowSteps;

import java.util.List;

@Service
public class TrustedUploadWorkflowFactory {
    private final TrustedUploadWorkflowSteps trustedUploadWorkflowSteps;

    public TrustedUploadWorkflowFactory(TrustedUploadWorkflowSteps trustedUploadWorkflowSteps) {
        this.trustedUploadWorkflowSteps = trustedUploadWorkflowSteps;
    }

    public TrustedUploadWorkflow create() {
        return new TrustedUploadWorkflow(List.of(
                trustedUploadWorkflowSteps::resolvePlan,
                trustedUploadWorkflowSteps::writeTempUpload,
                trustedUploadWorkflowSteps::assignDefaultFileName,
                trustedUploadWorkflowSteps::publishTraffic,
                trustedUploadWorkflowSteps::validateUpload,
                trustedUploadWorkflowSteps::prepareMaterialization,
                trustedUploadWorkflowSteps::persistUpload
        ));
    }
}
