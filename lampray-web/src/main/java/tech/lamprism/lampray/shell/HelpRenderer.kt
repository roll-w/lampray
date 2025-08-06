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
package tech.lamprism.lampray.shell

import org.apache.commons.text.WordUtils
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

private const val LINE_LENGTH = 110

/**
 * Renders help information for commands.
 *
 * E.g.:
 *
 * ```text
 * HEADER OF HELP
 *
 * Usage: <COMMAND> [OPTIONS]
 *
 * Description of the command.
 *
 * Available Commands:
 *  command1      Description of command1
 *  command2      Description of command2
 *
 * Options:
 *     -o1, --option1 <VALUE=defaultValue>:
 *         Description of option1
 *     --option2 <VALUE>:
 *         Description of option2
 *
 * Use "help <COMMAND>" or "<COMMAND> --help" for more information about a given command.
 * ```
 *
 * @author RollW
 */
class HelpRenderer(
    private val root: CommandTree,
    private val header: String = "",
) {
    companion object {
        const val NO_PARAM = "NO_PARAM"
    }

    fun getHelp(vararg commands: String): AttributedString {
        return if (commands.isEmpty()) {
            renderCommands()
        } else {
            renderCommand(commands.joinToString(" ") { s ->
                s.trim { it <= ' ' }
            })
        }
    }

    private fun renderCommands(): AttributedString {
        return AttributedStringBuilder()
            .apply {
                renderCommand(root)
            }
            .toAttributedString()
    }

    private fun renderCommand(command: String): AttributedString {
        val commandTree = root.findChildByFullName(command)
            ?: return AttributedString("Unknown command '$command'")

        return AttributedStringBuilder().apply {
            renderCommand(commandTree)
        }.toAttributedString()
    }

    private fun AttributedStringBuilder.renderCommand(commandTree: CommandTree) {
        if (header.isNotEmpty()) {
            appendLine(header)
            appendLine()
        }

        fun getCommandName(tree: CommandTree): String {
            val fullName = tree.fullName.trim()
            if (fullName.isBlank()) {
                return ""
            }
            return "$fullName "
        }

        append("Usage: ")
        val command = if (commandTree.children.isNotEmpty()) {
            "<COMMAND> [OPTIONS]"
        } else {
            "[OPTIONS]"
        } + "\n"
        appendLine("${getCommandName(commandTree)}$command")
        renderDescription(commandTree)
        renderChildrenCommands(commandTree)
        renderOptions(commandTree)

        val helpCommand = if (commandTree.children.isEmpty())
            "${getCommandName(root)}<COMMAND>" else "${getCommandName(commandTree)}<COMMAND>"

        append("Use \"help $helpCommand\" or \"$helpCommand --help\" for more information about a given command.")
    }

    private fun AttributedStringBuilder.renderDescription(commandTree: CommandTree) =
        commandTree.description.let { it ->
            if (it == null) {
                return
            }

            it.trim().wrap(110).lines().forEach { line ->
                appendLine(line)
            }
            if (it.isNotEmpty()) {
                appendLine()
            }
        }

    private fun AttributedStringBuilder.renderChildrenCommands(commandTree: CommandTree) {
        if (commandTree.children.isEmpty()) {
            return
        }
        commandTree.children.groupBy { it.group }.forEach { (group, commands) ->
            append("${(group ?: "").ifEmpty { "Available Commands" }}: \n", AttributedStyle.DEFAULT)
            commands.forEach {
                if (it.isHidden) {
                    return@forEach
                }
                append("  ${it.name.padEnd(14, ' ')}")
                val descriptionPadding = if (it.name.length > 14) {
                    appendLine()
                    16
                } else 0

                if (it.description.isNullOrEmpty()) {
                    appendLine()
                } else {
                    it.description.trim().wrap(LINE_LENGTH - 16).lines().forEachIndexed { i, it ->
                        if (i == 0) {
                            appendLine("${" ".repeat(descriptionPadding)}$it")
                        } else {
                            appendLine("${" ".repeat(16)}$it")
                        }
                    }
                }
            }
            appendLine()
        }
    }

    private fun List<CommandSpecification.Option>.isOnlyHelp(): Boolean {
        return size == 1 && this.any { it -> it.names.any { it == "help" } }
    }

    private fun AttributedStringBuilder.renderOptions(commandTree: CommandTree) {
        if (commandTree.options.isEmpty() || commandTree.options.isOnlyHelp()) {
            return
        }
        appendLine("Options:")
        commandTree.options.forEach { it ->
            if (it.isHidden) {
                return@forEach
            }
            append("    ${it.names.sortedBy { it.length }.joinToString(", ")}")
            if (it.type != null && !it.label.isNullOrBlank() && it.label != NO_PARAM) {
                append(" <${it.label ?: "VALUE"}")
                if (it.defaultValue != null) {
                    append("=${it.defaultValue}>")
                } else {
                    append(">")
                }
            }

            it.description?.let { it ->
                if (it.isEmpty()) {
                    return@let
                }
                appendLine(":")
                it.trim().wrap(LINE_LENGTH - 8).lines().forEach { line ->
                    appendLine("${" ".repeat(8)}$line")
                }
            }
            append("\n")
        }
    }

    private fun String.wrap(length: Int): String {
        return WordUtils.wrap(this, length)
    }

    private fun AttributedStringBuilder.appendLine(line: String, style: AttributedStyle = AttributedStyle.DEFAULT) {
        append("$line\n", style)
    }
}
