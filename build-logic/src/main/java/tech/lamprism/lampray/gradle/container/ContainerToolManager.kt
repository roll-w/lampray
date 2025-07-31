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
 * Container tool detector for OCI-compliant container operations
 *
 * @author RollW
 */
class ContainerToolManager(private val customTools: List<ContainerTool> = emptyList()) {

    private var detectedTool: ContainerTool? = null

    fun detectAvailableTool(preferredTool: String? = null): ContainerTool {
        if (detectedTool != null) {
            return detectedTool!!
        }

        val allTools = customTools + ContainerTool.Companion.getDefaultTools()

        // Try preferred tool first if specified
        if (preferredTool != null) {
            val preferred = allTools.find { it.executable == preferredTool || it.displayName == preferredTool }
            if (preferred != null && isToolAvailable(preferred)) {
                detectedTool = preferred
                return preferred
            }
        }

        // Try all tools in order
        for (tool in allTools) {
            if (isToolAvailable(tool)) {
                detectedTool = tool
                return tool
            }
        }

        throw GradleException(
            "No container build tool found. Please install one of: " +
                    allTools.joinToString(", ") { it.displayName }
        )
    }

    private fun isToolAvailable(tool: ContainerTool): Boolean {
        return try {
            val process = ProcessBuilder()
                .command(tool.executable, "--version")
                .redirectErrorStream(true)
                .start()

            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}
