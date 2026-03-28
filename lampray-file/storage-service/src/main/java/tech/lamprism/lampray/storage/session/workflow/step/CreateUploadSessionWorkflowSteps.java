package tech.lamprism.lampray.storage.session.workflow.step;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.FileType;
import tech.lamprism.lampray.storage.StorageAccessRequest;
import tech.lamprism.lampray.storage.StorageResourceKind;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.StorageUploadRequest;
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageGroupConfig;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.policy.StorageTransferModeResolver;
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.store.BlobStore;
import tech.lamprism.lampray.storage.session.DirectUploadProvision;
import tech.lamprism.lampray.storage.session.DirectUploadRequestCreator;
import tech.lamprism.lampray.storage.session.StorageUploadSessionEntityFactory;
import tech.lamprism.lampray.storage.session.workflow.CreateUploadSessionWorkflowContext;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class CreateUploadSessionWorkflowSteps {
    private static final StorageContentRules contentRules = StorageContentRules.INSTANCE;
    private static final StorageValidationRules validationRules = StorageValidationRules.INSTANCE;

    private final StorageRuntimeConfig runtimeSettings;
    private final BlobStoreLocator blobStoreLocator;
    private final ResourceIdGenerator resourceIdGenerator;
    private final StorageWritePlanResolver storageWritePlanResolver;
    private final StorageTransferModeResolver transferModeResolver;
    private final DirectUploadRequestCreator directUploadRequestCreator;
    private final StorageUploadSessionEntityFactory storageUploadSessionEntityFactory;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final TransactionTemplate transactionTemplate;

    public CreateUploadSessionWorkflowSteps(StorageRuntimeConfig runtimeSettings,
                                            BlobStoreLocator blobStoreLocator,
                                            ResourceIdGenerator resourceIdGenerator,
                                            StorageWritePlanResolver storageWritePlanResolver,
                                            DirectUploadRequestCreator directUploadRequestCreator,
                                            StorageUploadSessionEntityFactory storageUploadSessionEntityFactory,
                                            StorageUploadSessionRepository storageUploadSessionRepository,
                                            PlatformTransactionManager transactionManager) {
        this.runtimeSettings = runtimeSettings;
        this.blobStoreLocator = blobStoreLocator;
        this.resourceIdGenerator = resourceIdGenerator;
        this.storageWritePlanResolver = storageWritePlanResolver;
        this.transferModeResolver = new StorageTransferModeResolver(runtimeSettings);
        this.directUploadRequestCreator = directUploadRequestCreator;
        this.storageUploadSessionEntityFactory = storageUploadSessionEntityFactory;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void resolvePlan(CreateUploadSessionWorkflowContext context) {
        String groupName = resolveGroupName(context.getRequest().getGroupName());
        StorageWritePlan writePlan = storageWritePlanResolver.select(groupName);
        context.getState().setGroupName(groupName);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }

    public void normalizeAndValidateRequest(CreateUploadSessionWorkflowContext context) {
        StorageUploadRequest request = context.getRequest();
        StorageGroupConfig groupSettings = Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings");
        String fileName = validationRules.normalizeFileName(request.getFileName());
        String mimeType = contentRules.requireMimeType(request.getMimeType());
        FileType fileType = contentRules.resolveFileType(mimeType);
        validationRules.validateUploadRequest(request, groupSettings, fileType);

        context.getState().setFileName(fileName);
        context.getState().setMimeType(mimeType);
        context.getState().setFileType(fileType);
        context.getState().setChecksum(validationRules.normalizeChecksum(request.getChecksumSha256()));
    }

    public void resolveUploadMode(CreateUploadSessionWorkflowContext context) {
        String uploadId = newId();
        String fileId = newId();
        String primaryBackend = Objects.requireNonNull(context.getState().getWritePlan(), "writePlan").getPrimaryBackend();
        BlobStore primaryBlobStore = blobStoreLocator.require(primaryBackend);
        StorageUploadMode uploadMode = transferModeResolver.resolveUploadMode(
                context.getRequest(),
                context.getState().getChecksum(),
                primaryBlobStore
        );
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(runtimeSettings.getPendingUploadExpireSeconds());

        context.getState().setUploadId(uploadId);
        context.getState().setFileId(fileId);
        context.getState().setPrimaryBackend(primaryBackend);
        context.getState().setPrimaryBlobStore(primaryBlobStore);
        context.getState().setUploadMode(uploadMode);
        context.getState().setNow(now);
        context.getState().setExpiresAt(expiresAt);
    }

    public void createDirectUploadIfNeeded(CreateUploadSessionWorkflowContext context) throws java.io.IOException {
        if (context.getState().getUploadMode() != StorageUploadMode.DIRECT) {
            return;
        }
        long declaredSize = Objects.requireNonNull(context.getRequest().getSize(), "Direct uploads require a declared size.");
        DirectUploadProvision provision = directUploadRequestCreator.create(
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getPrimaryBackend(), "primaryBackend"),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                context.getState().getChecksum(),
                declaredSize,
                Objects.requireNonNull(context.getState().getPrimaryBlobStore(), "primaryBlobStore"),
                runtimeSettings.getDirectAccessTtlSeconds()
        );
        context.getState().setObjectKey(provision.getObjectKey());
        context.getState().setDirectRequest(provision.getAccessRequest());
    }

    public void persistUploadSession(CreateUploadSessionWorkflowContext context) {
        StorageUploadSessionEntity uploadSessionEntity = storageUploadSessionEntityFactory.createPendingSession(
                Objects.requireNonNull(context.getState().getUploadId(), "uploadId"),
                Objects.requireNonNull(context.getState().getFileId(), "fileId"),
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                context.getRequest().getSize(),
                Objects.requireNonNull(context.getState().getMimeType(), "mimeType"),
                Objects.requireNonNull(context.getState().getFileType(), "fileType"),
                context.getState().getChecksum(),
                context.getUserId(),
                Objects.requireNonNull(context.getState().getPrimaryBackend(), "primaryBackend"),
                context.getState().getObjectKey(),
                Objects.requireNonNull(context.getState().getUploadMode(), "uploadMode"),
                Objects.requireNonNull(context.getState().getExpiresAt(), "expiresAt"),
                Objects.requireNonNull(context.getState().getNow(), "now")
        );
        transactionTemplate.executeWithoutResult(status -> context.getState().setUploadSessionEntity(save(uploadSessionEntity)));
    }

    public void buildResult(CreateUploadSessionWorkflowContext context) {
        context.getState().setResult(new StorageUploadSession(
                Objects.requireNonNull(context.getState().getUploadId(), "uploadId"),
                Objects.requireNonNull(context.getState().getUploadMode(), "uploadMode"),
                Objects.requireNonNull(context.getState().getFileName(), "fileName"),
                Objects.requireNonNull(context.getState().getGroupName(), "groupName"),
                Objects.requireNonNull(context.getState().getFileId(), "fileId"),
                context.getState().getDirectRequest(),
                Objects.requireNonNull(context.getState().getExpiresAt(), "expiresAt")
        ));
    }

    private String resolveGroupName(String requestedGroupName) {
        String normalizedGroupName = StringUtils.trimToNull(requestedGroupName);
        return normalizedGroupName != null ? normalizedGroupName : runtimeSettings.getDefaultGroup();
    }

    private String newId() {
        return resourceIdGenerator.nextId(StorageResourceKind.INSTANCE);
    }

    private StorageUploadSessionEntity save(StorageUploadSessionEntity uploadSessionEntity) {
        return storageUploadSessionRepository.save(uploadSessionEntity);
    }
}
