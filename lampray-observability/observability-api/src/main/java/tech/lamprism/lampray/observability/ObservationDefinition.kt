/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.observability

/**
 * @author RollW
 */
data class ObservationDefinition private constructor(
    val name: String,
    val domain: SignalDomain,
    val lowCardinalityTags: List<ObservationTag>
) {

    fun withLowCardinalityTag(key: String?, value: String?): ObservationDefinition {
        return copy(lowCardinalityTags = java.util.List.copyOf(lowCardinalityTags + ObservationTag.of(key, value)))
    }

    companion object {
        @JvmStatic
        fun business(name: String?): ObservationDefinition {
            return ObservationDefinition(
                name = requireName(name),
                domain = SignalDomain.BUSINESS,
                lowCardinalityTags = java.util.List.of()
            )
        }

        @JvmStatic
        fun system(name: String?): ObservationDefinition {
            return ObservationDefinition(
                name = requireName(name),
                domain = SignalDomain.SYSTEM,
                lowCardinalityTags = java.util.List.of()
            )
        }

        @JvmStatic
        fun of(
            name: String?,
            domain: SignalDomain?,
            lowCardinalityTags: List<ObservationTag>?
        ): ObservationDefinition {
            return ObservationDefinition(
                name = requireName(name),
                domain = requireNotNull(domain) { "domain cannot be null" },
                lowCardinalityTags = java.util.List.copyOf(
                    lowCardinalityTags ?: throw NullPointerException("lowCardinalityTags cannot be null")
                )
            )
        }

        private fun requireName(name: String?): String {
            requireNotNull(name) { "name cannot be null" }
            require(name.trim().isNotEmpty()) { "name cannot be blank" }
            return name
        }
    }
}
