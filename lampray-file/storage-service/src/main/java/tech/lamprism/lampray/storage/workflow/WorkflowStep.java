package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

@FunctionalInterface
public interface WorkflowStep<C> {
    void execute(C context) throws IOException;
}
