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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Task for creating multi-architecture container manifests
 *
 * @author RollW
 */
abstract class CreateContainerManifestTask : BaseContainerTask() {

    @get:Input
    abstract val manifestName: Property<String>

    @get:Input
    abstract val supportedArchitectures: ListProperty<String>

    init {
        description = "Create multi-architecture container manifest"
    }

    @TaskAction
    fun createManifest() {
        val toolManager = getContainerToolManager()
        val preferredTool = getPreferredTool()
        val tool = toolManager.detectAvailableTool(preferredTool)

        val manifestNameValue = manifestName.get()
        val versionValue = version.get()
        val architectures = supportedArchitectures.get()

        val context = ContainerBuildContext(
            platform = platform.get(),
            version = versionValue,
            architecture = architecture.get(),
            manifestName = manifestNameValue
        )

        logger.lifecycle("Creating multi-architecture manifest with tool: ${tool.displayName}")
        logger.lifecycle("Manifest name: $manifestNameValue")
        logger.lifecycle("Supported architectures: ${architectures.joinToString(", ")}")

        // Create manifest
        val createCommand = tool.manifestCreateCommand(context)

        execOperations.exec {
            workingDir = buildContext.get().asFile
            commandLine = createCommand
        }

        // Add images to manifest
        architectures.forEach { arch ->
            val imageName = "lampray:${versionValue}-${arch}"
            logger.lifecycle("Adding image to manifest: $imageName")

            val addCommand = tool.manifestAddCommand(context, imageName)

            try {
                execOperations.exec {
                    workingDir = buildContext.get().asFile
                    commandLine = addCommand
                }
            } catch (e: Exception) {
                logger.warn("Failed to add image $imageName to manifest: ${e.message}")
            }
        }

        logger.lifecycle("Successfully created multi-architecture manifest: $manifestNameValue")
    }
}
