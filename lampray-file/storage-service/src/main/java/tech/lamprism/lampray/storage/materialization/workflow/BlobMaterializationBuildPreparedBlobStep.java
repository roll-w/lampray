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

package tech.lamprism.lampray.storage.materialization.workflow;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class BlobMaterializationBuildPreparedBlobStep implements WorkflowStep<BlobMaterializationWorkflowContext> {
    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(BlobMaterializationWorkflowContext context) {
        BlobMaterializationRequest request = context.getRequest();
        if (context.getState().getExistingBlob() != null) {
            context.getState().setPreparedBlob(PreparedBlobMaterialization.existing(
                    context.getState().getExistingBlob(),
                    request.size(),
                    request.mimeType(),
                    request.fileType(),
                    context.getState().getMaterializedPlacements()
            ));
            return;
        }
        context.getState().setPreparedBlob(PreparedBlobMaterialization.newBlob(
                request.checksum(),
                request.size(),
                request.mimeType(),
                request.fileType(),
                request.primaryBackend(),
                Objects.requireNonNull(context.getState().getPrimaryObjectKey(), "primaryObjectKey"),
                context.getState().getMaterializedPlacements()
        ));
    }
}
