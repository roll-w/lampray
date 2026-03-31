package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

/**
 * Performs one workflow step against a shared context.
 * @author RollW
 */
public interface WorkflowStep<C> {
    int getOrder();

    /**
     * Applies the step to the current workflow context.
     */
    void execute(C context) throws IOException;
}
