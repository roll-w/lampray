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

package tech.lamprism.lampray.cli.runner

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import tech.lamprism.lampray.cli.CommandRunContext
import tech.lamprism.lampray.cli.CommandRunner
import tech.lamprism.lampray.shell.CommandSpecification
import tech.lamprism.lampray.shell.SimpleCommandSpecification
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * @author RollW
 */
class ResourceExtractCommandRunner : CommandRunner {
    companion object {
        private const val DEFAULT_PATH = "resources"

        private const val ASSET_PATH = "/assets"
    }

    override fun runCommand(context: CommandRunContext): Int {
        val args = context.arguments
        val path = args["path"]?.toString() ?: return 1
        val outputPath = File(path)
        println("Extracting resources to $path")
        val resource = ClassPathResource(ASSET_PATH)
        if (!resource.exists()) {
            println("Error: Not compiled with frontend resources, skipping resource extraction")
            return 1
        }
        val resolver = PathMatchingResourcePatternResolver()
        if (!outputPath.exists()) {
            outputPath.mkdirs()
        }
        copyResources(resolver, resource, outputPath)
        return 0
    }

    override fun getCommandSpecification(): CommandSpecification {
        return SimpleCommandSpecification.builder()
            .setNames("resources", "extract")
            .build()
    }

    private fun copyResources(
        resourceResolver: ResourcePatternResolver,
        assetResource: Resource,
        outputPath: File
    ) {
        val resources = resourceResolver.getResources("classpath:$ASSET_PATH/**")
        resources.forEach { resource ->
            val path = getRelativePath(assetResource, resource) ?: return@forEach
            val outputFile = File(outputPath, path)
            if (!resource.isReadable) {
                outputFile.mkdirs()
                return@forEach
            }
            resource.copyTo(outputFile)
            println("Extracted $path")
        }
    }

    private fun getRelativePath(
        parent: Resource,
        resource: Resource
    ): String? {
        val parentPath = parent.uri.toString()
        val resourcePath = resource.uri.toString()
        if (parentPath == resourcePath) {
            return null
        }

        // Remove common part of the path
        if (!resourcePath.startsWith(parentPath)) {
            return null
        }
        val relativePath = resourcePath.substring(parentPath.length)
        if (relativePath.isEmpty() || relativePath == "/") {
            return null
        }
        return URLDecoder.decode(relativePath.replaceFirst("/", ""), StandardCharsets.UTF_8)
    }

    private fun Resource.copyTo(outputPath: File) {
        if (!outputPath.exists()) {
            outputPath.createNewFile()
        }
        this.inputStream.use { input ->
            outputPath.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}