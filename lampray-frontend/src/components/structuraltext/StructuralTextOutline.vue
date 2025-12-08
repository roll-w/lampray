<!--
  - Copyright (C) 2023-2025 RollW
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from "vue"
import type {StructuralText} from "./types"
import {extractDocumentOutline, flattenOutline} from "./composables/useStructureExtractor"

interface FlatLink {
    id: string
    text: string
    level: number
}

interface Props {
    /** The StructuralText document to generate outline from */
    document?: StructuralText
    /** Title for the outline section */
    title?: string
    /** Color theme for links */
    color?: "primary" | "secondary" | "success" | "info" | "warning" | "error" | "neutral"

    ui?: {
        title?: string
        container?: string
    }
}

const props = withDefaults(defineProps<Props>(), {
    title: "Outline",
    color: "primary"
})

const emit = defineEmits<{
    /** Emitted when user navigates to a heading */
    move: [id: string]
}>()

const outline = computed(() => {
    if (!props.document) return []
    return extractDocumentOutline(props.document)
})

const flatOutline = computed(() => flattenOutline(outline.value))

const links = computed<FlatLink[]>(() => {
    return flatOutline.value.map(node => ({
        id: node.id,
        text: node.text,
        level: node.level
    }))
})

const activeId = ref<string>("")
const activeIndex = ref<number>(-1)
const visibleHeadings = ref<Set<string>>(new Set())

const linksContainerRef = ref<HTMLElement | null>(null)

// Calculate indicator position and height based on actual DOM element
const indicatorStyle = computed(() => {
    if (activeIndex.value < 0 || !linksContainerRef.value) {
        return {display: 'none'}
    }

    // Find the active link element
    const activeLinkElement = linksContainerRef.value.querySelector(`[data-link-id="${activeId.value}"]`)
    if (!activeLinkElement) {
        return {display: 'none'}
    }

    // Get actual position and height from DOM
    const containerRect = linksContainerRef.value.getBoundingClientRect()
    const linkRect = activeLinkElement.getBoundingClientRect()

    const position = linkRect.top - containerRect.top
    const height = linkRect.height

    return {
        transform: `translateY(${position}px)`,
        height: `${height}px`
    }
})

const colorClasses = computed(() => {
    const map: Record<string, { text: string, indicator: string }> = {
        primary: {
            text: 'text-primary',
            indicator: 'bg-primary'
        },
        secondary: {
            text: 'text-secondary',
            indicator: 'bg-secondary'
        },
        success: {
            text: 'text-success',
            indicator: 'bg-success'
        },
        info: {
            text: 'text-info',
            indicator: 'bg-info'
        },
        warning: {
            text: 'text-warning',
            indicator: 'bg-warning'
        },
        error: {
            text: 'text-error',
            indicator: 'bg-error'
        },
        neutral: {
            text: 'text-gray-900 dark:text-gray-100',
            indicator: 'bg-gray-900 dark:bg-gray-100'
        }
    }
    return map[props.color] || map.primary
})

function scrollToHeading(id: string) {
    const element = document.getElementById(id)
    if (element) {
        element.scrollIntoView({behavior: "smooth", block: "start"})
        updateActiveHeading(id)
        emit("move", id)
        history.replaceState(null, "", `#${id}`)
    }
}

function updateActiveHeading(id: string) {
    activeId.value = id
    activeIndex.value = links.value.findIndex(link => link.id === id)

    // Trigger indicator position recalculation on next tick
    nextTick(() => {
        // Force recomputation by accessing the computed property
        if (linksContainerRef.value) {
            void indicatorStyle.value
        }
    })
}

// Find the topmost visible heading
function findTopmostVisibleHeading(): string | null {
    if (visibleHeadings.value.size === 0) {
        return null
    }

    // Find the heading with the smallest index (topmost in document)
    let topmostId: string | null = null
    let topmostIndex = Infinity

    for (const id of visibleHeadings.value) {
        const index = links.value.findIndex(link => link.id === id)
        if (index !== -1 && index < topmostIndex) {
            topmostIndex = index
            topmostId = id
        }
    }

    return topmostId
}

// Observe heading visibility for active state
let observer: IntersectionObserver | null = null

function setupIntersectionObserver() {
    if (typeof window === "undefined") return

    const options = {
        rootMargin: "-80px 0px -80% 0px",
        threshold: 0
    }

    observer = new IntersectionObserver((entries) => {
        // Update visible headings set
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                visibleHeadings.value.add(entry.target.id)
            } else {
                visibleHeadings.value.delete(entry.target.id)
            }
        })

        // Find and highlight the topmost visible heading
        const topmostId = findTopmostVisibleHeading()
        if (topmostId) {
            updateActiveHeading(topmostId)
        }
    }, options)

    // Observe all heading elements
    flatOutline.value.forEach(node => {
        const element = document.getElementById(node.id)
        if (element) {
            observer?.observe(element)
        }
    })
}

function cleanupObserver() {
    if (observer) {
        observer.disconnect()
        observer = null
    }
}

onMounted(() => {
    nextTick(() => {
        setupIntersectionObserver()
    })
})

onUnmounted(() => {
    cleanupObserver()
})

watch(outline, () => {
    cleanupObserver()
    nextTick(() => {
        setupIntersectionObserver()
    })
})


</script>

<template>
    <div>
        <div class="pb-3">
            <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3"
                :class="[props.ui?.title || '']">
                {{ title }}
            </h3>
            <USeparator/>
        </div>

        <nav v-if="links.length > 0" class="sticky top-6 overflow-y-auto"
             :class="[props.ui?.container || '']">
            <div class="relative">
                <div class="absolute left-0 top-0 bottom-0 w-px bg-gray-200 dark:bg-gray-800"/>
                <div class="absolute left-0 w-px transition-all duration-200 ease-out"
                     :class="colorClasses?.indicator || 'bg-primary'"
                     :style="indicatorStyle"
                />
                <ul ref="linksContainerRef" class="relative space-y-0.5">
                    <li v-for="link in links"
                        class="list-none"
                        :key="link.id"
                        :data-link-id="link.id"
                        :style="{ paddingLeft: `${(link.level - 1) * 0.75}rem` }">
                        <ULink
                                :to="`#${link.id}`"
                                :active="activeId === link.id"
                                raw
                                class="block py-1 pl-4 pr-2 text-sm transition-colors duration-200 rounded-r-md"
                                :active-class="[colorClasses?.text || 'text-primary', 'font-medium'].join(' ')"
                                inactive-class="text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                                @click.prevent="scrollToHeading(link.id)">
                            <span class="block truncate">{{ link.text }}</span>
                        </ULink>
                    </li>
                </ul>
            </div>
        </nav>
    </div>
</template>
