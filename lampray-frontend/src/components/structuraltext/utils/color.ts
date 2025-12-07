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

import type {AttributeColor} from "@/components/structuraltext/types"

export interface ColorDefinition {
    name: AttributeColor
    backgroundClass: string
    hoverClass?: string
}

/**
 * Available colors for table cells and other attributes.
 */
export const ATTRIBUTE_COLORS: ColorDefinition[] = [
    {
        name: "yellow",
        backgroundClass: "bg-yellow-200 dark:bg-yellow-800",
        hoverClass: "hover:bg-yellow-300 dark:hover:bg-yellow-700"
    },
    {
        name: "green",
        backgroundClass: "bg-green-200 dark:bg-green-800",
        hoverClass: "hover:bg-green-300 dark:hover:bg-green-700"
    },
    {
        name: "blue",
        backgroundClass: "bg-blue-200 dark:bg-blue-800",
        hoverClass: "hover:bg-blue-300 dark:hover:bg-blue-700"
    },
    {
        name: "pink",
        backgroundClass: "bg-pink-200 dark:bg-pink-800",
        hoverClass: "hover:bg-pink-300 dark:hover:bg-pink-700"
    },
    {
        name: "orange",
        backgroundClass: "bg-orange-200 dark:bg-orange-800",
        hoverClass: "hover:bg-orange-300 dark:hover:bg-orange-700"
    },
    {
        name: "purple",
        backgroundClass: "bg-purple-200 dark:bg-purple-800",
        hoverClass: "hover:bg-purple-300 dark:hover:bg-purple-700"
    },
    {
        name: "red",
        backgroundClass: "bg-red-200 dark:bg-red-800",
        hoverClass: "hover:bg-red-300 dark:hover:bg-red-700"
    },
    {
        name: "lime",
        backgroundClass: "bg-lime-200 dark:bg-lime-800",
        hoverClass: "hover:bg-lime-300 dark:hover:bg-lime-700"
    },
    {
        name: "teal",
        backgroundClass: "bg-teal-200 dark:bg-teal-800",
        hoverClass: "hover:bg-teal-300 dark:hover:bg-teal-700"
    },
    {
        name: "cyan",
        backgroundClass: "bg-cyan-200 dark:bg-cyan-800",
        hoverClass: "hover:bg-cyan-300 dark:hover:bg-cyan-700"
    },
    {
        name: "light-yellow",
        backgroundClass: "bg-yellow-100 dark:bg-yellow-900",
        hoverClass: "hover:bg-yellow-200 dark:hover:bg-yellow-800"
    },
    {
        name: "light-green",
        backgroundClass: "bg-green-100 dark:bg-green-900",
        hoverClass: "hover:bg-green-200 dark:hover:bg-green-800"
    },
    {
        name: "light-blue",
        backgroundClass: "bg-blue-100 dark:bg-blue-900",
        hoverClass: "hover:bg-blue-200 dark:hover:bg-blue-800"
    },
    {
        name: "light-pink",
        backgroundClass: "bg-pink-100 dark:bg-pink-900",
        hoverClass: "hover:bg-pink-200 dark:hover:bg-pink-800"
    },
    {
        name: "light-orange",
        backgroundClass: "bg-orange-100 dark:bg-orange-900",
        hoverClass: "hover:bg-orange-200 dark:hover:bg-orange-800"
    },
    {
        name: "light-purple",
        backgroundClass: "bg-purple-100 dark:bg-purple-900",
        hoverClass: "hover:bg-purple-200 dark:hover:bg-purple-800"
    },
    {
        name: "light-red",
        backgroundClass: "bg-red-100 dark:bg-red-900",
        hoverClass: "hover:bg-red-200 dark:hover:bg-red-800"
    },
    {
        name: "light-lime",
        backgroundClass: "bg-lime-100 dark:bg-lime-900",
        hoverClass: "hover:bg-lime-200 dark:hover:bg-lime-800"
    },
    {
        name: "light-teal",
        backgroundClass: "bg-teal-100 dark:bg-teal-900",
        hoverClass: "hover:bg-teal-200 dark:hover:bg-teal-800"
    },
    {
        name: "light-cyan",
        backgroundClass: "bg-cyan-100 dark:bg-cyan-900",
        hoverClass: "hover:bg-cyan-200 dark:hover:bg-cyan-800"
    },
    {
        name: "dark-yellow",
        backgroundClass: "bg-yellow-300 dark:bg-yellow-700",
        hoverClass: "hover:bg-yellow-400 dark:hover:bg-yellow-600"
    },
    {
        name: "dark-green",
        backgroundClass: "bg-green-300 dark:bg-green-700",
        hoverClass: "hover:bg-green-400 dark:hover:bg-green-600"
    },
    {
        name: "dark-blue",
        backgroundClass: "bg-blue-300 dark:bg-blue-700",
        hoverClass: "hover:bg-blue-400 dark:hover:bg-blue-600"
    },
    {
        name: "dark-pink",
        backgroundClass: "bg-pink-300 dark:bg-pink-700",
        hoverClass: "hover:bg-pink-400 dark:hover:bg-pink-600"
    },
    {
        name: "dark-orange",
        backgroundClass: "bg-orange-300 dark:bg-orange-700",
        hoverClass: "hover:bg-orange-400 dark:hover:bg-orange-600"
    },
    {
        name: "dark-purple",
        backgroundClass: "bg-purple-300 dark:bg-purple-700",
        hoverClass: "hover:bg-purple-400 dark:hover:bg-purple-600"
    },
    {
        name: "dark-red",
        backgroundClass: "bg-red-300 dark:bg-red-700",
        hoverClass: "hover:bg-red-400 dark:hover:bg-red-600"
    },
    {
        name: "dark-lime",
        backgroundClass: "bg-lime-300 dark:bg-lime-700",
        hoverClass: "hover:bg-lime-400 dark:hover:bg-lime-600"
    },
    {
        name: "dark-teal",
        backgroundClass: "bg-teal-300 dark:bg-teal-700",
        hoverClass: "hover:bg-teal-400 dark:hover:bg-teal-600"
    },
    {
        name: "dark-cyan",
        backgroundClass: "bg-cyan-300 dark:bg-cyan-700",
        hoverClass: "hover:bg-cyan-400 dark:hover:bg-cyan-600"
    }
]

/**
 * Get background color class for an attribute color.
 *
 * @param color - The attribute color name
 */
export function getBackgroundColorClass(color: AttributeColor): string {
    const colorDef = ATTRIBUTE_COLORS.find(c => c.name === color)
    return colorDef?.backgroundClass || ""
}

/**
 * Get hover class for an attribute color.
 *
 * @param color - The attribute color name
 */
export function getHoverColorClass(color: AttributeColor): string {
    const colorDef = ATTRIBUTE_COLORS.find(c => c.name === color)
    return colorDef?.hoverClass || ""
}

export const BASIC_COLORS: ColorDefinition[] = ATTRIBUTE_COLORS

