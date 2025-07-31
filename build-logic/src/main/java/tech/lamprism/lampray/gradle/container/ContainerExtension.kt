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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring container build settings
 *
 * @author RollW
 */
abstract class ContainerExtension {

    /**
     * Project version for container image tagging
     */
    abstract val version: Property<String>

    /**
     * List of supported architectures for multi-arch builds
     */
    abstract val supportedArchitectures: ListProperty<String>

    /**
     * Base image name (without version and architecture suffix)
     */
    abstract val imageName: Property<String>

    /**
     * Path to the Containerfile (Dockerfile)
     */
    abstract val containerFile: RegularFileProperty

    /**
     * Custom container tools configuration
     */
    abstract val customTools: ListProperty<ContainerTool>

    /**
     * Preferred container tool (optional, will auto-detect if not specified)
     */
    abstract val preferredTool: Property<String>

    init {
        supportedArchitectures.convention(listOf("amd64", "arm64"))
        customTools.convention(emptyList())
    }
}
