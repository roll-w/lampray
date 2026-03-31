package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflow;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflowContext;

import java.io.IOException;

/**
 * @author RollW
 */
@Service
public class StorageUploadSessionCreationService {
    private final CreateUploadSessionWorkflow createUploadSessionWorkflow;

    public StorageUploadSessionCreationService(CreateUploadSessionWorkflow createUploadSessionWorkflow) {
        this.createUploadSessionWorkflow = createUploadSessionWorkflow;
    }

    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return createUploadSessionWorkflow.execute(new CreateUploadSessionWorkflowContext(request, userId));
    }
}
