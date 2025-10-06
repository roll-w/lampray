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
package tech.lamprism.lampray.system.console.shell.command.system

import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
import org.springframework.shell.component.StringInput
import org.springframework.shell.standard.AbstractShellComponent
import org.springframework.shell.table.AbsoluteWidthSizeConstraints
import org.springframework.shell.table.ArrayTableModel
import org.springframework.shell.table.BorderStyle
import org.springframework.shell.table.CellMatchers
import org.springframework.shell.table.SimpleHorizontalAligner
import org.springframework.shell.table.TableBuilder
import org.springframework.shell.table.TableModel
import org.springframework.stereotype.Component
import tech.lamprism.lampray.TimeAttributed
import tech.lamprism.lampray.setting.AttributedSettingSpecification
import tech.lamprism.lampray.setting.ConfigProvider
import tech.lamprism.lampray.setting.ConfigValue
import tech.lamprism.lampray.setting.SecretLevel
import tech.lamprism.lampray.setting.SettingDescriptionProvider
import tech.lamprism.lampray.setting.SettingSource
import tech.lamprism.lampray.setting.SettingSpecification.Companion.keyName
import tech.lamprism.lampray.setting.SettingSpecificationHelper.deserialize
import tech.lamprism.lampray.setting.SettingSpecificationProvider
import tech.lamprism.lampray.system.console.CommandGroups
import tech.lamprism.lampray.system.console.shell.command.HelpCommandProvider
import tech.lamprism.lampray.system.console.shell.command.HelpCommandProviderAware
import java.time.format.DateTimeFormatter

/**
 * Command for managing system configuration settings.
 * Provides functionality to list, show, set, and reset configuration values.
 *
 * @author RollW
 */
@Component
@Command(
    command = ["setting"],
    description = "Manage system configuration settings (list, show, set, reset)",
    group = CommandGroups.SYSTEM
)
class SettingCommand(
    private val configProvider: ConfigProvider,
    private val settingSpecificationProvider: SettingSpecificationProvider,
    private val settingDescriptionProvider: SettingDescriptionProvider
) : AbstractShellComponent(), HelpCommandProviderAware {

    private lateinit var helpCommandProvider: HelpCommandProvider

    override fun setHelpCommandProvider(helpCommandProvider: HelpCommandProvider) {
        this.helpCommandProvider = helpCommandProvider
    }

    @Command
    fun main() {
        helpCommandProvider.displayHelp("setting")
    }

    @Command(command = ["list"], description = "List all configuration settings with optional filtering and pagination")
    fun listSettings(
        @Option(
            longNames = ["page"],
            shortNames = ['p'],
            description = "Page number for pagination (default: 1)",
            defaultValue = "1"
        ) page: Int,
        @Option(
            longNames = ["size"],
            shortNames = ['s'],
            description = "Number of settings per page (default: 20)",
            defaultValue = "20"
        ) size: Int,
        @Option(
            longNames = ["filter"],
            shortNames = ['f'],
            description = "Filter settings by name (case-insensitive substring match)",
        ) filter: String? = null
    ) {
        try {
            val allSpecifications = settingSpecificationProvider.settingSpecifications
            val filteredSpecifications = if (filter?.isNotBlank() ?: false) {
                allSpecifications.filter { spec ->
                    spec.keyName.contains(filter, ignoreCase = true)
                }
            } else {
                allSpecifications
            }

            val sortedSpecifications = filteredSpecifications.sortedBy { it.key.name }
            val totalCount = sortedSpecifications.size
            val totalPages = (totalCount + size - 1) / size
            val validPage = page.coerceIn(1, maxOf(1, totalPages))
            val fromIndex = (validPage - 1) * size
            val toIndex = minOf(fromIndex + size, totalCount)
            val pageSpecifications = sortedSpecifications.subList(fromIndex, toIndex)

            if (pageSpecifications.isEmpty()) {
                val filterMsg = if (filter.isNullOrBlank()) "" else " matching filter: '$filter'"
                terminal.writer().println("No settings found$filterMsg.")
                return
            }

            val configValues = configProvider.list(pageSpecifications)
            val tableModel = configValues.collectToTableModel()

            val tableBuilder = TableBuilder(tableModel).apply {
                on(CellMatchers.column(0)).addSizer(AbsoluteWidthSizeConstraints(30))
                    .addAligner(SimpleHorizontalAligner.left)
                on(CellMatchers.column(1)).addSizer(AbsoluteWidthSizeConstraints(25))
                    .addAligner(SimpleHorizontalAligner.left)
                on(CellMatchers.column(2)).addSizer(AbsoluteWidthSizeConstraints(10))
                    .addAligner(SimpleHorizontalAligner.center)
                on(CellMatchers.column(3)).addSizer(AbsoluteWidthSizeConstraints(15))
                    .addAligner(SimpleHorizontalAligner.center)
                on(CellMatchers.column(4)).addSizer(AbsoluteWidthSizeConstraints(28))
                    .addAligner(SimpleHorizontalAligner.center)
            }.addFullBorder(BorderStyle.oldschool)

            terminal.writer().println(tableBuilder.build().render(110))
            terminal.writer().println(
                "Showing ${pageSpecifications.size} of $totalCount settings " +
                        "(Page $validPage/$totalPages)${if (filter?.isNotBlank() ?: false) " - Filter: '$filter'" else ""}"
            )
        } catch (e: Exception) {
            terminal.writer().println("Failed to list settings: ${e.message}")
        }
    }

    @Command(command = ["show"], description = "Show detailed information about a specific setting")
    fun showSetting(
        @Option(
            longNames = ["key"],
            shortNames = ['k'],
            description = "Setting key name to show details for",
            required = true
        ) key: String
    ) {
        try {
            val specification = settingSpecificationProvider.getSettingSpecification(key)

            val configValue = configProvider.getValue(specification)
            val secretLevel = if (specification.secret) SecretLevel.MEDIUM else SecretLevel.NONE
            val maskedValue = maskSecret(configValue.value, secretLevel)
            val description = settingDescriptionProvider.getSettingDescription(specification.description)

            val tableData = arrayOf(
                arrayOf("Key:", specification.key.name),
                arrayOf("Value:", maskedValue?.toString() ?: "null"),
                arrayOf("Type:", specification.key.type.toString()),
                arrayOf("Source:", configValue.source.name),
                arrayOf("Default:", specification.defaultValue?.toString() ?: "No default value"),
                arrayOf("Secret:", if (specification.secret) "Yes" else "No"),
                arrayOf("Description:", description),
                arrayOf(
                    "Updated:", if (configValue is TimeAttributed) {
                        configValue.getUpdateTime()?.format(DATE_FORMATTER) ?: "N/A"
                    } else {
                        "N/A"
                    }
                )
            )

            val tableBuilder = TableBuilder(ArrayTableModel(tableData)).apply {
                on(CellMatchers.column(0)).addSizer(AbsoluteWidthSizeConstraints(13))
                    .addAligner(SimpleHorizontalAligner.right)
                on(CellMatchers.column(1)).addSizer(AbsoluteWidthSizeConstraints(95))
                    .addAligner(SimpleHorizontalAligner.left)
            }

            terminal.writer().println("Setting Details:")
            terminal.writer().print(tableBuilder.build().render(110))
        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: ${e.message}")
        } catch (e: Exception) {
            terminal.writer().println("Failed to show setting: ${e.message}")
        }
    }

    @Command(command = ["set"], description = "Set the value of a configuration setting")
    fun setSetting(
        @Option(
            longNames = ["key"],
            shortNames = ['k'],
            description = "Setting key name to update",
            required = true
        ) key: String,
        @Option(
            longNames = ["value"],
            shortNames = ['v'],
            description = "New value for the setting",
            required = true
        ) value: String,
        @Option(
            longNames = ["force"],
            shortNames = ['f'],
            description = "Skip confirmation prompt for set settings"
        ) force: Boolean = false
    ) {
        try {
            val specification = settingSpecificationProvider.getSettingSpecification(key)

            // Show current value and ask for confirmation
            if (!force) {
                terminal.writer().println("You are about to update setting:")
                terminal.writer().println("Key: ${specification.key.name}")

                val stringInput = StringInput(
                    terminal,
                    "Are you sure you want to update this setting? ([Y/yes]/no): ",
                    "no"
                ) { context ->
                    val builder = AttributedStringBuilder()
                    builder.append(context.name)
                    if (context.resultValue != null) {
                        builder.append(context.resultValue)
                    } else {
                        context.input?.let { input -> builder.append(input) }
                            ?: builder.append(context.defaultValue, AttributedStyle.HIDDEN)
                    }
                    return@StringInput listOf(builder.toAttributedString())
                }
                val context = stringInput.run(StringInput.StringInputContext.empty())

                val confirmation = context.resultValue
                if (confirmation.isNullOrBlank() ||
                    (!confirmation.equals("yes", ignoreCase = true) && confirmation != "Y")
                ) {
                    terminal.writer().println("Set operation cancelled.")
                    return
                }
            }

            // Parse and validate the value
            @Suppress("UNCHECKED_CAST")
            val typedSpecification = specification as AttributedSettingSpecification<Any, Any>
            val parsedValue = try {
                value.deserialize(typedSpecification)
            } catch (e: Exception) {
                terminal.writer().println("Invalid value format for type ${specification.key.type}: ${e.message}")
                return
            }

            // Set the value
            val source = configProvider.set(typedSpecification, parsedValue)

            if (source != SettingSource.NONE) {
                terminal.writer().println("Successfully updated setting '$key' (stored in: ${source.name})")

                // Show the updated value (masked if secret)
                if (!specification.secret) {
                    terminal.writer().println("New value: $value")
                } else {
                    terminal.writer().println("New value: [HIDDEN]")
                }
            } else {
                terminal.writer()
                    .println("Failed to update setting '$key' - no writable configuration source available")
            }

        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: ${e.message}")
        } catch (e: Exception) {
            terminal.writer().println("Failed to set setting: ${e.message}")
        }
    }

    @Command(command = ["reset"], description = "Reset a configuration setting to its default value")
    fun resetSetting(
        @Option(
            longNames = ["key"],
            shortNames = ['k'],
            description = "Setting key name to reset",
            required = true
        ) key: String,
        @Option(
            longNames = ["force"],
            shortNames = ['f'],
            description = "Skip confirmation prompt"
        ) force: Boolean = false
    ) {
        try {
            val specification = settingSpecificationProvider.getSettingSpecification(key)

            val currentValue = configProvider.getValue(specification)
            val defaultValue = specification.defaultValue
            val secretLevel = if (specification.secret) SecretLevel.MEDIUM else SecretLevel.NONE

            // Show confirmation dialog
            if (!force) {
                terminal.writer().println("You are about to reset setting to default value:")
                terminal.writer().println("Key: ${specification.key.name}")
                terminal.writer().println("Current Value: ${maskSecret(currentValue.value, secretLevel)}")
                terminal.writer().println("Default Value: ${maskSecret(defaultValue, secretLevel)}")

                val stringInput = StringInput(
                    terminal,
                    "Are you sure you want to reset this setting? (yes/no): ",
                    "no"
                ) { context ->
                    val builder = AttributedStringBuilder()
                    builder.append(context.name)
                    if (context.resultValue != null) {
                        builder.append(context.resultValue)
                    } else {
                        context.input?.let { input -> builder.append(input) }
                            ?: builder.append(context.defaultValue, AttributedStyle.HIDDEN)
                    }
                    return@StringInput listOf(builder.toAttributedString())
                }
                val context = stringInput.run(StringInput.StringInputContext.empty())

                val confirmation = context.resultValue
                if (confirmation.isNullOrBlank() ||
                    (!confirmation.equals("yes", ignoreCase = true) && confirmation != "Y")
                ) {
                    terminal.writer().println("Reset operation cancelled.")
                    return
                }
            }

            // Reset to default value
            @Suppress("UNCHECKED_CAST")
            val typedSpecification = specification as AttributedSettingSpecification<Any, Any>
            val source = configProvider.set(typedSpecification, defaultValue)

            if (source != SettingSource.NONE) {
                terminal.writer()
                    .println("Successfully reset setting '$key' to default value (stored in: ${source.name})")
                // No need to mask default value for secret
                terminal.writer().println("Reset to: ${defaultValue ?: "null"}")
            } else {
                terminal.writer().println("Failed to reset setting '$key' - no writable configuration source available")
            }

        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: ${e.message}")
        } catch (e: Exception) {
            terminal.writer().println("Failed to reset setting: ${e.message}")
        }
    }

    private fun List<ConfigValue<*, *>>.collectToTableModel(): TableModel {
        val headers = arrayOf("Key", "Value", "Type", "Source", "Updated")
        val data = map { configValue ->
            val specification = configValue.specification as AttributedSettingSpecification<*, *>
            val secretLevel = if (specification.secret) SecretLevel.MEDIUM else SecretLevel.NONE
            val maskedValue = maskSecret(configValue.value, secretLevel)
            val updatedTime = if (configValue is TimeAttributed) {
                configValue.getUpdateTime()?.format(DATE_FORMATTER) ?: "N/A"
            } else {
                "N/A"
            }

            arrayOf(
                specification.key.name,
                maskedValue?.toString() ?: "null",
                specification.key.type.toString(),
                configValue.source.name,
                updatedTime
            )
        }
        return ArrayTableModel(arrayOf(headers) + data)
    }

    private fun maskSecret(value: Any?, secretLevel: SecretLevel): Any? {
        if (value == null || secretLevel == SecretLevel.NONE) {
            return value
        }
        val valueStr = value.toString()
        return secretLevel.maskValue(valueStr)
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
