package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

/**
 * Performs one workflow step against a shared context.
 * @author RollW
 */
@FunctionalInterface
public interface WorkflowStep<C> {
    /**
     * Applies the step to the current workflow context.
     */
    void execute(C context) throws IOException;
}
