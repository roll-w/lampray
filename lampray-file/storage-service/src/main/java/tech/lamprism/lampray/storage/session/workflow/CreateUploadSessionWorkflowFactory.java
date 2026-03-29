package tech.lamprism.lampray.storage.session.workflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tech.lamprism.lampray.common.data.ResourceIdGenerator;
import tech.lamprism.lampray.storage.backend.BlobStoreLocator;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.session.DirectUploadRequestCreator;
import tech.lamprism.lampray.storage.session.StorageUploadSessionEntityFactory;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class CreateUploadSessionWorkflowFactory {
    private final WorkflowStep<CreateUploadSessionWorkflowContext> resolvePlanStep;
    private final WorkflowStep<CreateUploadSessionWorkflowContext> normalizeAndValidateRequestStep;
    private final WorkflowStep<CreateUploadSessionWorkflowContext> resolveUploadModeStep;
    private final WorkflowStep<CreateUploadSessionWorkflowContext> createDirectUploadIfNeededStep;
    private final WorkflowStep<CreateUploadSessionWorkflowContext> persistUploadSessionStep;
    private final WorkflowStep<CreateUploadSessionWorkflowContext> buildResultStep;

    public CreateUploadSessionWorkflowFactory(StorageRuntimeConfig runtimeSettings,
                                              BlobStoreLocator blobStoreLocator,
                                              ResourceIdGenerator resourceIdGenerator,
                                              StorageWritePlanResolver storageWritePlanResolver,
                                              DirectUploadRequestCreator directUploadRequestCreator,
                                              StorageUploadSessionEntityFactory storageUploadSessionEntityFactory,
                                              StorageUploadSessionRepository storageUploadSessionRepository,
                                              PlatformTransactionManager transactionManager) {
        this.resolvePlanStep = new CreateUploadSessionResolvePlanStep(runtimeSettings, storageWritePlanResolver);
        this.normalizeAndValidateRequestStep = new CreateUploadSessionNormalizeAndValidateRequestStep();
        this.resolveUploadModeStep = new CreateUploadSessionResolveUploadModeStep(
                runtimeSettings,
                blobStoreLocator,
                resourceIdGenerator
        );
        this.createDirectUploadIfNeededStep = new CreateUploadSessionCreateDirectUploadIfNeededStep(runtimeSettings, directUploadRequestCreator);
        this.persistUploadSessionStep = new CreateUploadSessionPersistUploadSessionStep(
                storageUploadSessionEntityFactory,
                storageUploadSessionRepository,
                transactionManager
        );
        this.buildResultStep = new CreateUploadSessionBuildResultStep();
    }

    public CreateUploadSessionWorkflow create() {
        return new CreateUploadSessionWorkflow(List.of(
                resolvePlanStep,
                normalizeAndValidateRequestStep,
                resolveUploadModeStep,
                createDirectUploadIfNeededStep,
                persistUploadSessionStep,
                buildResultStep
        ));
    }
}
