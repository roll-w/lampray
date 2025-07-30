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

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerSaveImage
import org.apache.tools.ant.taskdefs.condition.Os
import java.util.Locale

plugins {
    id("lampray-project")
    id("com.bmuschko.docker-remote-api") version "9.4.0"
}

val supportedPlatforms = mapOf(
    "amd64" to "linux/amd64",
    "arm64" to "linux/arm64"
)

fun detectCurrentArchitecture(): String {
    val osArch = System.getProperty("os.arch").lowercase(Locale.ROOT)
    return when {
        osArch.contains("amd64") || osArch.contains("x86_64") -> "amd64"
        osArch.contains("aarch64") || osArch.contains("arm64") -> "arm64"
        else -> "amd64"
    }
}

val currentArch = detectCurrentArchitecture()

tasks.register<Tar>("package") {
    dependsOn(":lampray-web:assemble")

    group = "distribution"
    description = "Creates a distribution package for the project"

    val baseDir = "/lampray-${version}"
    into(baseDir) {
        from("${project.rootDir}/distribution/README")
        from("${project.rootDir}/distribution/NOTICE")
        from("${project.rootDir}/LICENSE")
    }
    into("${baseDir}/lib") {
        from("${project(":lampray-web").projectDir}/build/libs/lampray-web-${version}.jar") {
            rename("lampray-web-${version}.jar", "lampray.jar")
        }
    }
    into("${baseDir}/bin") {
        from("${project.projectDir}/scripts/lampray.sh") {
            rename("lampray.sh", "lampray")
            filePermissions {
                unix("555")
            }
        }
    }

    into("${baseDir}/conf") {
        from("${project(":lampray-web").projectDir}/src/main/resources/lampray.conf")
    }

    archiveFileName = "lampray-${version}-dist.tar.gz"
    destinationDirectory = layout.buildDirectory.dir("dist")
    compression = Compression.GZIP

    outputs.upToDateWhen { false }
}

tasks.register("buildImage") {
    group = "build"
    description = "Build Docker image for lampray (auto-detect current architecture)."
    dependsOn("buildImage${currentArch.replaceFirstChar { it.uppercase() }}")

    outputs.upToDateWhen { false }
}

supportedPlatforms.forEach { (arch, platform) ->
    tasks.register<DockerBuildImage>("buildImage${arch.replaceFirstChar { it.uppercase() }}") {
        group = "build"
        description = "Build Docker image for lampray on $platform architecture."
        dependsOn(":package")

        doFirst {
            copy {
                from("Dockerfile")
                into(layout.buildDirectory.dir("dist"))
            }
        }

        inputDir = layout.buildDirectory.dir("dist")
        images = setOf("lampray:${version}-${arch}")
        buildArgs = mapOf(
            "LAMPRAY_VERSION" to version.toString(),
            "CTX_PATH" to "./"
        )
        this.platform = platform
        outputs.upToDateWhen { false }
    }
}

tasks.register("buildImageMultiArch") {
    group = "build"
    description = "Build Docker images for all supported architectures."

    supportedPlatforms.keys.forEach { arch ->
        dependsOn("buildImage${arch.replaceFirstChar { it.uppercase() }}")
    }
    outputs.upToDateWhen { false }
}

tasks.register("packageImage") {
    group = "distribution"
    description = "Package Docker image for lampray (auto-detect current architecture)."
    dependsOn("packageImage${currentArch.replaceFirstChar { it.uppercase() }}")

    outputs.upToDateWhen { false }
}

supportedPlatforms.forEach { (arch, platform) ->
    tasks.register<DockerSaveImage>("packageImage${arch.replaceFirstChar { it.uppercase() }}") {
        group = "distribution"
        description = "Package Docker image for lampray on $platform architecture."
        dependsOn("buildImage${arch.replaceFirstChar { it.uppercase() }}")

        images = setOf("lampray:${version}-${arch}")
        destFile = layout.buildDirectory.file("dist/lampray-${version}-${arch}-image.tar.gz")
        useCompression = true

        outputs.upToDateWhen { false }
    }
}

tasks.register("packageImagesMultiArch") {
    group = "distribution"
    description = "Package Docker images for all supported architectures."

    supportedPlatforms.keys.forEach { arch ->
        dependsOn("packageImage${arch.replaceFirstChar { it.uppercase() }}")
    }

    outputs.upToDateWhen { false }
}

tasks.register<Exec>("buildFrontend") {
    group = "build"
    description = "Build frontend of this project."

    workingDir = file("${project.projectDir}/lampray-frontend")

    val npm = when {
        Os.isFamily(Os.FAMILY_WINDOWS) -> "npm.cmd"
        else -> "npm"
    }
    commandLine = listOf(npm, "run", "build")
    standardOutput = System.out
    outputs.upToDateWhen { false }
}

tasks.register<Tar>("packageFrontend") {
    group = "distribution"
    description = "Creates distribution pack for the project frontend."

    dependsOn("buildFrontend")

    into("lampray-frontend") {
        from("${project.projectDir}/lampray-frontend/dist")
    }
    // TODO: may move to package task

    archiveFileName.set("lampray-frontend-${version}.tar.gz")
    destinationDirectory.set(file("${project.projectDir}/build/dist"))
    compression = Compression.GZIP
}

tasks.register("version") {
    group = "tool"
    description = "Displays the version of this project."
    println(version)

    outputs.upToDateWhen { false }
}
