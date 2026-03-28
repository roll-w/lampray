package tech.lamprism.lampray.storage.upload.workflow.trusted;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.materialization.TempUploads;
import tech.lamprism.lampray.storage.monitoring.StorageTrafficPublisher;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.upload.workflow.TrustedUploadWorkflowContext;

import java.io.IOException;
import java.util.Objects;

@Service
public class TrustedUploadWorkflowSteps {
    private static final StorageContentRules contentRules = StorageContentRules.INSTANCE;
    private static final StorageValidationRules validationRules = StorageValidationRules.INSTANCE;

    private final StorageWritePlanResolver storageWritePlanResolver;
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageTrafficPublisher storageTrafficPublisher;
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFilePersistenceService storageFilePersistenceService;

    public TrustedUploadWorkflowSteps(StorageWritePlanResolver storageWritePlanResolver,
                                      StorageRuntimeConfig runtimeSettings,
                                      StorageTrafficPublisher storageTrafficPublisher,
                                      StorageBlobMaterializationService storageBlobMaterializationService,
                                      StorageFilePersistenceService storageFilePersistenceService) {
        this.storageWritePlanResolver = storageWritePlanResolver;
        this.runtimeSettings = runtimeSettings;
        this.storageTrafficPublisher = storageTrafficPublisher;
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFilePersistenceService = storageFilePersistenceService;
    }

    public void resolvePlan(TrustedUploadWorkflowContext context) {
        String groupName = runtimeSettings.getDefaultGroup();
        StorageWritePlan writePlan = storageWritePlanResolver.select(groupName);
        context.getState().setGroupName(groupName);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
        String mimeType = contentRules.requireMimeType("application/octet-stream");
        context.getState().setMimeType(mimeType);
        context.getState().setFileType(contentRules.resolveFileType(mimeType));
    }

    public void writeTempUpload(TrustedUploadWorkflowContext context) throws IOException {
        StorageGroupConfig groupSettings = Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings");
        context.getState().setTempUpload(TempUploads.write(context.getInputStream(), groupSettings.getMaxSizeBytes()));
    }

    public void assignDefaultFileName(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        String checksum = tempUpload.getChecksumSha256();
        String suffix = checksum.length() > 12 ? checksum.substring(0, 12) : checksum;
        context.getState().setFileName("upload-" + suffix + ".bin");
    }

    public void publishTraffic(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        storageTrafficPublisher.publishProxyUpload(Objects.requireNonNull(context.getState().getGroupName(), "groupName"), tempUpload.getSize());
    }

    public void validateUpload(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        validationRules.validateUploadRequest(
                new StorageUploadRequest(
                        Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                        Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                        tempUpload.getSize(),
                        Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                        tempUpload.getChecksumSha256()
                ),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType")
        );
    }

    public void prepareMaterialization(TrustedUploadWorkflowContext context) throws IOException {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forTempUpload(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                        Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                        tempUpload.getSize(),
                        tempUpload.getChecksumSha256(),
                        tempUpload.getPath()
                )
        );
        context.getState().setPreparedBlob(preparedBlob);
    }

    public void persistUpload(TrustedUploadWorkflowContext context) {
        FileStorage fileStorage = storageFilePersistenceService.persistTrustedUpload(
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                null,
                Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
        );
        context.getState().setResult(fileStorage);
    }
}
