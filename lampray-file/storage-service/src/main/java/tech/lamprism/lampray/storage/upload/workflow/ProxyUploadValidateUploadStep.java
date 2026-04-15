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
import tech.lamprism.lampray.storage.checksum.ContentFingerprintProfile;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
@Component
public class ProxyUploadValidateUploadStep implements WorkflowStep<ProxyUploadWorkflowContext> {
    private final ContentFingerprintProfile contentFingerprintProfile;

    ProxyUploadValidateUploadStep(ContentFingerprintProfile contentFingerprintProfile) {
        this.contentFingerprintProfile = contentFingerprintProfile;
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void execute(ProxyUploadWorkflowContext context) {
        context.getUploadSession().validateUploadedContent(
                Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload"),
                Objects.requireNonNull(context.getState().getGroupSettings(), "groupSettings"),
                contentFingerprintProfile
        );
    }
}
