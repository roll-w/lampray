/*
 * Copyright (C) 2023-2025 RollW
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

package tech.lamprism.lampray.gradle.container

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Base task for container operations
 *
 * @author RollW
 */
abstract class BaseContainerTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val architecture: Property<String>

    @get:Input
    abstract val platform: Property<String>

    @get:InputDirectory
    abstract val buildContext: DirectoryProperty

    @get:Nested
    abstract val containerExtension: Property<ContainerExtension>

    @Internal
    protected fun getContainerToolManager(): ContainerToolManager {
        val extension = containerExtension.get()
        return ContainerToolManager(extension.customTools.get())
    }

    @Internal
    protected fun getPreferredTool(): String? {
        val extension = containerExtension.get()
        return extension.preferredTool.orNull
    }

    init {
        group = "container"
        outputs.upToDateWhen { false }
    }
}
