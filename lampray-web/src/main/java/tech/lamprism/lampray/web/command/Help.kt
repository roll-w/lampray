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

package tech.lamprism.lampray.web.command

import org.jline.utils.AttributedString
import org.springframework.shell.Utils
import org.springframework.shell.standard.AbstractShellComponent
import org.springframework.shell.standard.CommandValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import tech.lamprism.lampray.system.console.HelpRenderer
import tech.lamprism.lampray.system.console.shell.ShellCommandTree
import tech.lamprism.lampray.system.console.shell.command.HelpCommandProvider

private const val HEADER = "Lampray Command Line Interface"

/**
 * @author RollW
 */
@ShellComponent
class Help : AbstractShellComponent(), HelpCommandProvider {

    override fun displayHelp(vararg commands: String) {
        terminal.writer().println(getHelp(*commands).toAnsi())
    }

    override fun getHelp(vararg commands: String): AttributedString {
        return if (commands.isEmpty()) {
            renderCommands()
        } else {
            renderCommand(*commands)
        }
    }

    @ShellMethod(
        value = "Display help about available commands.",
        group = CommandGroups.COMMON,
        key = ["help"]
    )
    fun help(
        @ShellOption(
            defaultValue = ShellOption.NULL,
            valueProvider = CommandValueProvider::class,
            value = ["-C", "--command"],
            help = "The command to obtain help for.",
            arity = Int.MAX_VALUE
        ) command: Array<String>?
    ): AttributedString = getHelp(*(command ?: emptyArray()))

    private fun renderCommands(): AttributedString {
        val registrations = Utils
            .removeHiddenCommands(commandCatalog.registrations)
        val root = ShellCommandTree.of(registrations)
        val helpRenderer = HelpRenderer(root, header = HEADER)
        return helpRenderer.getHelp()
    }

    private fun renderCommand(vararg command: String): AttributedString {
        val registrations = Utils
            .removeHiddenCommands(commandCatalog.registrations)
        val root = ShellCommandTree.of(registrations)
        val helpRenderer = HelpRenderer(root, header = HEADER)
        return helpRenderer.getHelp(*command)
    }
}