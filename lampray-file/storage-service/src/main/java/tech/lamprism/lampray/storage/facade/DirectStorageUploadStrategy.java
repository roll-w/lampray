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

package tech.lamprism.lampray.storage.facade;

import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.FileStorage;
import tech.lamprism.lampray.storage.StorageUploadMode;
import tech.lamprism.lampray.storage.persistence.StorageUploadSessionEntity;
import tech.lamprism.lampray.storage.upload.workflow.DirectUploadCompletionWorkflowContext;
import tech.lamprism.lampray.storage.upload.workflow.DirectUploadCompletionWorkflowFactory;

import java.io.IOException;

@Component
public class DirectStorageUploadStrategy implements StorageUploadStrategy {
    private final DirectUploadCompletionWorkflowFactory directUploadCompletionWorkflowFactory;

    public DirectStorageUploadStrategy(DirectUploadCompletionWorkflowFactory directUploadCompletionWorkflowFactory) {
        this.directUploadCompletionWorkflowFactory = directUploadCompletionWorkflowFactory;
    }

    @Override
    public StorageUploadMode mode() {
        return StorageUploadMode.DIRECT;
    }

    @Override
    public FileStorage completeUpload(StorageUploadSessionEntity uploadSession) throws IOException {
        return directUploadCompletionWorkflowFactory.create().execute(new DirectUploadCompletionWorkflowContext(uploadSession));
    }
}
