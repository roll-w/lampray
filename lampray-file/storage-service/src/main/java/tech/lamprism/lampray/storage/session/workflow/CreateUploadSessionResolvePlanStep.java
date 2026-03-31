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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tech.lamprism.lampray.storage.configuration.StorageRuntimeConfig;
import tech.lamprism.lampray.storage.routing.StorageWritePlan;
import tech.lamprism.lampray.storage.routing.StorageWritePlanResolver;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

/**
 * @author RollW
 */
@Component
final class CreateUploadSessionResolvePlanStep implements WorkflowStep<CreateUploadSessionWorkflowContext> {
    private final StorageRuntimeConfig runtimeSettings;
    private final StorageWritePlanResolver storageWritePlanResolver;

    CreateUploadSessionResolvePlanStep(StorageRuntimeConfig runtimeSettings,
                                       StorageWritePlanResolver storageWritePlanResolver) {
        this.runtimeSettings = runtimeSettings;
        this.storageWritePlanResolver = storageWritePlanResolver;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void execute(CreateUploadSessionWorkflowContext context) {
        String groupName = resolveGroupName(context.getRequest().getGroupName());
        StorageWritePlan writePlan = storageWritePlanResolver.select(groupName);
        context.getState().setGroupName(groupName);
        context.getState().setWritePlan(writePlan);
        context.getState().setGroupSettings(writePlan.getGroupSettings());
    }

    private String resolveGroupName(String requestedGroupName) {
        String normalizedGroupName = StringUtils.trimToNull(requestedGroupName);
        return normalizedGroupName != null ? normalizedGroupName : runtimeSettings.getDefaultGroup();
    }
}
