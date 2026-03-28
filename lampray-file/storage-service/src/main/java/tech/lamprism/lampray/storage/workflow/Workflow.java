package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

public interface Workflow<C, R> {
    R execute(C context) throws IOException;
}
