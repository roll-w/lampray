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

import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.Parser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.TerminalBuilder.SystemOutput
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.shell.Shell
import org.springframework.shell.boot.JLineAutoConfiguration
import org.springframework.shell.boot.JLineShellAutoConfiguration
import org.springframework.shell.boot.LineReaderAutoConfiguration
import org.springframework.shell.boot.ShellContextAutoConfiguration
import org.springframework.shell.boot.ShellRunnerAutoConfiguration
import org.springframework.shell.boot.SpringShellProperties
import org.springframework.shell.boot.StandardCommandsAutoConfiguration
import org.springframework.shell.command.CommandCatalog
import org.springframework.shell.command.annotation.CommandScan
import org.springframework.shell.context.ShellContext
import org.springframework.shell.jline.ExtendedDefaultParser
import org.springframework.shell.jline.PromptProvider
import tech.lamprism.lampray.system.console.shell.DefaultTerminalContext
import tech.lamprism.lampray.system.console.shell.MultiInteractiveShellRunner
import tech.lamprism.lampray.system.console.shell.TerminalContext
import tech.lamprism.lampray.system.console.shell.TerminalContextHolder
import tech.lamprism.lampray.system.console.shell.TerminalContextHolderStrategy
import tech.lamprism.lampray.system.console.shell.TerminalRegistry
import tech.lamprism.lampray.system.console.shell.ThreadLocalTerminalContextHolderStrategy
import tech.lamprism.lampray.system.console.shell.reader.LamprayLineReaderFactory
import tech.lamprism.lampray.system.console.shell.reader.LineReaderFactory
import tech.lamprism.lampray.system.console.shell.terminal.MultiTerminal

/**
 * @author RollW
 */
@Configuration
@EnableAutoConfiguration(
    exclude = [
        StandardCommandsAutoConfiguration::class,
        ShellRunnerAutoConfiguration::class,
        JLineAutoConfiguration::class,
        JLineShellAutoConfiguration::class,
        LineReaderAutoConfiguration::class,
        ShellContextAutoConfiguration::class
    ]
)
@CommandScan("tech.lamprism.lampray")
class ShellConfiguration {
    @Bean
    @Primary
    fun springShellProperties(): SpringShellProperties = SpringShellProperties().apply {
        interactive = interactive.apply {
            isEnabled = true
        }
        noninteractive = noninteractive.apply {
            isEnabled = false
        }
    }

    @Bean
    @Primary
    fun interactiveApplicationRunner(
        lineReader: LineReader,
        promptProvider: PromptProvider,
        shell: Shell,
        shellContext: ShellContext
    ): MultiInteractiveShellRunner {
        return MultiInteractiveShellRunner(lineReader, promptProvider, shell, shellContext)
    }

    @Bean(destroyMethod = "closeAll")
    fun terminal(): MultiTerminal {
        val builder = TerminalBuilder.builder()
            .name("System")
            .systemOutput(SystemOutput.SysOut)
        val systemTerminal = builder.build()
        return MultiTerminal(listOf(systemTerminal), 0)
    }

    @Bean
    fun shellContextHolderStrategy(
        terminalRegistry: TerminalRegistry
    ): TerminalContextHolderStrategy = ThreadLocalTerminalContextHolderStrategy {
        DefaultTerminalContext(terminalRegistry)
    }

    @Bean
    fun terminalContext(
        terminalContextHolderStrategy: TerminalContextHolderStrategy
    ): TerminalContext {
        return TerminalContextHolder().apply {
            TerminalContextHolder.setStrategy(terminalContextHolderStrategy)
        }
    }

    @Bean
    fun parser(): Parser = ExtendedDefaultParser().apply {
        isEofOnUnclosedQuote = true
        isEofOnEscapedNewLine = true
    }

    @Bean
    fun lineReaderFactory(
        terminal: Terminal,
        completer: Completer,
        parser: Parser,
        commandCatalog: CommandCatalog
    ) =
        LamprayLineReaderFactory(
            terminal, completer,
            parser, commandCatalog
        )

    @Bean
    fun lineReader(lineReaderFactory: LineReaderFactory): LineReader =
        lineReaderFactory.newLineReader()
}