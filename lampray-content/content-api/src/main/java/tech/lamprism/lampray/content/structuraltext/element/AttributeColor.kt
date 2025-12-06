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

package tech.lamprism.lampray.content.structuraltext.element

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Color for attribute elements.
 *
 * @author RollW
 */
@ConsistentCopyVisibility
data class AttributeColor private constructor(val value: String) {

    @JsonValue
    fun toJson(): String = value

    override fun toString(): String = value

    companion object {
        // Predefined color constants
        @JvmField
        val YELLOW = AttributeColor("yellow")
        @JvmField
        val GREEN = AttributeColor("green")
        @JvmField
        val BLUE = AttributeColor("blue")
        @JvmField
        val PINK = AttributeColor("pink")
        @JvmField
        val ORANGE = AttributeColor("orange")
        @JvmField
        val PURPLE = AttributeColor("purple")
        @JvmField
        val RED = AttributeColor("red")
        @JvmField
        val LIME = AttributeColor("lime")
        @JvmField
        val TEAL = AttributeColor("teal")
        @JvmField
        val CYAN = AttributeColor("cyan")

        // light variants
        @JvmField
        val LIGHT_YELLOW = AttributeColor("light-yellow")
        @JvmField
        val LIGHT_GREEN = AttributeColor("light-green")
        @JvmField
        val LIGHT_BLUE = AttributeColor("light-blue")
        @JvmField
        val LIGHT_PINK = AttributeColor("light-pink")
        @JvmField
        val LIGHT_ORANGE = AttributeColor("light-orange")
        @JvmField
        val LIGHT_PURPLE = AttributeColor("light-purple")
        @JvmField
        val LIGHT_RED = AttributeColor("light-red")
        @JvmField
        val LIGHT_LIME = AttributeColor("light-lime")
        @JvmField
        val LIGHT_TEAL = AttributeColor("light-teal")
        @JvmField
        val LIGHT_CYAN = AttributeColor("light-cyan")

        // dark variants
        @JvmField
        val DARK_YELLOW = AttributeColor("dark-yellow")
        @JvmField
        val DARK_GREEN = AttributeColor("dark-green")
        @JvmField
        val DARK_BLUE = AttributeColor("dark-blue")
        @JvmField
        val DARK_PINK = AttributeColor("dark-pink")
        @JvmField
        val DARK_ORANGE = AttributeColor("dark-orange")
        @JvmField
        val DARK_PURPLE = AttributeColor("dark-purple")
        @JvmField
        val DARK_RED = AttributeColor("dark-red")
        @JvmField
        val DARK_LIME = AttributeColor("dark-lime")
        @JvmField
        val DARK_TEAL = AttributeColor("dark-teal")
        @JvmField
        val DARK_CYAN = AttributeColor("dark-cyan")

        private val BY_VALUE: Map<String, AttributeColor> = listOf(
            YELLOW, GREEN, BLUE, PINK, ORANGE, PURPLE, RED, LIME, TEAL, CYAN,
            LIGHT_YELLOW, LIGHT_GREEN, LIGHT_BLUE, LIGHT_PINK, LIGHT_ORANGE, LIGHT_PURPLE, LIGHT_RED, LIGHT_LIME, LIGHT_TEAL, LIGHT_CYAN,
            DARK_YELLOW, DARK_GREEN, DARK_BLUE, DARK_PINK, DARK_ORANGE, DARK_PURPLE, DARK_RED, DARK_LIME, DARK_TEAL, DARK_CYAN
        ).associateBy { it.toString() }

        @JvmStatic
        @JsonCreator
        fun fromString(name: String?): AttributeColor? {
            if (name == null) return null
            val key = name.trim().lowercase()
            return BY_VALUE[key] ?: throw IllegalArgumentException("Unsupported color: $name")
        }

        fun values(): List<AttributeColor> = BY_VALUE.values.toList()
    }
}