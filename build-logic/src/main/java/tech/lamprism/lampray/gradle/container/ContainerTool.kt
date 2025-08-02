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

    companion object {
        fun getDefaultTools(): List<ContainerTool> = listOf(
            BuildahTool(), DockerTool(), DockerBuildxTool(), PodmanTool(), NerdctlTool()
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

    protected abstract fun getValidationCommand(): List<String>
    protected abstract fun getVersionCommand(): List<String>
    protected abstract fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean
    protected abstract fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations): Boolean

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
 * Docker Buildx tool implementation with multi-platform support
 *
 * Experimental feature: requires Docker Buildx to be installed and configured.
 */
class DockerBuildxTool : BaseContainerTool() {
    override val executable = "docker"
    override val displayName = "Docker Buildx"
    override val supportsBuild = true
    override val supportsSave = true

    override fun getValidationCommand() = listOf("docker", "buildx", "version")
    override fun getVersionCommand() = listOf("docker", "buildx", "version")

    override fun validateTool(): Boolean {
        return try {
            val process = ProcessBuilder().command(getValidationCommand()).redirectErrorStream(true).start()
            val exitCode = process.waitFor()
            if (exitCode == 0) ensureBuilder() else false
        } catch (_: Exception) {
            false
        }
    }

    private fun ensureBuilder(): Boolean {
        return try {
            val checkProcess =
                ProcessBuilder().command("docker", "buildx", "inspect", "multiarch-builder").redirectErrorStream(true)
                    .start()
            if (checkProcess.waitFor() != 0) {
                val createProcess = ProcessBuilder().command(
                    "docker",
                    "buildx",
                    "create",
                    "--name",
                    "multiarch-builder",
                    "--use",
                    "--bootstrap"
                ).redirectErrorStream(true).start()
                createProcess.waitFor() == 0
            } else {
                val useProcess =
                    ProcessBuilder().command("docker", "buildx", "use", "multiarch-builder").redirectErrorStream(true)
                        .start()
                useProcess.waitFor() == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations): Boolean {
        val buildxArgs = listOf(
            "docker", "buildx", "build", "--provenance", "false", "--platform", context.platform,
            "--build-arg", "LAMPRAY_VERSION=${context.version}", "--build-arg", "CTX_PATH=./",
            "--tag", context.imageName, "--file", context.containerFile, "--load", context.buildContext
        )
        return executeCommand(buildxArgs, context, execOperations)
    }

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(listOf("docker", "save", "-o", context.outputFile!!, context.imageName), context, execOperations)
}

/**
 * Docker tool implementation
 */
class DockerTool : BaseContainerTool() {
    override val executable = "docker"
    override val displayName = "Docker"
    override val supportsBuild = true
    override val supportsSave = true

    override fun getValidationCommand() = listOf("docker", "info")
    override fun getVersionCommand() = listOf("docker", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(
            listOf("docker", "build", "--provenance", "false") + standardBuildArgs(context),
            context,
            execOperations
        )

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(listOf("docker", "save", "-o", context.outputFile!!, context.imageName), context, execOperations)
}

/**
 * Podman tool implementation
 */
class PodmanTool : BaseContainerTool() {
    override val executable = "podman"
    override val displayName = "Podman"
    override val supportsBuild = true
    override val supportsSave = true

    override fun getValidationCommand() = listOf("podman", "info")
    override fun getVersionCommand() = listOf("podman", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(listOf("podman", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(
            listOf(
                "podman",
                "save",
                "--format",
                "oci-archive",
                "-o",
                context.outputFile!!,
                context.imageName
            ), context, execOperations
        )
}

/**
 * Buildah tool implementation
 */
class BuildahTool : BaseContainerTool() {
    override val executable = "buildah"
    override val displayName = "Buildah"
    override val supportsBuild = true
    override val supportsSave = true

    override fun getValidationCommand() = listOf("buildah", "info")
    override fun getVersionCommand() = listOf("buildah", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(listOf("buildah", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(
            listOf("buildah", "push", context.imageName, "oci-archive:${context.outputFile}"),
            context,
            execOperations
        )
}

/**
 * nerdctl tool implementation
 */
class NerdctlTool : BaseContainerTool() {
    override val executable = "nerdctl"
    override val displayName = "nerdctl"
    override val supportsBuild = true
    override val supportsSave = true

    override fun getValidationCommand() = listOf("nerdctl", "info")
    override fun getVersionCommand() = listOf("nerdctl", "--version")

    override fun doExecuteBuild(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(listOf("nerdctl", "build") + standardBuildArgs(context), context, execOperations)

    override fun doExecuteSave(context: ContainerBuildContext, execOperations: ExecOperations) =
        executeCommand(
            listOf("nerdctl", "save", "-o", context.outputFile!!, context.imageName),
            context,
            execOperations
        )
}
