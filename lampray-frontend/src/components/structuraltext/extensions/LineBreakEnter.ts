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
import {Plugin, PluginKey} from "@tiptap/pm/state";

/**
 * Applies enter key behavior as follows:
 * 
 * - First Enter: Insert a hard break (line break within paragraph)
 * - Second Enter (consecutive): Create a new paragraph
 *
 * @author RollW
 */
export const LineBreakEnter = Extension.create({
    name: "lineBreakEnter",

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey("lineBreakEnter"),
                props: {
                    handleKeyDown: (view, event) => {
                        if (event.key !== "Enter" || event.shiftKey || event.ctrlKey || event.metaKey) {
                            return false
                        }

                        const {state} = view
                        const {$from} = state.selection

                        // Don't handle Enter in lists (bullet list, ordered list, task list)
                        // Let the list extensions handle it
                        for (let i = $from.depth; i > 0; i--) {
                            const node = $from.node(i)
                            if (node.type.name === "listItem" || node.type.name === "taskItem") {
                                return false
                            }
                        }

                        // Don't handle Enter in code blocks
                        if ($from.parent.type.name === "codeBlock") {
                            return false
                        }

                        // Check if we're in a paragraph
                        if ($from.parent.type.name !== "paragraph") {
                            return false
                        }

                        // Check if the last character before cursor is a newline (hard break)
                        // This indicates a consecutive Enter press
                        const lastNode = $from.nodeBefore
                        const isAfterHardBreak = lastNode && lastNode.type.name === "hardBreak"

                        if (isAfterHardBreak) {
                            // Second Enter: create new paragraph
                            // Delete the hard break and split the paragraph
                            const tr = state.tr
                            tr.delete($from.pos - 1, $from.pos)

                            // Split the block
                            const splitPos = tr.selection.$from.pos
                            tr.split(splitPos)

                            view.dispatch(tr)
                            return true
                        } else {
                            // First Enter: insert hard break
                            const {tr} = state

                            // If there's a selection, delete it first
                            if (!state.selection.empty) {
                                tr.deleteSelection()
                            }

                            // Insert hard break
                            const hardBreakType = state.schema.nodes.hardBreak
                            if (!hardBreakType) {
                                return false
                            }

                            const hardBreak = hardBreakType.create()
                            tr.replaceSelectionWith(hardBreak)

                            view.dispatch(tr)
                            return true
                        }
                    },
                },
            }),
        ]
    },
})

