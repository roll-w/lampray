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

import {Extension} from "@tiptap/core";

/**
 * Adds Tab/Shift-Tab for indentation and smart Backspace behavior.
 */
export const ListKeyboardShortcuts = Extension.create({
    name: "listKeyboardShortcuts",

    addKeyboardShortcuts() {
        return {
            // Tab to increase indent (sink)
            Tab: () => {
                const {state} = this.editor
                const {$anchor} = state.selection

                for (let i = $anchor.depth; i > 0; i--) {
                    const node = $anchor.node(i)
                    if (node.type.name === "listItem") {
                        return this.editor.commands.sinkListItem("listItem")
                    }
                }
                return false
            },

            // Shift-Tab to decrease indent (lift)
            "Shift-Tab": () => {
                const {state} = this.editor
                const {$anchor} = state.selection

                for (let i = $anchor.depth; i > 0; i--) {
                    const node = $anchor.node(i)
                    if (node.type.name === "listItem") {
                        return this.editor.commands.liftListItem("listItem")
                    }
                }

                return false
            },

            // Backspace at the start of list item
            Backspace: () => {
                const {state} = this.editor
                const {$anchor} = state.selection

                // Check if cursor is at the start of a node
                if ($anchor.parentOffset !== 0) {
                    return false
                }

                for (let i = $anchor.depth; i > 0; i--) {
                    const node = $anchor.node(i)

                    if (node.type.name === "listItem") {
                        const parentPos = $anchor.before(i)
                        const resolvedPos = state.doc.resolve(parentPos + 1)

                        if (resolvedPos.parentOffset === 0) {
                            // Try to lift first (reduce indent)
                            if (this.editor.commands.liftListItem("listItem")) {
                                return true
                            }
                            return this.editor.commands.clearNodes()
                        }
                    }
                }

                return false
            },
        }
    },
})

