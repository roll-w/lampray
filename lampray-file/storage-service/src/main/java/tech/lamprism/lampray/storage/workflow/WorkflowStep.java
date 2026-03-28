package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

@FunctionalInterface
/**
 * Performs one workflow step against a shared context.
 */
public interface WorkflowStep<C> {
    /**
     * Applies the step to the current workflow context.
     */
    void execute(C context) throws IOException;
}
