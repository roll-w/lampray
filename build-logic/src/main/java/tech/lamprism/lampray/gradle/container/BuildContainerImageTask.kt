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

        val context = ContainerBuildContext(
            platform = platform.get(),
            version = version.get(),
            architecture = architecture.get()
        )

        logger.lifecycle("Building container image with tool: ${tool.displayName}")
        logger.lifecycle("Target platform: ${context.platform}")
        logger.lifecycle("Target architecture: ${context.architecture}")

        // Prepare build context
        prepareBuildContext()

        val buildCommand = tool.buildCommand(context)

        execOperations.exec {
            workingDir = buildContext.get().asFile
            commandLine = buildCommand
            environment("LAMPRAY_VERSION", context.version)
            environment("TARGET_ARCH", context.architecture)
            environment("TARGET_PLATFORM", context.platform)
        }

        logger.lifecycle("Successfully built container image: ${context.imageName}")
    }

    private fun prepareBuildContext() {
        val contextDir = buildContext.get().asFile
        val projectDir = project.rootDir

        // Copy Containerfile to build context
        val containerFile = projectDir.resolve("Containerfile")
        if (containerFile.exists()) {
            containerFile.copyTo(contextDir.resolve("Containerfile"), overwrite = true)
        } else {
            throw IllegalStateException("Containerfile not found in project root")
        }
    }
}
