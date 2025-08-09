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

package tech.lamprism.lampray.system.console.cli.runner

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import tech.lamprism.lampray.system.console.CommandSpecification
import tech.lamprism.lampray.system.console.SimpleCommandOption
import tech.lamprism.lampray.system.console.SimpleCommandSpecification
import tech.lamprism.lampray.system.console.cli.CommandRunContext
import tech.lamprism.lampray.system.console.cli.CommandRunner
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Command runner for exporting embedded resources from the classpath.
 *
 * Supports multiple export formats:
 * - folder: Extract to directory structure (default)
 * - zip: Create a ZIP archive
 * - tar: Create a TAR archive
 * - tar.gz: Create a compressed TAR.GZ archive
 *
 * @author RollW
 */
class ResourcesExportCommandRunner : CommandRunner {
    companion object {
        private const val DEFAULT_PATH = "resources"
        private const val ASSET_PATH = "/assets"

        // Supported export formats
        private const val FORMAT_FOLDER = "folder"
        private const val FORMAT_ZIP = "zip"
        private const val FORMAT_TAR = "tar"
        private const val FORMAT_TAR_GZ = "tar.gz"

        private val SUPPORTED_FORMATS = setOf(FORMAT_FOLDER, FORMAT_ZIP, FORMAT_TAR, FORMAT_TAR_GZ)
    }

    override fun runCommand(context: CommandRunContext): Int {
        val args = context.arguments
        val path = args["--output"]?.toString()
        if (path.isNullOrBlank()) {
            context.printStream.println("Error: No output path specified. Use --output to specify the output path.")
            return 1
        }

        val format = args["--format"]?.toString()?.lowercase() ?: FORMAT_FOLDER
        if (format !in SUPPORTED_FORMATS) {
            context.printStream.println(
                "Error: Unsupported format '$format'. Supported formats: ${
                    SUPPORTED_FORMATS.joinToString(
                        ", "
                    )
                }"
            )
            return 1
        }

        val outputPath = File(path)
        context.printStream.println("Extracting resources to $path using format: $format")

        val resource = ClassPathResource(ASSET_PATH)
        if (!resource.exists()) {
            context.printStream.println("Error: Frontend resources not found in classpath. Ensure the application is built with frontend assets.")
            return 1
        }

        val resolver = PathMatchingResourcePatternResolver()
        val resources = try {
            resolver.getResources("classpath:$ASSET_PATH/**")
        } catch (e: Exception) {
            context.printStream.println("Error: Failed to load resources: ${e.message}")
            return 1
        }

        return try {
            when (format) {
                FORMAT_FOLDER -> exportToFolder(context, resource, resources, outputPath)
                FORMAT_ZIP -> exportToArchive(context, resource, resources, outputPath, ::createZipExporter)
                FORMAT_TAR -> exportToArchive(context, resource, resources, outputPath, ::createTarExporter)
                FORMAT_TAR_GZ -> exportToArchive(context, resource, resources, outputPath, ::createTarGzExporter)
                else -> {
                    context.printStream.println("Error: Unsupported format: $format")
                    1
                }
            }
        } catch (e: Exception) {
            context.printStream.println("Error during export: ${e.message}")
            1
        }
    }

    /**
     * Archive exporter interface for different archive formats
     */
    private fun interface ArchiveExporter {
        fun addEntry(path: String, resource: Resource)
    }

    /**
     * Export resources to a directory structure
     */
    private fun exportToFolder(
        context: CommandRunContext,
        assetResource: Resource,
        resources: Array<Resource>,
        outputPath: File
    ): Int {
        if (!outputPath.exists()) {
            outputPath.mkdirs()
        }

        resources.forEach { resource ->
            val relativePath = getRelativePath(assetResource, resource) ?: return@forEach
            val outputFile = File(outputPath, relativePath)

            if (!resource.isReadable) {
                outputFile.mkdirs()
                return@forEach
            }

            resource.copyTo(outputFile)
            context.printStream.println("Extracted: $relativePath")
        }

        context.printStream.println("Resources successfully exported to directory: ${outputPath.absolutePath}")
        return 0
    }

    /**
     * Export resources to archive using provided exporter factory
     */
    private fun exportToArchive(
        context: CommandRunContext,
        assetResource: Resource,
        resources: Array<Resource>,
        outputPath: File,
        exporterFactory: (File, CommandRunContext) -> Pair<OutputStream, ArchiveExporter>
    ): Int {
        val (outputStream, exporter) = exporterFactory(outputPath, context)

        outputStream.use {
            resources.forEach { resource ->
                val relativePath = getRelativePath(assetResource, resource) ?: return@forEach
                if (!resource.isReadable) return@forEach

                exporter.addEntry(relativePath, resource)
                context.printStream.println("Added to archive: $relativePath")
            }
        }
        context.printStream.println("Resources successfully exported to archive: ${outputPath.absolutePath}")
        return 0
    }

    /**
     * Create ZIP exporter
     */
    private fun createZipExporter(
        outputPath: File,
        @Suppress("UNUSED_PARAMETER") context: CommandRunContext
    ): Pair<OutputStream, ArchiveExporter> {
        outputPath.parentFile?.mkdirs()

        val zipOutput = ZipOutputStream(FileOutputStream(outputPath))
        val exporter = ArchiveExporter { path, resource ->
            val zipEntry = ZipEntry(path)
            zipOutput.putNextEntry(zipEntry)
            resource.inputStream.use { input ->
                input.copyTo(zipOutput)
            }
            zipOutput.closeEntry()
        }

        return zipOutput to exporter
    }

    /**
     * Create TAR exporter
     */
    private fun createTarExporter(
        outputPath: File,
        @Suppress("UNUSED_PARAMETER") context: CommandRunContext
    ): Pair<OutputStream, ArchiveExporter> {
        outputPath.parentFile?.mkdirs()

        val tarOutput = TarArchiveOutputStream(FileOutputStream(outputPath))
        val exporter = ArchiveExporter { path, resource ->
            val tarEntry = TarArchiveEntry(path)
            tarEntry.size = resource.contentLength()
            tarOutput.putArchiveEntry(tarEntry)
            resource.inputStream.use { input ->
                input.copyTo(tarOutput)
            }
            tarOutput.closeArchiveEntry()
        }

        return tarOutput to exporter
    }

    /**
     * Create TAR.GZ exporter
     */
    private fun createTarGzExporter(
        outputPath: File,
        @Suppress("UNUSED_PARAMETER") context: CommandRunContext
    ): Pair<OutputStream, ArchiveExporter> {
        outputPath.parentFile?.mkdirs()

        val tarGzOutput = TarArchiveOutputStream(GZIPOutputStream(FileOutputStream(outputPath)))
        val exporter = ArchiveExporter { path, resource ->
            val tarEntry = TarArchiveEntry(path)
            tarEntry.size = resource.contentLength()
            tarGzOutput.putArchiveEntry(tarEntry)
            resource.inputStream.use { input ->
                input.copyTo(tarGzOutput)
            }
            tarGzOutput.closeArchiveEntry()
        }

        return tarGzOutput to exporter
    }

    /**
     * Extract relative path from parent resource to child resource
     */
    private fun getRelativePath(parent: Resource, resource: Resource): String? {
        val parentPath = parent.uri.toString()
        val resourcePath = resource.uri.toString()

        if (parentPath == resourcePath || !resourcePath.startsWith(parentPath)) {
            return null
        }

        val relativePath = resourcePath.substring(parentPath.length)
        if (relativePath.isEmpty() || relativePath == "/") {
            return null
        }

        return URLDecoder.decode(relativePath.removePrefix("/"), StandardCharsets.UTF_8)
    }

    /**
     * Copy resource content to output file
     */
    private fun Resource.copyTo(outputPath: File) {
        outputPath.parentFile?.mkdirs()
        if (!outputPath.exists()) {
            outputPath.createNewFile()
        }
        this.inputStream.use { input ->
            outputPath.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    override fun getCommandSpecification(): CommandSpecification {
        return SimpleCommandSpecification.builder()
            .setNames("resources", "export")
            .setDescription("Export embedded frontend resources (assets) from the application classpath to local storage.")
            .setHeader("Export embedded frontend resources")
            .addOption(
                SimpleCommandOption.builder()
                    .setNames("--output", "-o")
                    .setDescription("Output path for exported resources. For archives, this will be the archive filename (extension added automatically if not provided)")
                    .setType(String::class.java)
                    .setRequired(true)
                    .setDefaultValue(DEFAULT_PATH)
                    .build()
            )
            .addOption(
                SimpleCommandOption.builder()
                    .setNames("--format", "-f")
                    .setDescription("Export format: 'folder' (directory structure), 'zip' (ZIP archive), 'tar' (TAR archive), or 'tar.gz' (compressed TAR archive). Default: folder")
                    .setType(String::class.java)
                    .setRequired(false)
                    .setDefaultValue(FORMAT_FOLDER)
                    .build()
            )
            .build()
    }
}