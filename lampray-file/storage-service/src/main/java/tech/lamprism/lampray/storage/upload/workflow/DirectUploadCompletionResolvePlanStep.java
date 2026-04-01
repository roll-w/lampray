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
import tech.lamprism.lampray.storage.routing.StorageGroupRouter;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.DataErrorCode;

/**
 * @author RollW
 */
@Component
final class DirectUploadCompletionResolvePlanStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private final StorageGroupRouter storageGroupRouter;

    DirectUploadCompletionResolvePlanStep(StorageGroupRouter storageGroupRouter) {
        this.storageGroupRouter = storageGroupRouter;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        String checksum = context.getUploadSession().requireChecksum();
        StorageWritePlan writePlan = restoreWritePlan(
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getPrimaryBackend()
        );
        context.getState().setExpectedChecksum(checksum);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }

    private StorageWritePlan restoreWritePlan(String groupName,
                                              String primaryBackend) {
        try {
            return storageGroupRouter.restoreWritePlan(groupName, primaryBackend);
        } catch (IllegalArgumentException exception) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new StorageException(DataErrorCode.ERROR_DATA_NOT_EXIST, exception.getMessage());
        }
    }
}
