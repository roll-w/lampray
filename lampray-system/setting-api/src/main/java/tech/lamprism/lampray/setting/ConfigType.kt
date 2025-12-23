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

import tech.rollw.common.value.ValueCodec
import tech.rollw.common.value.formatter.IdentityValueFormatter
import tech.rollw.common.value.formatter.ValueFormatter
import tech.rollw.common.value.parser.IdentityValueParser
import tech.rollw.common.value.parser.ValueParser

/**
 * Configuration type definition.
 *
 * @param T the target type used in the application.
 * @param codecs the map of raw value types to their respective codecs.
 * @param targetClass the target class type.
 * @param preferredRawType the preferred raw type for serialization (default is String).
 * @author RollW
 */
data class ConfigType<T>(
    val codecs: Map<Class<*>, ValueCodec<*, T>> = emptyMap(),
    val targetClass: Class<T>,
    val preferredRawType: Class<*> = String::class.java
) {
    override fun toString(): String {
        return "ConfigType<$targetClass>(codecs=${codecs.keys})"
    }

    operator fun <R> get(rawValueType: Class<R>): ValueCodec<R, T>? {
        return codecs[rawValueType] as ValueCodec<R, T>?
    }

    /**
     * Parse the raw value to target type T. Shorthand for getting the parser and invoking it.
     *
     * @param rawValue the raw value to parse.
     * @return the parsed value of type T.
     * @throws IllegalArgumentException if no parser is found for the raw value's type.
     */
    fun <R> parse(rawValue: R): T {
        if (rawValue == null) {
            throw IllegalArgumentException("Raw value cannot be null for parsing to type: $targetClass")
        }
        val parser = get(rawValue::class.java) ?: throw IllegalArgumentException(
            "No parser found for raw type: ${rawValue::class.java} to target type: $targetClass"
        )
        @Suppress("UNCHECKED_CAST")
        return (parser as ValueParser<R, T>).parse(rawValue)
    }

    /**
     * Format value to a specific raw type.
     *
     * @param value the value to format.
     * @param rawClass the target raw type class.
     * @return the formatted value in the specified raw type.
     * @throws IllegalArgumentException if no formatter is found for the specified raw type.
     */
    fun <R> format(value: T, rawClass: Class<R>): R {
        val formatter = get(rawClass) ?: throw IllegalArgumentException(
            "No formatter found for target type: $targetClass to raw type: $rawClass"
        )
        @Suppress("UNCHECKED_CAST")
        return (formatter as ValueFormatter<T, R>).format(value)
    }

    class Builder<T>(
        private var targetClass: Class<T>? = null
    ) {
        private val parsers: MutableMap<Class<*>, ValueCodec<*, T>> = LinkedHashMap()
        private var preferredRawType: Class<*> = String::class.java

        fun <R> addCodec(type: Class<R>, parser: ValueCodec<R, T>): Builder<T> {
            parsers[type] = parser
            return this
        }

        inline fun <reified R> addCodec(parser: ValueCodec<R, T>): Builder<T> {
            return addCodec(R::class.java, parser)
        }

        fun addCodecs(map: Map<Class<*>, ValueCodec<*, T>>): Builder<T> {
            for ((k, v) in map) {
                parsers[k] = v
            }
            return this
        }

        fun setPreferredRawType(rawType: Class<*>): Builder<T> {
            this.preferredRawType = rawType
            return this
        }

        inline fun <reified R> setPreferredRawType(): Builder<T> {
            return setPreferredRawType(R::class.java)
        }

        fun build(): ConfigType<T> {
            val tc = targetClass
                ?: throw IllegalStateException("targetClass is required to build ConfigType")
            return ConfigType(
                codecs = parsers.toMap(),
                targetClass = tc,
                preferredRawType = preferredRawType
            )
        }
    }

    companion object {
        @JvmStatic
        inline fun <reified R, reified T> of(codec: ValueCodec<R, T>): ConfigType<T> {
            return ConfigType(
                codecs = mapOf(
                    R::class.java to codec
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
            b.addCodecs(configType.codecs)
            b.setPreferredRawType(configType.preferredRawType)
            return b
        }

        @JvmField
        val STRING = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<String>(), IdentityValueFormatter.getInstance<String>()
            )
        )

        @JvmField
        val INT = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<Int>(), IdentityValueFormatter.getInstance<Int>()
            )
        )

        @JvmField
        val LONG = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<Long>(), IdentityValueFormatter.getInstance<Long>()
            )
        )

        @JvmField
        val FLOAT = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<Float>(), IdentityValueFormatter.getInstance<Float>()
            )
        )

        @JvmField
        val DOUBLE = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<Double>(), IdentityValueFormatter.getInstance<Double>()
            )
        )

        @JvmField
        val BOOLEAN = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<Boolean>(), IdentityValueFormatter.getInstance<Boolean>()
            )
        )

        @JvmField
        val STRING_SET = of(
            ValueCodec.of(
                IdentityValueParser.getInstance<Set<String>>(),
                IdentityValueFormatter.getInstance<Set<String>>()
            )
        )
    }
}