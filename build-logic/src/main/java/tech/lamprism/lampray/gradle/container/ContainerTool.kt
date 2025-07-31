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

import org.gradle.api.GradleException

/**
 * Container build context for passing parameters to tools
 *
 * @author RollW
 */
data class ContainerBuildContext(
    val platform: String,
    val version: String,
    val architecture: String,
    val imageName: String = "lampray:${version}-${architecture}",
    val containerFile: String = "Containerfile",
    val buildContext: String = ".",
    val outputFile: String? = null,
    val manifestName: String? = null
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

    fun buildCommand(context: ContainerBuildContext): List<String>
    fun saveCommand(context: ContainerBuildContext): List<String>
    fun manifestCreateCommand(context: ContainerBuildContext): List<String>
    fun manifestAddCommand(context: ContainerBuildContext, imageName: String): List<String>

    companion object {
        fun getDefaultTools(): List<ContainerTool> = listOf(
            BuildahTool(), DockerTool(), PodmanTool(), NerdctlTool(), KanikoTool()
        )
    }
}

/**
 * Abstract base implementation for container tools
 */
abstract class BaseContainerTool : ContainerTool {

    override fun buildCommand(context: ContainerBuildContext): List<String> {
        if (!supportsBuild) {
            throw GradleException("Tool $displayName does not support build operations")
        }
        return doBuildCommand(context)
    }

    override fun saveCommand(context: ContainerBuildContext): List<String> {
        if (!supportsSave) {
            throw GradleException("Tool $displayName does not support save operations")
        }

        val outputFile = context.outputFile
            ?: throw IllegalArgumentException("Output file must be specified for save operations")

        return doSaveCommand(context, outputFile)
    }

    override fun manifestCreateCommand(context: ContainerBuildContext): List<String> {
        if (!supportsManifest) {
            throw GradleException("Tool $displayName does not support manifest operations")
        }

        val manifestName = context.manifestName
            ?: throw IllegalArgumentException("Manifest name must be specified for manifest operations")

        return doManifestCreateCommand(context, manifestName)
    }

    override fun manifestAddCommand(context: ContainerBuildContext, imageName: String): List<String> {
        if (!supportsManifest) {
            throw GradleException("Tool $displayName does not support manifest operations")
        }

        val manifestName = context.manifestName
            ?: throw IllegalArgumentException("Manifest name must be specified for manifest operations")

        return doManifestAddCommand(context, manifestName, imageName)
    }

    protected abstract fun doBuildCommand(context: ContainerBuildContext): List<String>
    protected abstract fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String>
    protected abstract fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String>
    protected abstract fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String>

    protected fun standardBuildArgs(context: ContainerBuildContext) = listOf(
        "--platform", context.platform,
        "--build-arg", "LAMPRAY_VERSION=${context.version}",
        "--build-arg", "CTX_PATH=./",
        "--tag", context.imageName,
        "--file", context.containerFile,
        context.buildContext
    )
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

    override fun doBuildCommand(context: ContainerBuildContext): List<String> =
        listOf("docker", "build") + standardBuildArgs(context)

    override fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String> =
        listOf("docker", "save", "-o", outputFile, context.imageName)

    override fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String> =
        listOf("docker", "manifest", "create", manifestName)

    override fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String> =
        listOf("docker", "manifest", "annotate", manifestName, imageName)
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

    override fun doBuildCommand(context: ContainerBuildContext): List<String> =
        listOf("podman", "build") + standardBuildArgs(context)

    override fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String> =
        listOf("podman", "save", "--format", "oci-archive", "-o", outputFile, context.imageName)

    override fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String> =
        listOf("podman", "manifest", "create", manifestName)

    override fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String> =
        listOf("podman", "manifest", "add", manifestName, imageName)
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

    override fun doBuildCommand(context: ContainerBuildContext): List<String> =
        listOf("buildah", "build") + standardBuildArgs(context)

    override fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String> =
        listOf("buildah", "push", context.imageName, "oci-archive:$outputFile")

    override fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String> =
        listOf("buildah", "manifest", "create", manifestName)

    override fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String> =
        listOf("buildah", "manifest", "add", manifestName, imageName)
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

    override fun doBuildCommand(context: ContainerBuildContext): List<String> =
        listOf("nerdctl", "build") + standardBuildArgs(context)

    override fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String> =
        listOf("nerdctl", "save", "-o", outputFile, context.imageName)

    override fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String> =
        listOf("nerdctl", "manifest", "create", manifestName)

    override fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String> =
        listOf("nerdctl", "manifest", "annotate", manifestName, imageName)
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

    override fun doBuildCommand(context: ContainerBuildContext): List<String> = listOf(
        "kaniko",
        "--dockerfile", context.containerFile,
        "--context", context.buildContext,
        "--destination", context.imageName,
        "--build-arg", "LAMPRAY_VERSION=${context.version}",
        "--build-arg", "CTX_PATH=./"
    )

    override fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String> =
        throw UnsupportedOperationException("Kaniko does not support save operations")

    override fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String> =
        throw UnsupportedOperationException("Kaniko does not support manifest operations")

    override fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String> = throw UnsupportedOperationException("Kaniko does not support manifest operations")
}

/**
 * Custom container tool implementation for user-defined tools
 */
class CustomContainerTool(
    override val executable: String,
    override val displayName: String,
    override val supportsBuild: Boolean = true,
    override val supportsSave: Boolean = true,
    override val supportsManifest: Boolean = true,
    private val buildCommandBuilder: ((ContainerBuildContext) -> List<String>)? = null,
    private val saveCommandBuilder: ((ContainerBuildContext, String) -> List<String>)? = null,
    private val manifestCreateCommandBuilder: ((ContainerBuildContext, String) -> List<String>)? = null,
    private val manifestAddCommandBuilder: ((ContainerBuildContext, String, String) -> List<String>)? = null
) : BaseContainerTool() {

    override fun doBuildCommand(context: ContainerBuildContext): List<String> =
        buildCommandBuilder?.invoke(context) ?: (listOf(executable, "build") + standardBuildArgs(context))

    override fun doSaveCommand(context: ContainerBuildContext, outputFile: String): List<String> =
        saveCommandBuilder?.invoke(context, outputFile) ?: listOf(
            executable,
            "save",
            "-o",
            outputFile,
            context.imageName
        )

    override fun doManifestCreateCommand(context: ContainerBuildContext, manifestName: String): List<String> =
        manifestCreateCommandBuilder?.invoke(context, manifestName) ?: listOf(
            executable,
            "manifest",
            "create",
            manifestName
        )

    override fun doManifestAddCommand(
        context: ContainerBuildContext,
        manifestName: String,
        imageName: String
    ): List<String> =
        manifestAddCommandBuilder?.invoke(context, manifestName, imageName) ?: listOf(
            executable,
            "manifest",
            "add",
            manifestName,
            imageName
        )
}