package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

/**
 * Executes a workflow against a context object.
 */
public interface Workflow<C, R> {
    /**
     * Runs the workflow and returns its result.
     */
    R execute(C context) throws IOException;
}
