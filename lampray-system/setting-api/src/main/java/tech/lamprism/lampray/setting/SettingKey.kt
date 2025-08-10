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

private val PARAMETER_REGEX = Regex.fromLiteral("\\[\\w+\\]")

/**
 * The key of a setting.
 *
 * Allow parameters in [name], such as `key.[param1].[param2]`.
 *
 * @param name the name of the setting key, can contain parameters in the format of `[param]` otherwise the `[`and `]` will be
 * considered as illegal characters.
 * @see SettingType
 * @author RollW
 */
data class SettingKey<T, V> @JvmOverloads constructor(
    val name: String,
    val type: SettingType<T, V>,
    val parameters: List<Parameter> = emptyList(),
) {
    data class Parameter(
        val name: String,
        val values: List<String> = emptyList(),
    )

    init {
        require(name.isNotEmpty()) { "Name must not be empty" }
        require(name.all {
            it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' ||
                    it == '[' || it == ']'
        }) {
            "Name must only contain letters, digits, '.', '_', '-', '[', ']'"
        }
        // TODO: check parameters

        val parametersDefinedInName = PARAMETER_REGEX.findAll(name)
            .map { it.value.substring(1, it.value.length - 1) }
            .toSet()

        require(parametersDefinedInName.all { param ->
            parameters.any { it.name == param }
        }) {
            "Parameters defined in name must be defined in parameters: $parametersDefinedInName"
        }
    }

    private fun List<Parameter>.removeUsed(names: List<String>): List<Parameter> {
        return filter { it.name !in names }
    }

    fun withParameters(parameters: Map<String, String>): SettingKey<T, V> {
        if (!hasParameters()) {
            return this
        }
        return SettingKey(
            name = replaceParameters(parameters),
            type = type,
            parameters = this.parameters.removeUsed(parameters.keys.toList())
        )
    }

    fun withParameters(vararg parameters: Pair<String, String>): SettingKey<T, V> {
        return withParameters(parameters.toMap())
    }

    fun withParameter(name: String, value: String): SettingKey<T, V> {
        return withParameters(mapOf(name to value))
    }

    private fun replaceParameters(parameters: Map<String, String>): String {
        var replaced = name
        for ((key, value) in parameters) {
            // TODO: check if placeholder is valid
            replaced = replaced.replace("[$key]", value)
        }
        return replaced
    }

    private fun hasParameters(): Boolean {
        // Check has any format like [*]
        return name.contains(PARAMETER_REGEX)
    }

    fun isTemplate(): Boolean {
        return parameters.isNotEmpty() || hasParameters()
    }

    class Builder<T, V> {
        constructor(type: SettingType<T, V>) {
            this.type = type
        }

        constructor(name: String, type: SettingType<T, V>) {
            this.type = type
            this.name = name
        }

        private val type: SettingType<T, V>
        private var name: String = ""
        private var parameters: List<Parameter>? = null

        fun setName(name: String) = apply {
            this.name = name
        }

        fun setParameters(parameters: List<Parameter>) = apply {
            this.parameters = parameters.toMutableList()
        }

        fun setParameters(vararg parameters: Parameter) = apply {
            this.parameters = parameters.toMutableList()
        }

        fun addParameter(name: String, values: List<String>) = apply {
            if (parameters == null) {
                parameters = mutableListOf()
            }
            (parameters as MutableList).add(Parameter(name, values))
        }

        fun addParameter(name: String, vararg values: String) = apply {
            if (parameters == null) {
                parameters = mutableListOf()
            }
            (parameters as MutableList).add(Parameter(name, values.toList()))
        }

        fun addParameter(name: String) = apply {
            if (parameters == null) {
                parameters = mutableListOf()
            }
            (parameters as MutableList).add(Parameter(name))
        }

        fun build(): SettingKey<T, V> {
            return SettingKey(name, type, parameters ?: emptyList())
        }

        companion object {

            @JvmStatic
            fun <T, V> of(name: String, type: SettingType<T, V>): Builder<T, V> {
                return Builder(name, type)
            }

            @JvmStatic
            fun ofString(name: String): Builder<String, String> {
                return of(name, SettingType.STRING)
            }

            @JvmStatic
            fun ofStringSet(name: String): Builder<Set<String>, String> {
                return of(name, SettingType.STRING_SET)
            }

            @JvmStatic
            fun ofInt(name: String): Builder<Int, Int> {
                return of(name, SettingType.INT)
            }

            @JvmStatic
            fun ofLong(name: String): Builder<Long, Long> {
                return of(name, SettingType.LONG)
            }

            @JvmStatic
            fun ofFloat(name: String): Builder<Float, Float> {
                return of(name, SettingType.FLOAT)
            }

            @JvmStatic
            fun ofDouble(name: String): Builder<Double, Double> {
                return of(name, SettingType.DOUBLE)
            }

            @JvmStatic
            fun ofBoolean(name: String): Builder<Boolean, Boolean> {
                return of(name, SettingType.BOOLEAN)
            }
        }
    }

    companion object {
        @JvmStatic
        fun <T, V> builder(type: SettingType<T, V>): Builder<T, V> {
            return Builder(type)
        }

        @JvmStatic
        fun <T, V> of(name: String, type: SettingType<T, V>): SettingKey<T, V> {
            return SettingKey(name, type)
        }

        @JvmStatic
        fun ofString(name: String): SettingKey<String, String> {
            return of(name, SettingType.STRING)
        }

        @JvmStatic
        fun ofStringSet(name: String): SettingKey<Set<String>, String> {
            return of(name, SettingType.STRING_SET)
        }

        @JvmStatic
        fun ofInt(name: String): SettingKey<Int, Int> {
            return of(name, SettingType.INT)
        }

        @JvmStatic
        fun ofLong(name: String): SettingKey<Long, Long> {
            return of(name, SettingType.LONG)
        }

        @JvmStatic
        fun ofFloat(name: String): SettingKey<Float, Float> {
            return of(name, SettingType.FLOAT)
        }

        @JvmStatic
        fun ofDouble(name: String): SettingKey<Double, Double> {
            return of(name, SettingType.DOUBLE)
        }

        @JvmStatic
        fun ofBoolean(name: String): SettingKey<Boolean, Boolean> {
            return of(name, SettingType.BOOLEAN)
        }
    }
}
