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
        // Extracted keydown handler to a named constant to improve readability and avoid
        // linter warnings about unused inline properties.
        const onKeyDown = (view: any, event: KeyboardEvent): boolean => {
            // Only handle plain Enter (no modifiers)
            if ((event as KeyboardEvent).key !== "Enter" || (event as KeyboardEvent).shiftKey || (event as KeyboardEvent).ctrlKey || (event as KeyboardEvent).metaKey) {
                return false
            }

            const {state} = view
            const {$from} = state.selection

            const isInside = (pos: any, names: string[]) => {
                for (let i = pos.depth; i > 0; i--) {
                    const node = pos.node(i)
                    if (names.includes(node.type.name)) return true
                }
                return false
            }

            // Let list or task extensions handle Enter
            if (isInside($from, ["listItem", "taskItem"])) {
                return false
            }

            // Ignore code blocks and non-paragraph parents
            if ($from.parent.type.name === "codeBlock") {
                return false
            }
            if ($from.parent.type.name !== "paragraph") {
                return false
            }

            // Determine whether the cursor is immediately after a hard break
            const lastNode = $from.nodeBefore
            const isAfterHardBreak = lastNode?.type?.name === "hardBreak"

            const tr = state.tr

            if (isAfterHardBreak) {
                // Second Enter: remove the hardBreak and split into a new paragraph
                // hardBreak is at position $from.pos - 1
                const breakStart = $from.pos - 1
                tr.delete(breakStart, $from.pos)

                // Split at the position where the hard break was deleted
                tr.split(breakStart)

                view.dispatch(tr)
                return true
            }

            // First Enter: insert a hard break within the paragraph
            if (!state.selection.empty) {
                tr.deleteSelection()
            }

            const hardBreakType = state.schema.nodes.hardBreak
            if (!hardBreakType) return false

            tr.replaceSelectionWith(hardBreakType.create())
            view.dispatch(tr)
            return true
        }

        return [
            new Plugin({
                key: new PluginKey("lineBreakEnter"),
                props: {
                    handleKeyDown: onKeyDown,
                },
            }),
        ]
    },
})
