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

package tech.lamprism.lampray.storage.upload.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.file.workflow.PersistSessionUploadWorkflow;
import tech.lamprism.lampray.storage.file.workflow.PersistSessionUploadWorkflowContext;
import tech.lamprism.lampray.storage.session.UploadObjectCleaner;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class DirectUploadCompletionPersistUploadStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final Logger logger = LoggerFactory.getLogger(DirectUploadCompletionPersistUploadStep.class);

    private final PersistSessionUploadWorkflow persistSessionUploadWorkflow;
    private final UploadObjectCleaner uploadObjectCleaner;

    DirectUploadCompletionPersistUploadStep(PersistSessionUploadWorkflow persistSessionUploadWorkflow,
                                           UploadObjectCleaner uploadObjectCleaner) {
        this.persistSessionUploadWorkflow = persistSessionUploadWorkflow;
        this.uploadObjectCleaner = uploadObjectCleaner;
    }

    @Override
    public int getOrder() {
        return 600;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        FileStorage fileStorage = persistSessionUploadWorkflow.execute(
                new PersistSessionUploadWorkflowContext(
                        context.getUploadSession(),
                        Objects.requireNonNull(context.getState().getPreparedBlob(), "preparedBlob")
                )
        );
        cleanupUnusedUploadedObject(context);
        context.getState().setResult(fileStorage);
    }

    private void cleanupUnusedUploadedObject(DirectUploadCompletionWorkflowContext context) {
        try {
            uploadObjectCleaner.cleanup(context.getUploadSession());
        } catch (IOException exception) {
            logger.warn(
                    "Failed to cleanup unused direct upload object for session {}",
                    context.getUploadSession().getUploadId(),
                    exception
            );
        }
    }
}
