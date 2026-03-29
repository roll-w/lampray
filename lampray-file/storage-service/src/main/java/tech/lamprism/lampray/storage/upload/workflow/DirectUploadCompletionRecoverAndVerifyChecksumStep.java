package tech.lamprism.lampray.storage.upload.workflow;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.springframework.util.StringUtils;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.store.BlobObject;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.support.BlobMetadataSupport;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author RollW
 */
final class DirectUploadCompletionRecoverAndVerifyChecksumStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final int BUFFER_SIZE = 8192;
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        BlobStore blobStore = Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore");
        BlobObject uploadedObject = Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject");
        String actualChecksum = uploadedObject.getChecksumSha256();
        if (!StringUtils.hasText(actualChecksum)) {
            String metadataChecksum = BlobMetadataSupport.metadataChecksum(uploadedObject.getMetadata());
            if (StringUtils.hasText(metadataChecksum)) {
                actualChecksum = VALIDATION_RULES.normalizeChecksum(metadataChecksum);
            }
        }
        if (!StringUtils.hasText(actualChecksum)) {
            actualChecksum = calculateChecksum(blobStore, uploadedObject.getKey());
        }
        VALIDATION_RULES.validateChecksumMatch(Objects.requireNonNull(context.getState().getExpectedChecksum(), "expectedChecksum"), actualChecksum);
        context.getState().setActualChecksum(actualChecksum);
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
