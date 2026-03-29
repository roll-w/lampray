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

import tech.lamprism.lampray.storage.materialization.BlobMaterializationRequest;
import tech.lamprism.lampray.storage.materialization.PreparedBlobMaterialization;
import tech.lamprism.lampray.storage.materialization.StorageBlobMaterializationService;
import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.io.IOException;
import java.util.Objects;

/**
 * @author RollW
 */
final class ProxyUploadPrepareMaterializationStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private final StorageBlobMaterializationService storageBlobMaterializationService;

    ProxyUploadPrepareMaterializationStep(StorageBlobMaterializationService storageBlobMaterializationService) {
        this.storageBlobMaterializationService = storageBlobMaterializationService;
    }

    @Override
    public void execute(ProxyUploadWorkflowContext context) throws IOException {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        PreparedBlobMaterialization preparedBlob = storageBlobMaterializationService.prepareBlobMaterialization(
                BlobMaterializationRequest.forTempUpload(
                        Objects.requireNonNull(context.getState().getWritePlan(), "writePlan"),
                        context.getUploadSession().getMimeType(),
                        context.getUploadSession().getFileType(),
                        tempUpload.getSize(),
                        tempUpload.getChecksumSha256(),
                        tempUpload.getPath()
                )
        );
        context.getState().setPreparedBlob(preparedBlob);
    }
}
