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

import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.regex.Pattern

/**
 * Command for managing the system's filter table that controls access
 * by blocking or allowing specific identifiers like IP addresses or user IDs.
 *
 * @author RollW
 */
@Component
@Command(
    command = ["filter"],
    description = "Manage the system's filter table for blocking or allowing specific identifiers (IP addresses, user IDs, etc.)",
    group = CommandGroups.SECURITY
)
class FilterTableCommand(private val filterTable: FilterTable) : AbstractShellComponent() {
    @Command(command = ["add"], description = "Add a new filter entry to block or allow specific identifiers")
    fun addEntry(
        @Option(
            longNames = ["identifier"],
            shortNames = ['i'],
            description = "Identifier value (IP address like '192.168.1.100', user ID like '12345', etc.)",
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
            defaultValue = "Manual rule"
        ) reason: String,
        @Option(
            longNames = ["expires"],
            shortNames = ['e'],
            description = "Expiration time in format: yyyy-MM-dd HH:mm:ss (optional, e.g., '2024-12-31 23:59:59')"
        ) expires: String?,
        @Option(
            longNames = ["duration"],
            shortNames = ['d'],
            description = "Duration for which the rule is valid (e.g., '1h', '30m', '1d', 'inf' for permanent). Overrides --expires if both are provided."
        ) duration: String?
    ) {
        val identifierType = IdentifierType.fromString(type)
        val filterMode = FilterMode.fromString(mode)

        val expirationTime = calculateExpirationTime(expires, duration)

        val entry = FilterEntry(identifier, identifierType, filterMode, expirationTime, reason)
        filterTable.plusAssign(entry)

        terminal.writer().println(
            String.format(
                "Successfully added filter entry: %s %s [%s] expires at %s - %s",
                filterMode.name.lowercase(Locale.getDefault()),
                identifier,
                identifierType.name,
                formatDateTime(expirationTime),
                reason
            )
        )
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
                terminal.writer().println(
                    String.format(
                        "No filter entry found for %s [%s]", identifier, identifierType.name
                    )
                )
                return
            }

            filterTable.minusAssign(existingEntry)
            terminal.writer().println(
                String.format(
                    "Successfully removed filter entry: %s [%s]", identifier, identifierType.name
                )
            )
        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: " + e.message)
        } catch (e: Exception) {
            terminal.writer().println("Failed to remove filter entry: " + e.message)
        }
    }

    @Command(command = ["list"], description = "List all active filter entries in the filter table")
    fun listEntries() {
        val filterEntries = filterTable.toList()
        val tableModel = filterEntries.collectToTableModel()
        terminal.writer().apply {
            println("Filter Table total ${filterEntries.size} blocks")
            val tableBuilder = TableBuilder(tableModel)
                .apply {
                    on(CellMatchers.column(0)).addSizer(AbsoluteWidthSizeConstraints(8))
                        .addAligner(SimpleHorizontalAligner.center)
                    on(CellMatchers.column(1)).addSizer(AbsoluteWidthSizeConstraints(14))
                        .addAligner(SimpleHorizontalAligner.center)
                    on(CellMatchers.column(2)).addSizer(AbsoluteWidthSizeConstraints(10))
                        .addAligner(SimpleHorizontalAligner.center)
                    on(CellMatchers.column(3)).addSizer(AbsoluteWidthSizeConstraints(36))
                        .addAligner(SimpleHorizontalAligner.center)
                    on(CellMatchers.column(4)).addSizer(AbsoluteWidthSizeConstraints(18))
                        .addAligner(SimpleHorizontalAligner.center)
                }.addFullBorder(BorderStyle.oldschool)
            println(tableBuilder.build().render(140))
        }

        terminal.writer().println(String.format("Total: %d filter entries", filterEntries.size))
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
                terminal.writer().println(
                    String.format(
                        "No filter entry found for %s [%s]", identifier, identifierType.name
                    )
                )
                return
            }

            terminal.writer().println("Filter Entry Details:")

            val array = arrayOf(
                arrayOf("Identifier", entry.identifier),
                arrayOf("Type", entry.type.name),
                arrayOf("Mode", entry.mode.name),
                arrayOf("Expiration", formatDateTime(entry.expiration)),
                arrayOf("Reason", entry.reason)
            )
            val tableModel = ArrayTableModel(array)
            val tableBuilder = TableBuilder(tableModel)
                .apply {
                    on(CellMatchers.column(0)).addSizer(AbsoluteWidthSizeConstraints(12))
                        .addAligner(SimpleHorizontalAligner.center)
                    on(CellMatchers.column(1)).addSizer(AbsoluteWidthSizeConstraints(54))
                        .addAligner(SimpleHorizontalAligner.left)
                }.addFullBorder(BorderStyle.air)
            terminal.writer().println("Filter Entry Details:")
            terminal.writer().println(tableBuilder.build().render(70))

        } catch (e: IllegalArgumentException) {
            terminal.writer().println("Error: " + e.message)
        } catch (e: Exception) {
            terminal.writer().println("Failed to show filter entry: " + e.message)
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

            filterTable.clear()
            terminal.writer().println(
                String.format(
                    "Successfully cleared %d filter entries from the filter table.", entries.size
                )
            )
        } catch (e: Exception) {
            terminal.writer().println("Failed to clear filter table: " + e.message)
        }
    }

    /**
     * Calculate expiration time based on expires string or duration string
     */
    private fun calculateExpirationTime(expires: String?, duration: String?): OffsetDateTime {
        val now = OffsetDateTime.now()

        // Duration takes precedence over expires
        if (duration != null && !duration.trim { it <= ' ' }.isEmpty()) {
            return parseDuration(duration, now)
        }

        if (expires != null && !expires.trim { it <= ' ' }.isEmpty()) {
            return parseDateTime(expires)
        }

        // Default to permanent (max value)
        return FilterEntry.INF
    }

    /**
     * Parse duration string like '1h', '30m', '1d', 'inf'
     */
    private fun parseDuration(duration: String, from: OffsetDateTime): OffsetDateTime {
        var duration = duration
        duration = duration.trim { it <= ' ' }.lowercase(Locale.getDefault())

        if ("inf" == duration || "infinite" == duration) {
            return FilterEntry.INF
        }

        require(
            DURATION_PATTERN.matcher(duration).matches()
        ) { "Invalid duration format. Use format like '1h', '30m', '1d', or 'inf'" }

        val value = duration.dropLast(1).toInt()
        return when (val unit = duration[duration.length - 1]) {
            's' -> from.plusSeconds(value.toLong())
            'm' -> from.plusMinutes(value.toLong())
            'h' -> from.plusHours(value.toLong())
            'd' -> from.plusDays(value.toLong())
            else -> throw IllegalArgumentException("Invalid duration unit: $unit")
        }
    }

    /**
     * Parse datetime string in format yyyy-MM-dd HH:mm:ss
     */
    private fun parseDateTime(dateTimeStr: String?): OffsetDateTime {
        try {
            return OffsetDateTime.parse(
                dateTimeStr + "Z",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX")
            )
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Invalid date format. Use format: yyyy-MM-dd HH:mm:ss")
        }
    }

    /**
     * Format OffsetDateTime for display
     */
    private fun formatDateTime(dateTime: OffsetDateTime): String {
        if (dateTime == FilterEntry.INF) {
            return "Never"
        }
        return dateTime.format(DATE_FORMATTER)
    }

    private fun List<FilterEntry>.collectToTableModel(): TableModel {
        val array = Array<Array<Any>>(size + 1) {
            if (it == 0) {
                arrayOf("Type", "Identifier", "Mode", "Expiration", "Reason")
            } else {
                val expiration = get(it - 1).expiration
                arrayOf(
                    get(it - 1).type.name,
                    get(it - 1).identifier,
                    get(it - 1).mode.name,
                    when (expiration) {
                        FilterEntry.INF -> "Never"
                        else -> expiration
                    },
                    get(it - 1).reason
                )
            }
        }
        return ArrayTableModel(array)
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val DURATION_PATTERN: Pattern = Pattern.compile("^(\\d+)([hmsd])$|^inf$")
    }
}
