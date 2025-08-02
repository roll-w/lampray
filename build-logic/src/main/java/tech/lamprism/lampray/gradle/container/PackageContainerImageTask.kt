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

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Task for packaging container images as OCI archives
 *
 * @author RollW
 */
abstract class PackageContainerImageTask : BaseContainerTask() {

    @get:Input
    abstract val outputFile: Property<String>

    init {
        description = "Package container image as OCI archive"
    }

    @TaskAction
    fun packageImage() {
        val toolManager = getContainerToolManager()
        val preferredTool = getPreferredTool()
        val tool = toolManager.detectAvailableTool(preferredTool)
        val extension = containerExtension.get()

        val outputPath = outputFile.get()
        val isGzipOutput = outputPath.endsWith(".gz")

        // Use a temporary file for uncompressed output if final output should be gzipped
        val actualOutputPath = if (isGzipOutput) {
            outputPath.removeSuffix(".gz")
        } else {
            outputPath
        }

        val context = ContainerBuildContext(
            platform = platform.get(),
            version = version.get(),
            architecture = architecture.get(),
            baseImageName = extension.imageName.get(),
            outputFile = actualOutputPath,
            workingDirectory = buildContext.get().asFile
        )

        logger.lifecycle("Packaging container image with tool: ${toolManager.getToolInfo(tool)}")
        logger.lifecycle("Image: ${context.imageName}")
        logger.lifecycle("Output: $outputPath")

        // Execute the container tool save operation
        if (!tool.executeSave(context, execOperations)) {
            throw ContainerPluginException("Failed to package container image to: $actualOutputPath")
        }

        val tempFile = File(actualOutputPath)
        if (!tempFile.exists()) {
            throw ContainerPluginException("Container tool did not create expected output file: $actualOutputPath")
        }

        // Compress with gzip if needed
        if (isGzipOutput) {
            compressWithGzip(tempFile, File(outputPath))

            // Clean up temporary uncompressed file
            if (tempFile.delete()) {
                logger.debug("Cleaned up temporary file: $actualOutputPath")
            } else {
                logger.warn("Failed to clean up temporary file: $actualOutputPath")
            }
        }

        // Show final file size
        val finalFile = File(outputPath)
        if (finalFile.exists()) {
            val sizeInMB = finalFile.length() / (1024 * 1024)
            logger.lifecycle("Successfully packaged container image to: $outputPath")
            logger.lifecycle("Package size: $sizeInMB MB")
        }
    }

    private fun compressWithGzip(inputFile: File, outputFile: File) {
        try {
            FileInputStream(inputFile).use { fis ->
                FileOutputStream(outputFile).use { fos ->
                    GZIPOutputStream(fos).use { gzos ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            gzos.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw ContainerPluginException("Failed to compress image archive: ${e.message}", e)
        }
    }
}
