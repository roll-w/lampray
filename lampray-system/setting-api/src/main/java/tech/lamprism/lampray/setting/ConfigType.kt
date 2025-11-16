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

package tech.lamprism.lampray.setting

import tech.rollw.common.value.parser.IdentityValueParser
import tech.rollw.common.value.parser.ValueParser

/**
 * Configuration type definition.
 *
 * @param T the target type used in the application.
 * @author RollW
 */
data class ConfigType<T>(
    val parsers: Map<Class<*>, ValueParser<*, T>> = emptyMap(),
    val targetClass: Class<T>
) {
    override fun toString(): String {
        return "ConfigType<$targetClass>(parsers=${parsers.keys})"
    }

    operator fun get(rawValueType: Class<*>): ValueParser<*, T>? {
        return parsers[rawValueType]
    }

    /**
     * Parse the raw value to target type T. Shorthand for getting the parser and invoking it.
     *
     * @param rawValue the raw value to parse.
     * @return the parsed value of type T.
     * @throws IllegalArgumentException if no parser is found for the raw value's type.
     */
    fun parse(rawValue: Any): T {
        val parser = get(rawValue::class.java)
            ?: throw IllegalArgumentException(
                "No parser found for type: ${rawValue::class.java} to target type: $targetClass"
            )
        @Suppress("UNCHECKED_CAST")
        return (parser as ValueParser<Any, T>).parse(rawValue)
    }

    class Builder<T>(
        private var targetClass: Class<T>? = null
    ) {
        private val parsers: MutableMap<Class<*>, ValueParser<*, T>> = LinkedHashMap()

        fun <R> addParser(type: Class<R>, parser: ValueParser<R, T>): Builder<T> {
            parsers[type] = parser
            return this
        }

        inline fun <reified R> addParser(parser: ValueParser<R, T>): Builder<T> {
            return addParser(R::class.java, parser)
        }

        fun addParsers(map: Map<Class<*>, ValueParser<*, T>>): Builder<T> {
            for ((k, v) in map) {
                parsers[k] = v
            }
            return this
        }

        fun build(): ConfigType<T> {
            val tc = targetClass
                ?: throw IllegalStateException("targetClass is required to build ConfigType")
            return ConfigType(parsers = parsers.toMap(), targetClass = tc)
        }
    }

    companion object {
        @JvmStatic
        inline fun <reified R, reified T> of(parser: ValueParser<R, T>): ConfigType<T> {
            return ConfigType(
                parsers = mapOf(
                    R::class.java to parser
                ),
                T::class.java
            )
        }

        /**
         * Create a builder with explicit target class (Java-friendly).
         */
        @JvmStatic
        fun <T> builder(targetClass: Class<T>): Builder<T> {
            return Builder(targetClass)
        }

        /**
         * Kotlin reified helper to create a builder for T.
         */
        inline fun <reified T> builder(): Builder<T> {
            return Builder(T::class.java)
        }

        /**
         * Create a builder pre-populated from an existing ConfigType.
         */
        @JvmStatic
        fun <T> from(configType: ConfigType<T>): Builder<T> {
            val b = Builder(configType.targetClass)
            b.addParsers(configType.parsers)
            return b
        }

        @JvmField
        val STRING = of(IdentityValueParser.getInstance<String>())

        @JvmField
        val INT = of(IdentityValueParser.getInstance<Int>())

        @JvmField
        val LONG = of(IdentityValueParser.getInstance<Long>())

        @JvmField
        val FLOAT = of(IdentityValueParser.getInstance<Float>())

        @JvmField
        val DOUBLE = of(IdentityValueParser.getInstance<Double>())

        @JvmField
        val BOOLEAN = of(IdentityValueParser.getInstance<Boolean>())

        @JvmField
        val STRING_SET = of(IdentityValueParser.getInstance<Set<String>>())
    }
}