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

package tech.rollw.common.value.formatter.support

import tech.rollw.common.value.formatter.ValueFormatter

/**
 * Formatter for Boolean values to String.
 *
 * @author RollW
 */
class BooleanToStringFormatter(
    private val trueValue: String = "true",
    private val falseValue: String = "false"
) : ValueFormatter<Boolean, String> {
    override fun format(value: Boolean): String {
        return if (value) trueValue else falseValue
    }

    companion object {
        @JvmStatic
        fun yesNo(): BooleanToStringFormatter = BooleanToStringFormatter("yes", "no")

        @JvmStatic
        fun onOff(): BooleanToStringFormatter = BooleanToStringFormatter("on", "off")

        @JvmStatic
        fun binary(): BooleanToStringFormatter = BooleanToStringFormatter("1", "0")
    }
}

