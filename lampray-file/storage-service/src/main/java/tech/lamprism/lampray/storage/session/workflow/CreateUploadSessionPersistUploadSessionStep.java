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
import tech.lamprism.lampray.storage.StorageUploadSession;
import tech.lamprism.lampray.storage.domain.StorageUploadSessionModel;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionRepository;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class CreateUploadSessionPersistUploadSessionStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageUploadSessionRepository storageUploadSessionRepository;
    private final TransactionTemplate transactionTemplate;

    CreateUploadSessionPersistUploadSessionStep(StorageUploadSessionRepository storageUploadSessionRepository,
                                                PlatformTransactionManager transactionManager) {
        this.storageUploadSessionRepository = storageUploadSessionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        StorageUploadSessionModel uploadSession = Objects.requireNonNull(
                context.getState().getUploadSession(),
                "uploadSession"
        );
        StorageUploadSessionEntity savedUploadSessionEntity = Objects.requireNonNull(
                transactionTemplate.execute(status -> storageUploadSessionRepository.save(uploadSession.getEntity())),
                "savedUploadSessionEntity"
        );
        StorageUploadSessionModel savedUploadSession = StorageUploadSessionModel.from(
                savedUploadSessionEntity,
                uploadSession.getDirectRequest()
        );
        context.getState().setUploadSession(savedUploadSession);
        context.getState().setResult(new StorageUploadSession(
                savedUploadSession.getUploadId(),
                savedUploadSession.getUploadMode(),
                savedUploadSession.getFileName(),
                savedUploadSession.getGroupName(),
                savedUploadSession.getFileId(),
                savedUploadSession.getDirectRequest(),
                savedUploadSession.getExpiresAt()
        ));
    }
}
