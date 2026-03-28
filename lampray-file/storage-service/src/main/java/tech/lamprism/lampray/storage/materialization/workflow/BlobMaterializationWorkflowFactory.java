package tech.lamprism.lampray.storage.materialization.workflow;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.materialization.workflow.step.BlobMaterializationWorkflowSteps;

import java.util.List;

@Service
public class BlobMaterializationWorkflowFactory {
    private final BlobMaterializationWorkflowSteps blobMaterializationWorkflowSteps;

    public BlobMaterializationWorkflowFactory(BlobMaterializationWorkflowSteps blobMaterializationWorkflowSteps) {
        this.blobMaterializationWorkflowSteps = blobMaterializationWorkflowSteps;
    }

    public BlobMaterializationWorkflow create() {
        return new BlobMaterializationWorkflow(List.of(
                blobMaterializationWorkflowSteps::resolveSource,
                blobMaterializationWorkflowSteps::resolveExistingBlob,
                blobMaterializationWorkflowSteps::preparePlacements,
                blobMaterializationWorkflowSteps::buildPreparedBlob
        ));
    }
}
