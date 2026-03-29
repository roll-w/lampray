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

package tech.lamprism.lampray.storage.workflow;

import java.io.IOException;

/**
 * Performs one workflow step against a shared context.
 *
 * @author RollW
 */
@FunctionalInterface
public interface WorkflowStep<C> {
    /**
     * Applies the step to the current workflow context.
     */
    void execute(C context) throws IOException;
}
