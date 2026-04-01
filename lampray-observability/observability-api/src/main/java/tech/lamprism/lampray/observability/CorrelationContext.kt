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
data class CorrelationContext private constructor(
    val requestId: String,
    val traceId: String?,
    val spanId: String?
) {

    companion object {
        @JvmStatic
        @JvmOverloads
        fun of(
            requestId: String?,
            traceId: String? = null,
            spanId: String? = null
        ): CorrelationContext {
            return CorrelationContext(
                requestId = requireText(requestId, "requestId"),
                traceId = normalize(traceId),
                spanId = normalize(spanId)
            )
        }

        private fun requireText(value: String?, fieldName: String): String {
            requireNotNull(value) { "$fieldName cannot be null" }
            require(value.trim().isNotEmpty()) { "$fieldName cannot be blank" }
            return value
        }

        private fun normalize(value: String?): String? {
            return value?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
