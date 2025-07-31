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

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Container build context for passing parameters to tools
 *
 * @author RollW
 */
data class ContainerBuildContext(
    val platform: String,
    val version: String,
    val architecture: String,
    val baseImageName: String = "lampray",
    val imageName: String = "${baseImageName}:${version}-${architecture}",
    val containerFile: String = "Containerfile",
    val buildContext: String = ".",
    val outputFile: String? = null,
    val manifestName: String? = null,
    val workingDirectory: File? = null,
    val environment: Map<String, String> = emptyMap()
)

/**
 * Interface for container tools that support OCI-compliant container operations
 */
interface ContainerTool {
    val executable: String
    val displayName: String
    val supportsBuild: Boolean
    val supportsSave: Boolean
    val supportsManifest: Boolean

    /**
     * Validate if the tool is available and working
     */
    fun validateTool(): Boolean

    /**
     * Get tool version information
     */
    fun getToolVersion(): String?

    /**
     * Execute build operation
     */
    fun executeBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean

    /**
     * Execute save operation
     */
    fun executeSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean

    /**
     * Execute manifest creation with images
     */
    fun executeManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean

    /**
     * Generate OCI manifest content by executing container tool commands
     */
    fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String

    companion object {
        fun getDefaultTools(): List<ContainerTool> = listOf(
            // DockerBuildxTool(),
            BuildahTool(), DockerTool(), PodmanTool(), NerdctlTool(), KanikoTool()
        )
    }
}

/**
 * Abstract base implementation for container tools
 */
abstract class BaseContainerTool : ContainerTool {

    override fun validateTool(): Boolean {
        return try {
            val process = ProcessBuilder()
                .command(getValidationCommand())
                .redirectErrorStream(true)
                .start()

            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    override fun getToolVersion(): String? {
        return try {
            val process = ProcessBuilder()
                .command(getVersionCommand())
                .redirectErrorStream(true)
                .start()

            if (process.waitFor() == 0) {
                process.inputStream.bufferedReader().readText().trim()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun executeBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean {
        if (!supportsBuild) {
            throw ContainerPluginException("Tool $displayName does not support build operations")
        }
        return doExecuteBuild(context, execOperations)
    }

    override fun executeSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean {
        if (!supportsSave) {
            throw ContainerPluginException("Tool $displayName does not support save operations")
        }

        if (context.outputFile == null) {
            throw ContainerPluginException("Output file must be specified for save operations")
        }

        return doExecuteSave(context, execOperations)
    }

    override fun executeManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean {
        if (!supportsManifest) {
            throw ContainerPluginException("Tool $displayName does not support manifest operations")
        }

        return doExecuteManifestCreation(context, imageNames, execOperations)
    }

    protected abstract fun getValidationCommand(): List<String>
    protected abstract fun getVersionCommand(): List<String>
    protected abstract fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean
    protected abstract fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean
    protected abstract fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean

    protected fun standardBuildArgs(context: ContainerBuildContext) = listOf(
        "--platform", context.platform,
        "--build-arg", "LAMPRAY_VERSION=${context.version}",
        "--build-arg", "CTX_PATH=./",
        "--tag", context.imageName,
        "--file", context.containerFile,
        context.buildContext
    )

    protected fun executeCommand(
        command: List<String>,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): Boolean = try {
        execOperations.exec {
            workingDir = context.workingDirectory ?: File(".")
            commandLine = command
            environment(context.environment)
        }
        true
    } catch (_: Exception) {
        false
    }

    protected fun executeCommandForOutput(
        command: List<String>,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String {
        val byteOutputStream = ByteArrayOutputStream()

        execOperations.exec {
            workingDir = context.workingDirectory ?: File(".")
            commandLine = command
            environment(context.environment)
            standardOutput = byteOutputStream
        }
        return byteOutputStream.toString("UTF-8").trim()
    }
}

/**
 * Docker tool implementation
 */
class DockerTool : BaseContainerTool() {
    override val executable = "docker"
    override val displayName = "Docker"
    override val supportsBuild = true
    override val supportsSave = true
    override val supportsManifest = true

    override fun getValidationCommand(): List<String> = listOf("docker", "info")
    override fun getVersionCommand(): List<String> = listOf("docker", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(listOf("docker", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(listOf("docker", "save", "-o", context.outputFile!!, context.imageName), context, execOperations)

    override fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean {
        val commands = mutableListOf<List<String>>()
        // Create manifest with all images in one command
        commands.add(listOf("docker", "manifest", "create", context.manifestName!!) + imageNames)
        return commands.all { executeCommand(it, context, execOperations) }
    }

    override fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String {
        val inspectCommand = listOf("docker", "manifest", "inspect", manifestName)
        return executeCommandForOutput(inspectCommand, context, execOperations)
    }
}

/**
 * Docker Buildx tool implementation with multi-platform support
 */
class DockerBuildxTool : BaseContainerTool() {
    override val executable = "docker"
    override val displayName = "Docker Buildx"
    override val supportsBuild = true
    override val supportsSave = true
    override val supportsManifest = true

    override fun getValidationCommand(): List<String> = listOf("docker", "buildx", "version")
    override fun getVersionCommand(): List<String> = listOf("docker", "buildx", "version")

    override fun validateTool(): Boolean {
        return try {
            val process = ProcessBuilder()
                .command(getValidationCommand())
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                ensureBuilder()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun ensureBuilder(): Boolean {
        return try {
            val checkProcess = ProcessBuilder()
                .command("docker", "buildx", "inspect", "multiarch-builder")
                .redirectErrorStream(true)
                .start()

            if (checkProcess.waitFor() != 0) {
                val createProcess = ProcessBuilder()
                    .command("docker", "buildx", "create", "--name", "multiarch-builder", "--use", "--bootstrap")
                    .redirectErrorStream(true)
                    .start()
                createProcess.waitFor() == 0
            } else {
                val useProcess = ProcessBuilder()
                    .command("docker", "buildx", "use", "multiarch-builder")
                    .redirectErrorStream(true)
                    .start()
                useProcess.waitFor() == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean {
        val buildxArgs = listOf(
            "docker", "buildx", "build",
            "--platform", context.platform,
            "--build-arg", "LAMPRAY_VERSION=${context.version}",
            "--build-arg", "CTX_PATH=./",
            "--tag", context.imageName,
            "--file", context.containerFile,
            "--load",
            context.buildContext
        )
        return executeCommand(buildxArgs, context, execOperations)
    }

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(listOf("docker", "save", "-o", context.outputFile!!, context.imageName), context, execOperations)

    override fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean {
        val platforms = imageNames.joinToString(",") { imageName ->
            val arch = imageName.substringAfterLast("-")
            when (arch) {
                "amd64" -> "linux/amd64"
                "arm64" -> "linux/arm64"
                else -> "linux/amd64"
            }
        }

        val buildxManifestArgs = listOf(
            "docker", "buildx", "build",
            "--platform", platforms,
            "--build-arg", "LAMPRAY_VERSION=${context.version}",
            "--build-arg", "CTX_PATH=./",
            "--tag", context.manifestName!!,
            "--file", context.containerFile,
            "--push",
            context.buildContext
        )
        return executeCommand(buildxManifestArgs, context, execOperations)
    }

    override fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String {
        val inspectCommand = listOf("docker", "buildx", "imagetools", "inspect", "--raw", manifestName)
        return executeCommandForOutput(inspectCommand, context, execOperations)
    }
}

/**
 * Podman tool implementation
 */
class PodmanTool : BaseContainerTool() {
    override val executable = "podman"
    override val displayName = "Podman"
    override val supportsBuild = true
    override val supportsSave = true
    override val supportsManifest = true

    override fun getValidationCommand(): List<String> = listOf("podman", "info")
    override fun getVersionCommand(): List<String> = listOf("podman", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(listOf("podman", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(
            listOf("podman", "save", "--format", "oci-archive", "-o", context.outputFile!!, context.imageName),
            context,
            execOperations
        )

    override fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean {
        val commands = mutableListOf<List<String>>()
        commands.add(listOf("podman", "manifest", "create", context.manifestName!!))
        imageNames.forEach { imageName ->
            commands.add(listOf("podman", "manifest", "add", context.manifestName, imageName))
        }
        return commands.all { executeCommand(it, context, execOperations) }
    }

    override fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String {
        val inspectCommand = listOf("podman", "manifest", "inspect", manifestName)
        return executeCommandForOutput(inspectCommand, context, execOperations)
    }
}

/**
 * Buildah tool implementation
 */
class BuildahTool : BaseContainerTool() {
    override val executable = "buildah"
    override val displayName = "Buildah"
    override val supportsBuild = true
    override val supportsSave = true
    override val supportsManifest = true

    override fun getValidationCommand(): List<String> = listOf("buildah", "info")
    override fun getVersionCommand(): List<String> = listOf("buildah", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(listOf("buildah", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(
            listOf("buildah", "push", context.imageName, "oci-archive:${context.outputFile}"),
            context,
            execOperations
        )

    override fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean {
        val commands = mutableListOf<List<String>>()
        commands.add(listOf("buildah", "manifest", "create", context.manifestName!!))
        imageNames.forEach { imageName ->
            commands.add(listOf("buildah", "manifest", "add", context.manifestName, imageName))
        }
        return commands.all { executeCommand(it, context, execOperations) }
    }

    override fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String {
        val inspectCommand = listOf("buildah", "manifest", "inspect", manifestName)
        return executeCommandForOutput(inspectCommand, context, execOperations)
    }
}

/**
 * nerdctl tool implementation
 */
class NerdctlTool : BaseContainerTool() {
    override val executable = "nerdctl"
    override val displayName = "nerdctl"
    override val supportsBuild = true
    override val supportsSave = true
    override val supportsManifest = true

    override fun getValidationCommand(): List<String> = listOf("nerdctl", "info")
    override fun getVersionCommand(): List<String> = listOf("nerdctl", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(listOf("nerdctl", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(
            listOf("nerdctl", "save", "-o", context.outputFile!!, context.imageName),
            context,
            execOperations
        )

    override fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean = executeCommand(
        listOf("nerdctl", "manifest", "create", context.manifestName!!) + imageNames,
        context,
        execOperations
    )

    override fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String {
        val inspectCommand = listOf("nerdctl", "manifest", "inspect", manifestName)
        return executeCommandForOutput(inspectCommand, context, execOperations)
    }
}

/**
 * Kaniko tool implementation (build-only tool)
 */
class KanikoTool : BaseContainerTool() {
    override val executable = "kaniko"
    override val displayName = "Kaniko"
    override val supportsBuild = true
    override val supportsSave = false
    override val supportsManifest = false

    override fun getValidationCommand(): List<String> = listOf("kaniko", "version")
    override fun getVersionCommand(): List<String> = listOf("kaniko", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        executeCommand(
            listOf(
                "kaniko",
                "--dockerfile", context.containerFile,
                "--context", context.buildContext,
                "--destination", context.imageName,
                "--build-arg", "LAMPRAY_VERSION=${context.version}",
                "--build-arg", "CTX_PATH=./"
            ),
            context,
            execOperations
        )

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean =
        throw UnsupportedOperationException("Kaniko does not support save operations")

    override fun doExecuteManifestCreation(
        context: ContainerBuildContext,
        imageNames: List<String>,
        execOperations: ExecOperations
    ): Boolean = throw UnsupportedOperationException("Kaniko does not support manifest operations")

    override fun executeGenerateOCIManifest(
        manifestName: String,
        context: ContainerBuildContext,
        execOperations: ExecOperations
    ): String = throw UnsupportedOperationException("Kaniko does not support manifest generation")
}
