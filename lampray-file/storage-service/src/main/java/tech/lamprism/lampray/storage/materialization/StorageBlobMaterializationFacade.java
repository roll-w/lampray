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

package tech.lamprism.lampray.storage.materialization;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.storage.materialization.persistence.BlobMaterializationPersistenceService;
import tech.lamprism.lampray.storage.materialization.workflow.BlobMaterializationWorkflow;
import tech.lamprism.lampray.storage.materialization.workflow.BlobMaterializationWorkflowContext;
import tech.lamprism.lampray.storage.persistence.StorageBlobEntity;

/**
 * @author RollW
 */
@Service
public class StorageBlobMaterializationFacade implements StorageBlobMaterializationService {
    private final BlobMaterializationWorkflow blobMaterializationWorkflow;
    private final BlobMaterializationPersistenceService blobMaterializationPersistenceService;

    public StorageBlobMaterializationFacade(BlobMaterializationWorkflow blobMaterializationWorkflow,
                                            BlobMaterializationPersistenceService blobMaterializationPersistenceService) {
        this.blobMaterializationWorkflow = blobMaterializationWorkflow;
        this.blobMaterializationPersistenceService = blobMaterializationPersistenceService;
    }

    @Override
    public PreparedBlobMaterialization prepareBlobMaterialization(BlobMaterializationRequest request) throws java.io.IOException {
        return blobMaterializationWorkflow.execute(new BlobMaterializationWorkflowContext(request));
    }

    @Override
    public StorageBlobEntity persistBlobMaterialization(PreparedBlobMaterialization preparedBlob) {
        return blobMaterializationPersistenceService.persist(preparedBlob);
    }
}
