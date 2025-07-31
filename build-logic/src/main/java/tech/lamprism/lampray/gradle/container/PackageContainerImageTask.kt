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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Task for packaging container images as OCI archives
 *
 * @author RollW
 */
abstract class PackageContainerImageTask : BaseContainerTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Package container image as OCI archive"
    }

    @TaskAction
    fun packageImage() {
        val toolManager = getContainerToolManager()
        val preferredTool = getPreferredTool()
        val tool = toolManager.detectAvailableTool(preferredTool)
        val extension = containerExtension.get()

        val outputPath = outputFile.get().asFile.absolutePath
        val context = ContainerBuildContext(
            platform = platform.get(),
            version = version.get(),
            architecture = architecture.get(),
            baseImageName = extension.imageName.get(),
            outputFile = outputPath,
            workingDirectory = buildContext.get().asFile
        )

        logger.lifecycle("Packaging container image with tool: ${toolManager.getToolInfo(tool)}")
        logger.lifecycle("Image: ${context.imageName}")
        logger.lifecycle("Output: $outputPath")


        if (tool.executeSave(context, execOperations)) {
            logger.lifecycle("Successfully packaged container image to: $outputPath")

            // Show file size if exists
            val file = outputFile.get().asFile
            if (file.exists()) {
                val sizeInMB = file.length() / (1024 * 1024)
                logger.lifecycle("Package size: $sizeInMB MB")
            }
            return
        }

        throw ContainerPluginException("Failed to package container image to: $outputPath")
    }
}
