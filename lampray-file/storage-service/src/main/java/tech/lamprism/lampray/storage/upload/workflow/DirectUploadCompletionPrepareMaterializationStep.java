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

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.workflow.BlobMaterializationWorkflow;
import tech.lamprism.lampray.storage.materialization.workflow.BlobMaterializationWorkflowContext;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class DirectUploadCompletionPrepareMaterializationStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private final BlobMaterializationWorkflow blobMaterializationWorkflow;

    DirectUploadCompletionPrepareMaterializationStep(BlobMaterializationWorkflow blobMaterializationWorkflow) {
        this.blobMaterializationWorkflow = blobMaterializationWorkflow;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) throws IOException {
        PreparedBlobMaterialization preparedBlob = blobMaterializationWorkflow.execute(new BlobMaterializationWorkflowContext(
                BlobMaterializationRequest.forUploadedObject(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        context.getUploadSession().getMimeType(),
                        context.getUploadSession().getFileType(),
                        Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject").getSize(),
                        Objects.requireNonNull(context.getState().getActualChecksum(), "actualChecksum"),
                        Objects.requireNonNull(context.getState().getUploadedObject(), "uploadedObject")
                )
        ));
        context.getState().setPreparedBlob(preparedBlob);
    }
}
