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
import tech.lamprism.lampray.storage.policy.StorageValidationRules;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;
import tech.rollw.common.web.CommonErrorCode;

/**
 * @author RollW
 */
@Component
final class DirectUploadCompletionResolvePlanStep implements WorkflowStep<DirectUploadCompletionWorkflowContext> {
    private static final StorageValidationRules VALIDATION_RULES = StorageValidationRules.INSTANCE;

    private final StorageWritePlanResolver storageWritePlanResolver;

    DirectUploadCompletionResolvePlanStep(StorageWritePlanResolver storageWritePlanResolver) {
        this.storageWritePlanResolver = storageWritePlanResolver;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(DirectUploadCompletionWorkflowContext context) {
        String checksum = VALIDATION_RULES.normalizeChecksum(context.getUploadSession().getChecksumSha256());
        if (checksum == null) {
            throw new StorageException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Direct uploads require a checksum.");
        }
        StorageWritePlan writePlan = storageWritePlanResolver.restore(
                context.getUploadSession().getGroupName(),
                context.getUploadSession().getPrimaryBackend()
        );
        context.getState().setExpectedChecksum(checksum);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }
}
