package tech.lamprism.lampray.storage.session;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflowContext;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflowFactory;

import java.io.IOException;

@Service
public class StorageUploadSessionCreationService {
    private final CreateUploadSessionWorkflowFactory createUploadSessionWorkflowFactory;

    public StorageUploadSessionCreationService(CreateUploadSessionWorkflowFactory createUploadSessionWorkflowFactory) {
        this.createUploadSessionWorkflowFactory = createUploadSessionWorkflowFactory;
    }

    public StorageUploadSession createUploadSession(StorageUploadRequest request,
                                                    Long userId) throws IOException {
        return createUploadSessionWorkflowFactory.create().execute(new CreateUploadSessionWorkflowContext(request, userId));
    }
}
