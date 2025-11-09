/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray

/**
 * @author RollW
 */
object Version {
    const val VERSION = BuildConfig.VERSION
    const val GIT = "https://github.com/roll-w/lampray"
    const val BUILD_TIME = BuildConfig.BUILD_TIME
    const val GIT_COMMIT_TIME = BuildConfig.COMMIT_TIME
    const val GIT_COMMIT_ID_ABBREV = BuildConfig.COMMIT_ID_ABBREV
    const val BUILD_JAVA_VERSION = BuildConfig.JAVA_VERSION

    @JvmField
    val JAVA_VERSION: String? = System.getProperty("java.version")

    @JvmField
    val JAVA_RUNTIME_NAME: String? = System.getProperty("java.runtime.name")

    @JvmField
    val JAVA_RUNTIME_VERSION: String? = System.getProperty("java.runtime.version")

    @JvmField
    val JAVA_VM_NAME: String? = System.getProperty("java.vm.name")

    @JvmField
    val JAVA_VM_VERSION: String? = System.getProperty("java.vm.version")

    @JvmStatic
    fun formatVersion(): String = buildString {
        appendLine("Lampray version $VERSION")
        appendLine(GIT)
        appendLine("=======================")
        appendLine("Built at      : $BUILD_TIME with java version $BUILD_JAVA_VERSION")
        appendLine("From commit at: $GIT_COMMIT_TIME ($GIT_COMMIT_ID_ABBREV)")
        appendLine("Running on    : java version \"$JAVA_VERSION\"")
        appendLine("                $JAVA_RUNTIME_NAME (build $JAVA_RUNTIME_VERSION)")
        appendLine("                $JAVA_VM_NAME (build $JAVA_VM_VERSION)")
        appendLine("=======================")
        appendLine("Powered by Lampray")
    }
}