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
package tech.lamprism.lampray.system.console.shell.command.security

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
import tech.lamprism.lampray.security.firewall.IdentifierType
import tech.lamprism.lampray.security.firewall.RequestIdentifier
import tech.lamprism.lampray.security.firewall.filtertable.FilterEntry
import tech.lamprism.lampray.security.firewall.filtertable.FilterMode
import tech.lamprism.lampray.security.firewall.filtertable.FilterTable
import tech.lamprism.lampray.system.console.CommandGroups
import tech.lamprism.lampray.system.console.shell.command.HelpCommandProvider
import tech.lamprism.lampray.system.console.shell.command.HelpCommandProviderAware
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Command for managing the system's filter table that controls access
 * by blocking or allowing specific identifiers like IP addresses or user IDs.
 *
 * @author RollW
 */
@Component
@Command(
    command = ["filtertable"],
    description = "Manage the system's filter table for blocking or allowing specific identifiers (IP addresses, user IDs, etc.)",
    group = CommandGroups.SECURITY
)
class FilterTableCommand(
    private val filterTable: FilterTable,
) : AbstractShellComponent(), HelpCommandProviderAware {
    private lateinit var helpCommandProvider: HelpCommandProvider

    override fun setHelpCommandProvider(helpCommandProvider: HelpCommandProvider) {
        this.helpCommandProvider = helpCommandProvider
    }

    @Command
    fun main() {
        helpCommandProvider.displayHelp("filtertable")
    }

    @Command(command = ["add"], description = "Add a new filter entry to block or allow specific identifiers")
    fun addEntry(
        @Option(
            longNames = ["identifier"],
            shortNames = ['i'],
            description = "Identifier value (IP address like '192.168.1.100', '192.168.100/24' or '2001:db8::/32', or user ID like '12345')",
            required = true
        ) identifier: String,
        @Option(
            longNames = ["type"],
            shortNames = ['t'],
            description = "Identifier type: 'IP' for IP addresses, 'USER' for user IDs",
            defaultValue = "IP"
        ) type: String,
        @Option(
            longNames = ["mode"],
            shortNames = ['m'],
            description = "Filter mode: 'DENY' to block access, 'ALLOW' to permit access",
            defaultValue = "DENY"
        ) mode: String,
        @Option(
            longNames = ["reason"],
            shortNames = ['r'],
            description = "Reason for this filter rule (for administrative reference)",
            defaultValue = "Manual added filter entry"
        ) reason: String,
        @Option(
            longNames = ["until"],
            shortNames = ['u'],
            description = "Expiration time or duration. Supports either a specific date-time in format 'yyyy-MM-dd HH:mm:ss' " +
                    "or a duration like '1h', '30m', '1d', '2d3h', '2h10m10s', 'inf' for permanent. Examples: '2024-12-31 23:59:59' or '2d3h'. " +
                    "If not provided, the rule will be permanent.",
        ) until: String = ""
    ) {
        try {
            val identifierType = IdentifierType.fromString(type)
            val filterMode = FilterMode.fromString(mode)
            val expirationTime = calculateExpirationTime(until)

            val entry = FilterEntry(identifier, identifierType, filterMode, expirationTime, reason)
            filterTable.plusAssign(entry)

            terminal.writer().println(
                "Successfully added filter entry: ${filterMode.name.lowercase()} $identifier [${identifierType.name}] expires at ${
                    formatDateTime(expirationTime)
                } - $reason"
            )
        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: ${e.message}")
        } catch (e: Exception) {
            terminal.writer().println("Failed to add filter entry: ${e.message}")
        }
    }

    @Command(command = ["remove"], description = "Remove an existing filter entry by identifier")
    fun removeEntry(
        @Option(
            longNames = ["identifier"],
            shortNames = ['i'],
            description = "Identifier value to remove (must match exactly)",
            required = true
        ) identifier: String,
        @Option(
            longNames = ["type"],
            shortNames = ['t'],
            description = "Identifier type: 'IP' for IP addresses, 'USER' for user IDs",
            defaultValue = "IP"
        ) type: String
    ) {
        try {
            val identifierType = IdentifierType.fromString(type)
            val requestIdentifier = RequestIdentifier(identifier, identifierType)

            val existingEntry = filterTable[requestIdentifier]
            if (existingEntry == null) {
                terminal.writer().println("No filter entry found for $identifier [${identifierType.name}]")
                return
            }

            filterTable.minusAssign(existingEntry)
            terminal.writer().println("Successfully removed filter entry: $identifier [${identifierType.name}]")
        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: ${e.message}")
        } catch (e: Exception) {
            terminal.writer().println("Failed to remove filter entry: ${e.message}")
        }
    }

    @Command(command = ["list"], description = "List all active filter entries in the filter table")
    fun listEntries() {
        val filterEntries = filterTable.toList()
        val tableModel = filterEntries.collectToTableModel()

        val tableBuilder = TableBuilder(tableModel).apply {
            on(CellMatchers.column(0)).addSizer(AbsoluteWidthSizeConstraints(14))
                .addAligner(SimpleHorizontalAligner.center)
            on(CellMatchers.column(1)).addSizer(AbsoluteWidthSizeConstraints(8))
                .addAligner(SimpleHorizontalAligner.center)
            on(CellMatchers.column(2)).addSizer(AbsoluteWidthSizeConstraints(10))
                .addAligner(SimpleHorizontalAligner.center)
            on(CellMatchers.column(3)).addSizer(AbsoluteWidthSizeConstraints(36))
                .addAligner(SimpleHorizontalAligner.center)
            on(CellMatchers.column(4)).addSizer(AbsoluteWidthSizeConstraints(30))
                .addAligner(SimpleHorizontalAligner.center)
        }.addFullBorder(BorderStyle.oldschool)

        terminal.writer().println(tableBuilder.build().render(110))
        terminal.writer().println("Total: ${filterEntries.size} filter entries")
    }

    private fun List<FilterEntry>.collectToTableModel(): TableModel {
        val headers = arrayOf("Identifier", "Type", "Mode", "Expiration", "Reason")
        val data = map { entry ->
            arrayOf(
                entry.identifier,
                entry.type.name,
                entry.mode.name,
                formatDateTime(entry.expiration),
                entry.reason
            )
        }
        return ArrayTableModel(arrayOf(headers) + data)
    }

    @Command(command = ["show"], description = "Show detailed information of a specific filter entry")
    fun showEntry(
        @Option(
            longNames = ["identifier"],
            shortNames = ['i'],
            description = "Identifier value to show details for",
            required = true
        ) identifier: String,
        @Option(
            longNames = ["type"],
            shortNames = ['t'],
            description = "Identifier type: 'IP' for IP addresses, 'USER' for user IDs",
            defaultValue = "IP"
        ) type: String
    ) {
        try {
            val identifierType = IdentifierType.fromString(type)
            val requestIdentifier = RequestIdentifier(identifier, identifierType)

            val entry = filterTable[requestIdentifier]
            if (entry == null) {
                terminal.writer().println("No filter entry found for $identifier [${identifierType.name}]")
                return
            }

            val tableData = arrayOf(
                arrayOf("Identifier:", entry.identifier),
                arrayOf("Type:", entry.type.name),
                arrayOf("Mode:", entry.mode.name),
                arrayOf("Expiration:", formatDateTime(entry.expiration)),
                arrayOf("Reason:", entry.reason)
            )

            val tableBuilder = TableBuilder(ArrayTableModel(tableData)).apply {
                on(CellMatchers.column(0)).addSizer(AbsoluteWidthSizeConstraints(13))
                    .addAligner(SimpleHorizontalAligner.right)
                on(CellMatchers.column(1)).addSizer(AbsoluteWidthSizeConstraints(95))
                    .addAligner(SimpleHorizontalAligner.left)
            }

            terminal.writer().println("Filter Entry Details:")
            terminal.writer().print(tableBuilder.build().render(110))
        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: ${e.message}")
        } catch (e: Exception) {
            terminal.writer().println("Failed to show filter entry: ${e.message}")
        }
    }

    @Command(
        command = ["clear"],
        description = "Clear all filter rules (use with caution - this will remove ALL filtering rules)"
    )
    fun clearAll() {
        try {
            val entries = filterTable.toList()

            if (entries.isEmpty()) {
                terminal.writer().println("No filter entries to clear.")
                return
            }

            terminal.writer()
                .println("You are about to clear ALL (${entries.size}) filter entries from the filter table.")
            val stringInput =
                StringInput(terminal, "Are you sure you want to clear all filter entries? (yes/no): ", "no") {
                    val builder = AttributedStringBuilder()
                    builder.append(it.name)
                    if (it.resultValue != null) {
                        builder.append(it.resultValue)
                    } else {
                        it.input?.let { input -> builder.append(input) }
                            ?: builder.append(it.defaultValue, AttributedStyle.HIDDEN)
                    }
                    return@StringInput listOf(builder.toAttributedString())
                }
            val context = stringInput.run(StringInput.StringInputContext.empty())

            val confirmation = context.resultValue
            if (confirmation.isNullOrBlank() || !confirmation.equals("yes", ignoreCase = true) && confirmation != "Y") {
                terminal.writer().println("Clear operation cancelled.")
                return
            }

            filterTable.clear()
            terminal.writer().println("Successfully cleared ${entries.size} filter entries from the filter table.")
        } catch (e: Exception) {
            terminal.writer().println("Failed to clear filter table: ${e.message}")
        }
    }

    /**
     * Calculate expiration time based on expires string or duration string
     */
    private fun calculateExpirationTime(time: String): OffsetDateTime {
        if (time.isBlank()) {
            // Default to permanent (max value)
            return FilterEntry.INF
        }
        return if (time.contains("-") || time.contains(":") || time.contains(" ")) {
            parseDateTime(time)
        } else {
            val now = OffsetDateTime.now()
            parseDuration(time, now)
        }
    }

    /**
     * Parse duration string supporting combinations like '2d3h', '2h10m10s', '1d', 'inf'
     */
    private fun parseDuration(duration: String, from: OffsetDateTime): OffsetDateTime {
        val normalizedDuration = duration.trim().lowercase(Locale.getDefault())

        if (normalizedDuration in setOf("inf", "infinite", "never")) {
            return FilterEntry.INF
        }

        return DurationParser.parse(normalizedDuration, from)
    }

    /**
     * Parse datetime string in format yyyy-MM-dd HH:mm:ss
     */
    private fun parseDateTime(dateTimeStr: String): OffsetDateTime {
        return try {
            val localDateTime = LocalDateTime.parse(
                dateTimeStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            )
            localDateTime.atOffset(OffsetDateTime.now().offset)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Invalid date format. Use format: yyyy-MM-dd HH:mm:ss")
        }
    }

    /**
     * Format OffsetDateTime for display
     */
    private fun formatDateTime(dateTime: OffsetDateTime): String {
        return if (dateTime.isEqual(FilterEntry.INF)) "Never"
        else dateTime.format(DATE_FORMATTER)
    }

    /**
     * Duration parser that supports composite duration expressions
     */
    private object DurationParser {
        private val UNIT_MULTIPLIERS = mapOf(
            's' to 1L,
            'm' to 60L,
            'h' to 3600L,
            'd' to 86400L
        )

        fun parse(duration: String, from: OffsetDateTime): OffsetDateTime {
            if (duration.isBlank()) {
                throw IllegalArgumentException("Duration cannot be empty")
            }

            var totalSeconds = 0L
            val currentNumber = StringBuilder()

            for (char in duration) {
                when {
                    char.isDigit() -> currentNumber.append(char)
                    char in UNIT_MULTIPLIERS -> {
                        if (currentNumber.isEmpty()) {
                            throw IllegalArgumentException("Invalid duration format: missing number before unit '$char'")
                        }

                        val value = currentNumber.toString().toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid number: $currentNumber")

                        val multiplier = UNIT_MULTIPLIERS[char]!!
                        totalSeconds += value * multiplier
                        currentNumber.clear()
                    }

                    char.isWhitespace() -> continue // Skip whitespace
                    else -> throw IllegalArgumentException(
                        "Invalid character in duration: '$char'. Supported units: ${
                            UNIT_MULTIPLIERS.keys.joinToString(
                                ", "
                            )
                        }"
                    )
                }
            }

            if (currentNumber.isNotEmpty()) {
                throw IllegalArgumentException("Duration ends with incomplete unit. Found number: $currentNumber")
            }

            if (totalSeconds <= 0) {
                throw IllegalArgumentException("Duration must be positive")
            }

            return from.plusSeconds(totalSeconds)
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
