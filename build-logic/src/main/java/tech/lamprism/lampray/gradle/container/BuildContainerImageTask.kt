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

import org.gradle.api.tasks.TaskAction

/**
 * Task for building OCI-compliant container images
 *
 * @author RollW
 */
abstract class BuildContainerImageTask : BaseContainerTask() {

    init {
        description = "Build OCI-compliant container image"
    }

    @TaskAction
    fun buildImage() {
        val toolManager = getContainerToolManager()
        val preferredTool = getPreferredTool()
        val tool = toolManager.detectAvailableTool(preferredTool)
        val extension = containerExtension.get()

        val context = ContainerBuildContext(
            platform = platform.get(),
            version = version.get(),
            architecture = architecture.get(),
            baseImageName = extension.imageName.get(),
            workingDirectory = buildContext.get().asFile,
            environment = mapOf(
                "LAMPRAY_VERSION" to version.get(),
                "TARGET_ARCH" to architecture.get(),
                "TARGET_PLATFORM" to platform.get()
            )
        )

        logger.lifecycle("Building container image with tool: ${toolManager.getToolInfo(tool)}")
        logger.lifecycle("Target platform: ${context.platform}")
        logger.lifecycle("Target architecture: ${context.architecture}")

        // Prepare build context
        prepareBuildContext()

        if (tool.executeBuild(context, execOperations)) {
            logger.lifecycle("Successfully built container image: ${context.imageName}")
            return
        }
        throw ContainerPluginException("Failed to build container image: ${context.imageName}")
    }

    private fun prepareBuildContext() {
        val contextDir = buildContext.get().asFile
        val extension = containerExtension.get()

        // Ensure build context directory exists
        if (!contextDir.exists()) {
            contextDir.mkdirs()
        }

        // Copy Containerfile to build context
        val containerFile = extension.containerFile.get().asFile
        if (containerFile.exists()) {
            containerFile.copyTo(contextDir.resolve("Containerfile"), overwrite = true)
            logger.info("Copied Containerfile from ${containerFile.absolutePath}")
        } else {
            throw IllegalStateException("Containerfile not found at ${containerFile.absolutePath}")
        }
    }
}
