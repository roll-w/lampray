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

package tech.lamprism.lampray.security.firewall.filtertable

import org.slf4j.info
import org.slf4j.logger
import tech.lamprism.lampray.security.firewall.IdentifierType
import tech.lamprism.lampray.security.firewall.RequestIdentifier
import java.io.File
import java.time.OffsetDateTime

/**
 * Represents a filterlist that is stored in a file.
 *
 * The filterlist is stored in the following format:
 *
 * ```
 * # type    identifier    mode    expiration    reason
 * IP   127.0.0.1   ALLOW   2023-01-01T00:00:00Z   test
 * ```
 *
 * @author RollW
 */
class FileFilterTable(
    private val file: File
) : FilterTable {
    companion object {
        private val logger = logger<FileFilterTable>()
    }

    constructor(path: String) : this(File(path))

    private val delegate = InMemoryFilterTable()

    override fun addAll(entries: Collection<FilterEntry>) {
        delegate.addAll(entries)
        flushToFile()
    }

    init {
        readFile()
    }

    private fun readFile() {
        if (!file.exists()) {
            return
        }
        file.bufferedReader().use { reader ->
            reader.lineSequence().mapNotNull {
                if (it.isBlank() || it.startsWith("#")) {
                    return@mapNotNull null
                }
                parseLine(it)
            }.toList()
        }.let {
            delegate.addAll(it)
            logger.info {
                "Load ${it.size} Filterlist entries from file: ${file.absolutePath}"
            }
        }
    }

    private fun parseLine(line: String): FilterEntry {
        val splits = line.trim().split(" ")
        if (splits.size < 4) {
            throw FilterTableFormatException("Invalid Filterlist entry: '$line'")
        }
        val (type, identifier, mode, expiration, reason) =
            if (splits.size == 5) splits else splits.reformatAsEntry()
        return buildFilterlistEntry(
            type = type,
            identifier = identifier,
            mode = FilterMode.fromString(mode),
            expiration = expiration,
            reason = reason
        )
    }

    private fun buildFilterlistEntry(
        type: String,
        identifier: String,
        mode: FilterMode,
        expiration: String,
        reason: String
    ): FilterEntry {
        try {
            return FilterEntry(
                identifier,
                IdentifierType.valueOf(type),
                mode,
                if (expiration.contentEquals("inf", true)) {
                    FilterEntry.INF
                } else {
                    OffsetDateTime.parse(expiration)
                },
                reason
            )
        } catch (e: Exception) {
            throw FilterTableFormatException(
                "Invalid Filterlist entry: '$type $identifier $mode $reason $expiration'",
                e
            )
        }
    }

    private fun List<String>.reformatAsEntry(): List<String> {
        val nonEmpty: List<Pair<String, Int>> = mapIndexedNotNull { index, s ->
            if (s.isNotBlank()) {
                s to index
            } else {
                null
            }
        }
        if (nonEmpty.size < 3) {
            throw IllegalArgumentException("Invalid Filterlist entry: '$this'")
        }
        val type = nonEmpty[0].first.trim()
        val identifier = nonEmpty[1].first.trim()
        val mode = nonEmpty[2].first.trim()
        val expiration = nonEmpty[3].first.trim()
        if (nonEmpty.size == 4) {
            return listOf(type, identifier, mode, expiration, "")
        }
        val startIndex = nonEmpty[4].second
        val endIndex = nonEmpty.last().second
        val reason = subList(startIndex, endIndex + 1).joinToString(" ").trim()
        return listOf(type, identifier, mode, expiration, reason)
    }

    override fun plus(entry: FilterEntry) = apply {
        delegate.plus(entry)
        flushToFile()
        return this
    }

    override fun minus(entry: FilterEntry) = apply {
        delegate.minus(entry)
        flushToFile()
        return this
    }

    override fun plusAssign(entry: FilterEntry) {
        delegate.plusAssign(entry)
        flushToFile()
    }

    override fun minusAssign(entry: FilterEntry) {
        delegate.minusAssign(entry)
        flushToFile()
    }

    override fun contains(requestIdentifier: RequestIdentifier): Boolean {
        return delegate.contains(requestIdentifier).also {
            flushToFile()
        }
    }

    override fun get(requestIdentifier: RequestIdentifier): FilterEntry? {
        return delegate[requestIdentifier].also {
            flushToFile()
        }
    }

    override fun clear() {
        delegate.clear()
        flushToFile(emptyList<FilterEntry>().iterator())
    }

    override fun iterator(): Iterator<FilterEntry> {
        val snapshot = delegate.toList()
        return snapshot.iterator().also { _ ->
            flushToFile(snapshot.iterator())
        }
    }

    private fun flushToFile(
        iterator: Iterator<FilterEntry> = delegate.iterator()
    ) {
        if (!file.exists()) {
            file.createNewFile()
        }
        file.printWriter().use { writer ->
            writer.println("# type    identifier    mode    expiration    reason")
            iterator.forEach {
                writer.println(
                    "${it.type} ${it.identifier} ${it.mode} ${
                        if (it.expiration == FilterEntry.INF) "inf"
                        else it.expiration
                    } ${it.reason}"
                )
            }
        }
    }
}