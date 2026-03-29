package tech.lamprism.lampray.storage.upload.workflow;

import tech.lamprism.lampray.storage.materialization.TempUpload;
import tech.lamprism.lampray.storage.workflow.WorkflowStep;

import java.util.Objects;

/**
 * @author RollW
 */
final class TrustedUploadAssignDefaultFileNameStep implements WorkflowStep<TrustedUploadWorkflowContext> {
    @Override
    public void execute(TrustedUploadWorkflowContext context) {
        TempUpload tempUpload = Objects.requireNonNull(context.getState().getTempUpload(), "tempUpload");
        String checksum = tempUpload.getChecksumSha256();
        String suffix = checksum.length() > 12 ? checksum.substring(0, 12) : checksum;
        context.getState().setFileName("upload-" + suffix + ".bin");
    }
}
