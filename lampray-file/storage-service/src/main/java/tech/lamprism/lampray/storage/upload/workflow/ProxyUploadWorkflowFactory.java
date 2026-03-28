package tech.lamprism.lampray.storage.upload.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.upload.workflow.proxy.ProxyUploadWorkflowSteps;

import java.util.List;

@Service
public class ProxyUploadWorkflowFactory {
    private final ProxyUploadWorkflowSteps proxyUploadWorkflowSteps;

    public ProxyUploadWorkflowFactory(ProxyUploadWorkflowSteps proxyUploadWorkflowSteps) {
        this.proxyUploadWorkflowSteps = proxyUploadWorkflowSteps;
    }

    public ProxyUploadWorkflow create() {
        return new ProxyUploadWorkflow(List.of(
                proxyUploadWorkflowSteps::resolvePlan,
                proxyUploadWorkflowSteps::writeTempUpload,
                proxyUploadWorkflowSteps::publishTraffic,
                proxyUploadWorkflowSteps::validateUpload,
                proxyUploadWorkflowSteps::prepareMaterialization,
                proxyUploadWorkflowSteps::persistUpload
        ));
    }
}
