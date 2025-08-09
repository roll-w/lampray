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

package tech.lamprism.lampray.system.console.shell.configuration

import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.shell.command.CommandExceptionResolver
import org.springframework.shell.command.CommandHandlingResult
import org.springframework.shell.command.CommandRegistration
import org.springframework.shell.result.CommandNotFoundMessageProvider
import org.springframework.stereotype.Component

/**
 * @author RollW
 */
@Component
class LamprayCommandExceptionResolver : CommandExceptionResolver, CommandNotFoundMessageProvider {
    override fun resolve(ex: Exception): CommandHandlingResult {
        return CommandHandlingResult.of("Error: ${ex.message}\n")
    }

    override fun apply(context: CommandNotFoundMessageProvider.ProviderContext): String {
        val command = context.commands().joinToString(" ")
        val closestMatch = findClosestMatch(command, context.registrations())
        return "Command '$command' not found." + if (closestMatch.isNotEmpty()) {
            " Did you mean '$closestMatch'?"
        } else {
            ""
        }
    }

    private fun findClosestMatch(
        command: String,
        registrations: Map<String, CommandRegistration>
    ): String {
        if (command.isEmpty() || command.length > 30) {
            return ""
        }
        return registrations.keys.minByOrNull {
            levenshteinDistance.apply(command, it).apply {
                if (this < 0) {
                    return@minByOrNull Int.MAX_VALUE
                }
            }
        } ?: ""
    }

    companion object {
        private val levenshteinDistance = LevenshteinDistance(10)
    }
}