package tech.lamprism.lampray.storage.upload.workflow.direct;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.facade.StorageFilePersistenceService;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;
import tech.lamprism.lampray.storage.upload.workflow.DirectUploadCompletionWorkflowContext;
import tech.rollw.common.web.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Service
public class DirectUploadCompletionWorkflowSteps {
    private static final int BUFFER_SIZE = 8192;
    private static final StorageValidationRules validationRules = StorageValidationRules.INSTANCE;

    private final BlobStoreLocator blobStoreLocator;
    private final StorageWritePlanResolver storageWritePlanResolver;
    private final StorageBlobMaterializationService storageBlobMaterializationService;
    private final StorageFilePersistenceService storageFilePersistenceService;

    public DirectUploadCompletionWorkflowSteps(BlobStoreLocator blobStoreLocator,
                                               StorageWritePlanResolver storageWritePlanResolver,
                                               StorageBlobMaterializationService storageBlobMaterializationService,
                                               StorageFilePersistenceService storageFilePersistenceService) {
        this.blobStoreLocator = blobStoreLocator;
        this.storageWritePlanResolver = storageWritePlanResolver;
        this.storageBlobMaterializationService = storageBlobMaterializationService;
        this.storageFilePersistenceService = storageFilePersistenceService;
    }

    public void resolvePlan(DirectUploadCompletionWorkflowContext context) {
        String checksum = validationRules.normalizeChecksum(context.getUploadSession().getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Direct uploads require a checksum.");
        }
        StorageWritePlan writePlan = storageWritePlanResolver.restore(
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getPrimaryBackend()
        );
        context.getState().setExpectedChecksum(checksum);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }

    public void resolveUploadedObject(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore primaryBlobStore = blobStoreLocator.require(context.getUploadSession().getPrimaryBackend());
        BlobObject uploadedObject = primaryBlobStore.describe(Objects.requireNonNull(context.getUploadSession().getObjectKey()));
        context.getState().setPrimaryBlobStore(primaryBlobStore);
        context.getState().setUploadedObject(uploadedObject);
    }

    public void validateUploadedObject(DirectUploadCompletionWorkflowContext context) {
        validationRules.validateUploadedObject(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject"),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings")
        );
    }

    public void recoverAndVerifyChecksum(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore blobStore = Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore");
        BlobObject uploadedObject = Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject");
        String actualChecksum = uploadedObject.getChecksumSha256();
        if (!StringUtils.hasText(actualChecksum)) {
            String metadataChecksum = BlobMetadataSupport.metadataChecksum(uploadedObject.getMetadata());
            if (StringUtils.hasText(metadataChecksum)) {
                actualChecksum = validationRules.normalizeChecksum(metadataChecksum);
            }
        }
        if (!StringUtils.hasText(actualChecksum)) {
            actualChecksum = calculateChecksum(blobStore, uploadedObject.getKey());
        }
        validationRules.validateChecksumMatch(Objects.requireNonNull(context.getState().getExpectedChecksum(), "expectedChecksum"), actualChecksum);
        context.getState().setActualChecksum(actualChecksum);
    }

    public void prepareMaterialization(DirectUploadCompletionWorkflowContext context) throws IOException {
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forUploadedObject(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        context.getUploadSession().getMimeType(),
                        context.getUploadSession().getFileType(),
                        Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject").getSize(),
                        Objects.requireNonNull(context.getState().getActualChecksum(), "actualChecksum"),
                        Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject")
                )
        );
        context.getState().setPreparedBlob(preparedBlob);
    }

    public void persistUpload(DirectUploadCompletionWorkflowContext context) {
        FileStorage fileStorage = storageFilePersistenceService.persistSessionUpload(
                context.getUploadSession(),
                Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
        );
        context.getState().setResult(fileStorage);
    }

    private String calculateChecksum(BlobStore blobStore,
                                     String objectKey) throws IOException {
        Hasher hasher = Hashing.sha256().newHasher();
        try (InputStream inputStream = blobStore.openDownload(objectKey).openStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                hasher.putBytes(buffer, 0, read);
            }
        }
        return hasher.hash().toString();
    }
}
