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

package tech.lamprism.lampray.security.firewall.blocklist

import org.slf4j.info
import org.slf4j.logger
import tech.lamprism.lampray.security.firewall.IdentifierType
import tech.lamprism.lampray.security.firewall.RequestIdentifier
import java.io.File
import java.time.OffsetDateTime

/**
 * Represents a blocklist that is stored in a file.
 *
 * The blocklist is stored in the following format:
 *
 * ```
 * # type    identifier    expiration    reason
 * IP   127.0.0.1  2023-01-01T00:00:00Z   test
 * ```
 *
 * @author RollW
 */
class FileBlocklist(
    private val file: File
) : Blocklist {
    companion object {
        private val logger = logger<FileBlocklist>()
    }

    constructor(path: String) : this(File(path))

    private val delegate = InMemoryBlocklist()

    override fun addAll(entries: Collection<BlocklistEntry>) {
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
                "Load ${it.size} blocklist entries from file: ${file.absolutePath}"
            }
        }
    }

    private fun parseLine(line: String): BlocklistEntry {
        val line = line.trim()
        val splits = line.split(" ")
        if (splits.size < 3) {
            throw BlocklistFormatException("Invalid blocklist entry: '$line'")
        }
        val (type, identifier, reason, expiration) =
            if (splits.size == 4) splits else splits.reformatAsEntry()
        return buildBlocklistEntry(type, identifier, reason, expiration)
    }

    private fun buildBlocklistEntry(
        type: String,
        identifier: String,
        reason: String,
        expiration: String
    ): BlocklistEntry {
        try {
            return BlocklistEntry(
                identifier,
                IdentifierType.valueOf(type),
                reason,
                if (expiration.contentEquals("inf", true)) {
                    BlocklistEntry.INF
                } else {
                    OffsetDateTime.parse(expiration)
                }
            )
        } catch (e: Exception) {
            throw BlocklistFormatException(
                "Invalid blocklist entry: '$type $identifier $reason $expiration'",
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
            throw IllegalArgumentException("Invalid blocklist entry: '$this'")
        }
        val type = nonEmpty[0].first.trim()
        val identifier = nonEmpty[1].first.trim()
        val expiration = nonEmpty[2].first.trim()
        if (nonEmpty.size == 3) {
            return listOf(type, identifier, "", expiration)
        }
        val startIndex = nonEmpty[3].second
        val endIndex = nonEmpty.last().second
        val reason = subList(startIndex, endIndex + 1).joinToString(" ").trim()
        return listOf(type, identifier, reason, expiration)
    }

    override fun plus(item: BlocklistEntry) = apply {
        delegate.plus(item)
        flushToFile()
        return this
    }

    override fun minus(entry: BlocklistEntry) = apply {
        delegate.minus(entry)
        flushToFile()
        return this
    }

    override fun plusAssign(entry: BlocklistEntry) {
        delegate.plusAssign(entry)
        flushToFile()
    }

    override fun minusAssign(entry: BlocklistEntry) {
        delegate.minusAssign(entry)
        flushToFile()
    }

    override fun contains(requestIdentifier: RequestIdentifier): Boolean {
        return delegate.contains(requestIdentifier).also {
            flushToFile()
        }
    }

    override fun clear() {
        delegate.clear()
        flushToFile(emptyList<BlocklistEntry>().iterator())
    }

    override fun iterator(): Iterator<BlocklistEntry> {
        val snapshot = delegate.toList()
        return snapshot.iterator().also { _ ->
            flushToFile(snapshot.iterator())
        }
    }

    private fun flushToFile(
        iterator: Iterator<BlocklistEntry> = delegate.iterator()
    ) {
        if (!file.exists()) {
            file.createNewFile()
        }
        file.printWriter().use { writer ->
            writer.println("# type    identifier    expiration    reason")
            iterator.forEach {
                writer.println(
                    "${it.type} ${it.identifier} ${
                        if (it.expiration == BlocklistEntry.INF) "inf"
                        else it.expiration
                    } ${it.reason}"
                )
            }
        }
    }
}