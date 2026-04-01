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
import tech.lamprism.lampray.storage.StorageException;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.policy.StorageContentRules;
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

/**
 * @author RollW
 */
@Component
final class TrustedUploadResolvePlanStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    private static final StorageContentRules CONTENT_RULES = StorageContentRules.INSTANCE;

    private final StorageGroupRouter storageGroupRouter;
    private final StorageRuntimeConfig runtimeSettings;

    TrustedUploadResolvePlanStep(StorageGroupRouter storageGroupRouter,
                                 StorageRuntimeConfig runtimeSettings) {
        this.storageGroupRouter = storageGroupRouter;
        this.runtimeSettings = runtimeSettings;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        String groupName = runtimeSettings.getDefaultGroup();
        StorageWritePlan writePlan = selectWritePlan(groupName);
        context.getState().setGroupName(groupName);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
        String mimeType = CONTENT_RULES.requireMimeType("application/octet-stream");
        context.getState().setMimeType(mimeType);
        context.getState().setFileType(CONTENT_RULES.resolveFileType(mimeType));
    }

    private StorageWritePlan selectWritePlan(String groupName) {
        try {
            return storageGroupRouter.selectWritePlan(groupName);
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, exception.getMessage());
        }
    }
}
