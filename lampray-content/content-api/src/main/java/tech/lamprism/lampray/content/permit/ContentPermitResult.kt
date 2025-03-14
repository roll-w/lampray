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
package tech.lamprism.lampray.content.permit

import tech.rollw.common.web.ErrorCode

/**
 * @author RollW
 */
data class ContentPermitResult(
    val errors: Set<ErrorCode>,
    val isPermitted: Boolean
) {
    operator fun plus(other: ContentPermitResult): ContentPermitResult {
        val pluses = HashSet(errors)
        pluses.addAll(other.errors)
        if (isPermitted || other.isPermitted) {
            pluses.removeAll { it.success() }
        }
        return ContentPermitResult(
            pluses,
            isPermitted && other.isPermitted
        )
    }

    companion object {
        @JvmStatic
        fun permit(): ContentPermitResult {
            return ContentPermitResult(setOf(), true)
        }

        @JvmStatic
        fun deny(errorCode: ErrorCode): ContentPermitResult {
            check(errorCode.failed()) {
                "errorCode must be failed."
            }
            return ContentPermitResult(setOf(errorCode), false)
        }
    }
}
