/*
 * Copyright (C) 2023-2026 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.storage.session.workflow;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.session.StorageUploadSessionEntityFactory;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
final class CreateUploadSessionPersistUploadSessionStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageUploadSessionEntityFactory storageUploadSessionEntityFactory;
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final TransactionTemplate transactionTemplate;

    CreateUploadSessionPersistUploadSessionStep(StorageUploadSessionEntityFactory storageUploadSessionEntityFactory,
                                                StorageUploadSessionRepository storageUploadSessionRepository,
                                                PlatformTransactionManager transactionManager) {
        this.storageUploadSessionEntityFactory = storageUploadSessionEntityFactory;
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
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

    private StorageUploadSessionEntity save(StorageUploadSessionEntity uploadSessionEntity) {
        return storageUploadSessionRepository.save(uploadSessionEntity);
    }
}
