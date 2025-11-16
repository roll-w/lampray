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

package tech.rollw.common.value.formatter

/**
 * @author RollW
 */
interface ValueFormatter<T, R> {
    fun format(value: T): R

    fun <O> before(next: ValueFormatter<R, O>): ValueFormatter<T, O> =
        CompositeFormatter(this, next)

    data class CompositeFormatter<A, B, C>(
        val first: ValueFormatter<A, B>,
        val second: ValueFormatter<B, C>
    ) : ValueFormatter<A, C> {
        override fun format(value: A): C {
            val intermediate = first.format(value)
            return second.format(intermediate)
        }
    }
}