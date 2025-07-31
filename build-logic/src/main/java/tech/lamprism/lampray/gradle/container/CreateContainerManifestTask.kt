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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
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

    @get:OutputFile
    abstract val ociManifestFile: RegularFileProperty

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
        val extension = containerExtension.get()
        val architectures = supportedArchitectures.get()

        val context = ContainerBuildContext(
            platform = platform.get(),
            version = versionValue,
            architecture = architecture.get(),
            baseImageName = extension.imageName.get(),
            manifestName = manifestNameValue,
            workingDirectory = buildContext.get().asFile
        )

        logger.lifecycle("Creating multi-architecture manifest with tool: ${toolManager.getToolInfo(tool)}")
        logger.lifecycle("Manifest name: $manifestNameValue")
        logger.lifecycle("Supported architectures: ${architectures.joinToString(", ")}")

        // Build image names for all architectures
        val imageNames = architectures.map { arch ->
            "${extension.imageName.get()}:${versionValue}-${arch}"
        }

        logger.lifecycle("Images to include in manifest: ${imageNames.joinToString(", ")}")

        // Execute manifest creation using container tool
        if (tool.executeManifestCreation(context, imageNames, execOperations)) {
            // Generate OCI-compliant manifest file using user-provided generator
            generateOCIManifestFile(tool, manifestNameValue)

            logger.lifecycle("Successfully created multi-architecture manifest: $manifestNameValue")
            logger.lifecycle("Manifest includes ${imageNames.size} architecture(s): ${architectures.joinToString(", ")}")
            logger.lifecycle("OCI manifest saved to: ${ociManifestFile.get().asFile.absolutePath}")
            return
        }
        throw ContainerPluginException("Failed to create multi-architecture manifest: $manifestNameValue")

    }

    private fun generateOCIManifestFile(tool: ContainerTool, manifestNameValue: String) {
        val ociManifestFileObj = ociManifestFile.get().asFile

        // Ensure parent directory exists
        ociManifestFileObj.parentFile?.mkdirs()

        // Create context for manifest generation
        val context = ContainerBuildContext(
            platform = platform.get(),
            version = version.get(),
            architecture = architecture.get(),
            baseImageName = containerExtension.get().imageName.get(),
            manifestName = manifestNameValue,
            workingDirectory = buildContext.get().asFile
        )

        // Generate OCI manifest using tool's implementation
        val manifestContent = tool.executeGenerateOCIManifest(manifestNameValue, context, execOperations)
        if (manifestContent.isEmpty()) {
            throw ContainerPluginException("Failed to generate OCI manifest content for $manifestNameValue")
        }
        ociManifestFileObj.writeText(manifestContent)
        logger.lifecycle("Generated OCI manifest using ${tool.displayName}")
    }
}
