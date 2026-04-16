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

import {Extension, type Editor, type Range} from "@tiptap/core"
import Suggestion, {exitSuggestion, type SuggestionProps} from "@tiptap/suggestion"
import {PluginKey} from "@tiptap/pm/state"
import {h, render, type AppContext, type VNode} from "vue"
import SlashCommandMenu from "@/components/structuraltext/SlashCommandMenu.vue"

export interface SlashCommandItem {
    id: string
    label: string
    description: string
    icon: string
    keywords: string[]
    onSelect: (editor: Editor, range: Range) => void
}

interface CreateSlashCommandExtensionOptions {
    appContext: AppContext | null
    items: SlashCommandItem[]
}

const slashCommandPluginKey = new PluginKey("slashCommand")

function normalizeSearchValue(value: string) {
    return value.trim().toLocaleLowerCase()
}

function isSlashQueryAtBlockStart(textBefore: string) {
    const trimmedText = textBefore.trimStart()
    if (!trimmedText.startsWith("/")) {
        return false
    }

    return !trimmedText.slice(1).includes("\n")
}

function filterItems(items: SlashCommandItem[], query: string) {
    const normalizedQuery = normalizeSearchValue(query)
    if (!normalizedQuery) {
        return items
    }

    return items.filter(item => {
        const searchTargets = [item.label, item.description, ...item.keywords]
        return searchTargets.some(target => normalizeSearchValue(target).includes(normalizedQuery))
    })
}

function clampIndex(index: number, length: number) {
    if (length <= 0) {
        return 0
    }

    if (index < 0) {
        return length - 1
    }

    if (index >= length) {
        return 0
    }

    return index
}

export function createSlashCommandExtension(options: CreateSlashCommandExtensionOptions) {
    return Extension.create({
        name: "slashCommand",

        addProseMirrorPlugins() {
            return [
                Suggestion<SlashCommandItem, SlashCommandItem>({
                    editor: this.editor,
                    pluginKey: slashCommandPluginKey,
                    char: "/",
                    allowSpaces: false,
                    startOfLine: false,
                    decorationClass: "slash-command-suggestion",
                    items: ({query}) => filterItems(options.items, query),
                    command: ({editor, range, props}) => {
                        props.onSelect(editor, range)
                    },
                    allow: ({editor, state, range}) => {
                        if (!state.selection.empty) {
                            return false
                        }

                        if (editor.isActive("codeBlock") || editor.isActive("table")) {
                            return false
                        }

                        const parentNode = state.selection.$from.parent
                        if (!parentNode.isTextblock) {
                            return false
                        }

                        const textBefore = parentNode.textBetween(0, state.selection.$from.parentOffset, "\n", "\n")
                        return isSlashQueryAtBlockStart(textBefore)
                    },
                    render: () => {
                        let container: HTMLDivElement | null = null
                        let vnode: VNode | null = null
                        let selectedIndex = 0
                        let currentProps: SuggestionProps<SlashCommandItem, SlashCommandItem> | null = null

                        const updatePosition = () => {
                            if (!container || !currentProps?.clientRect) {
                                return
                            }

                            const clientRect = currentProps.clientRect()
                            if (!clientRect) {
                                return
                            }

                            const offset = 10
                            const viewportPadding = 12
                            const width = container.offsetWidth || 352
                            const height = container.offsetHeight || 320
                            const maxLeft = window.innerWidth - viewportPadding - width
                            const desiredLeft = Math.max(viewportPadding, Math.min(clientRect.left, maxLeft))
                            let desiredTop = clientRect.bottom + offset

                            if (desiredTop + height > window.innerHeight - viewportPadding) {
                                desiredTop = Math.max(viewportPadding, clientRect.top - height - offset)
                            }

                            container.style.left = `${desiredLeft}px`
                            container.style.top = `${desiredTop}px`
                        }

                        const renderMenu = () => {
                            if (!container || !currentProps) {
                                return
                            }

                            vnode = h(SlashCommandMenu, {
                                items: currentProps.items,
                                selectedIndex,
                                onSelect: (item: SlashCommandItem) => currentProps?.command(item),
                                onHighlight: (index: number) => {
                                    selectedIndex = index
                                    renderMenu()
                                },
                            })

                            if (options.appContext && vnode) {
                                vnode.appContext = options.appContext
                            }

                            render(vnode, container)
                            updatePosition()
                        }

                        const destroyMenu = () => {
                            if (container) {
                                render(null, container)
                                container.remove()
                            }

                            container = null
                            vnode = null
                            currentProps = null
                            selectedIndex = 0
                        }

                        return {
                            onStart: props => {
                                currentProps = props
                                selectedIndex = 0
                                container = document.createElement("div")
                                container.className = "slash-command-menu"
                                container.style.position = "fixed"
                                container.style.zIndex = "70"
                                container.addEventListener("mousedown", event => event.preventDefault())
                                document.body.appendChild(container)
                                renderMenu()
                            },
                            onUpdate: props => {
                                const previousQuery = currentProps?.query || ""
                                currentProps = props
                                if (props.query !== previousQuery) {
                                    selectedIndex = 0
                                } else {
                                    selectedIndex = clampIndex(selectedIndex, props.items.length)
                                }

                                renderMenu()
                            },
                            onKeyDown: props => {
                                if (!currentProps) {
                                    return false
                                }

                                if (props.event.key === "ArrowUp") {
                                    props.event.preventDefault()
                                    selectedIndex = clampIndex(selectedIndex - 1, currentProps.items.length)
                                    renderMenu()
                                    return true
                                }

                                if (props.event.key === "ArrowDown") {
                                    props.event.preventDefault()
                                    selectedIndex = clampIndex(selectedIndex + 1, currentProps.items.length)
                                    renderMenu()
                                    return true
                                }

                                if (props.event.key === "Enter") {
                                    const selectedItem = currentProps.items[selectedIndex]
                                    if (!selectedItem) {
                                        return false
                                    }

                                    props.event.preventDefault()
                                    currentProps.command(selectedItem)
                                    return true
                                }

                                if (props.event.key === "Escape") {
                                    props.event.preventDefault()
                                    exitSuggestion(props.view, slashCommandPluginKey)
                                    return true
                                }

                                return false
                            },
                            onExit: () => {
                                destroyMenu()
                            },
                        }
                    },
                }),
            ]
        },
    })
}

export {slashCommandPluginKey}
