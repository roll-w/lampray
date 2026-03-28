package tech.lamprism.lampray.storage.upload.workflow.proxy;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.materialization.TempUploads;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.upload.workflow.ProxyUploadWorkflowContext;

import java.io.IOException;
import java.util.Objects;

@Service
public class ProxyUploadWorkflowSteps {
    private static final StorageValidationRules validationRules = StorageValidationRules.INSTANCE;

    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFilePersistenceService storageFilePersistenceService;
    private final StorageWritePlanResolver storageWritePlanResolver;
    private final StorageTrafficPublisher storageTrafficPublisher;

    public ProxyUploadWorkflowSteps(StorageBlobMaterializationService storageBlobMaterializationService,
                                    StorageFilePersistenceService storageFilePersistenceService,
                                    StorageWritePlanResolver storageWritePlanResolver,
                                    StorageTrafficPublisher storageTrafficPublisher) {
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFilePersistenceService = storageFilePersistenceService;
        this.storageWritePlanResolver = storageWritePlanResolver;
        this.storageTrafficPublisher = storageTrafficPublisher;
    }

    public void resolvePlan(ProxyUploadWorkflowContext context) {
        StorageWritePlan writePlan = storageWritePlanResolver.restore(
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getPrimaryBackend()
        );
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }

    public void writeTempUpload(ProxyUploadWorkflowContext context) throws IOException {
        context.getState().setTempUpload(TempUploads.write(
                context.getInputStream(),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings").getMaxSizeBytes()
        ));
    }

    public void publishTraffic(ProxyUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        storageTrafficPublisher.publishProxyUpload(context.getUploadSession().getGroupName(), tempUpload.getSize());
    }

    public void validateUpload(ProxyUploadWorkflowContext context) {
        validationRules.validateUploadedContent(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload"),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings")
        );
    }

    public void prepareMaterialization(ProxyUploadWorkflowContext context) throws IOException {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forTempUpload(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        context.getUploadSession().getMimeType(),
                        context.getUploadSession().getFileType(),
                        tempUpload.getSize(),
                        tempUpload.getChecksumSha256(),
                        tempUpload.getPath()
                )
        );
        context.getState().setPreparedBlob(preparedBlob);
    }

    public void persistUpload(ProxyUploadWorkflowContext context) {
        FileStorage fileStorage = storageFilePersistenceService.persistSessionUpload(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
        );
        context.getState().setResult(fileStorage);
    }
}
