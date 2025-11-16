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

package tech.rollw.common.value.parser

/**
 * Parser for converting raw config values to typed objects.
 *
 * @param R The raw type of the value.
 * @param T The type that the value will be converted to.
 * @author RollW
 */
@JvmDefaultWithoutCompatibility
interface ValueParser<R, T> {
    /**
     * Parse the raw value to the target type.
     *
     * @param value the raw value
     * @return the parsed value
     * @throws ValueParseException if the value cannot be parsed
     */
    fun parse(value: R): T

    fun <O> then(next: ValueParser<T, O>): ValueParser<R, O> =
        CompositeParser(this, next)

    data class CompositeParser<A, B, C>(
        val first: ValueParser<A, B>,
        val second: ValueParser<B, C>
    ) : ValueParser<A, C> {
        override fun parse(value: A): C {
            val intermediate = first.parse(value)
            return second.parse(intermediate)
        }
    }
}