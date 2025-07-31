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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * Plugin for OCI-compliant container image building and packaging
 *
 * @author RollW
 */
class ContainerPlugin : Plugin<Project> {

    companion object {
        const val CONTAINER_GROUP = "container"

        val SUPPORTED_PLATFORMS = mapOf(
            "amd64" to "linux/amd64",
            "arm64" to "linux/arm64"
        )
    }

    override fun apply(project: Project) {
        // Create extension for configuration
        val extension = project.extensions.create<ContainerExtension>("containerImage")

        // Configure default values
        extension.version.convention(project.version.toString())
        extension.supportedArchitectures.convention(SUPPORTED_PLATFORMS.keys.toList())

        // Detect current architecture
        val currentArch = detectCurrentArchitecture()

        // Register build tasks for each architecture
        SUPPORTED_PLATFORMS.forEach { (arch, platform) ->
            val taskSuffix = arch.replaceFirstChar { it.uppercase() }

            project.tasks.register<BuildContainerImageTask>("buildImage$taskSuffix") {
                group = CONTAINER_GROUP
                description = "Build container image for $platform architecture"

                this.version.set(extension.version)
                this.architecture.set(arch)
                this.platform.set(platform)
                this.buildContext.set(project.layout.buildDirectory.dir("dist"))

                dependsOn("package")
            }

            project.tasks.register<PackageContainerImageTask>("packageImage$taskSuffix") {
                group = CONTAINER_GROUP
                description = "Package container image for $platform architecture"

                this.version.set(extension.version)
                this.architecture.set(arch)
                this.platform.set(platform)
                this.buildContext.set(project.layout.buildDirectory.dir("dist"))
                this.outputFile.set(
                    project.layout.buildDirectory.file("dist/lampray-${extension.version.get()}-${arch}-image.tar")
                )

                dependsOn("buildImage$taskSuffix")
            }
        }

        // Register convenience tasks
        project.tasks.register("buildImage") {
            group = CONTAINER_GROUP
            description = "Build container image for current architecture"
            dependsOn("buildImage${currentArch.replaceFirstChar { it.uppercase() }}")
        }

        project.tasks.register("buildImageMultiArch") {
            group = CONTAINER_GROUP
            description = "Build container images for all supported architectures"
            dependsOn(SUPPORTED_PLATFORMS.keys.map { "buildImage${it.replaceFirstChar { c -> c.uppercase() }}" })
        }

        project.tasks.register("packageImage") {
            group = CONTAINER_GROUP
            description = "Package container image for current architecture"
            dependsOn("packageImage${currentArch.replaceFirstChar { it.uppercase() }}")
        }

        project.tasks.register("packageImagesMultiArch") {
            group = CONTAINER_GROUP
            description = "Package container images for all supported architectures"
            dependsOn(SUPPORTED_PLATFORMS.keys.map { "packageImage${it.replaceFirstChar { c -> c.uppercase() }}" })
        }

        project.tasks.register<CreateContainerManifestTask>("createMultiArchManifest") {
            group = CONTAINER_GROUP
            description = "Create multi-architecture container manifest"

            version.set(extension.version)
            architecture.set(currentArch)
            platform.set(SUPPORTED_PLATFORMS[currentArch]!!)
            buildContext.set(project.layout.buildDirectory.dir("dist"))
            manifestName.set("lampray:${extension.version.get()}")
            supportedArchitectures.set(extension.supportedArchitectures)

            dependsOn("buildImageMultiArch")
        }
    }

    private fun detectCurrentArchitecture(): String {
        val osArch = System.getProperty("os.arch").lowercase()
        return when {
            osArch.contains("amd64") || osArch.contains("x86_64") -> "amd64"
            osArch.contains("aarch64") || osArch.contains("arm64") -> "arm64"
            else -> "amd64"
        }
    }
}
