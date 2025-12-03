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

import {mergeAttributes, Node, wrappingInputRule} from "@tiptap/core"
import {VueNodeViewRenderer} from "@tiptap/vue-3"
import TaskItemComponent from "../components/TaskItemComponent.vue"

export const TaskList = Node.create({
    name: "taskList",
    group: "block list",
    content: "taskItem+",

    parseHTML() {
        return [
            {
                tag: "ul[data-type='taskList']",
                priority: 51,
            },
        ]
    },

    renderHTML({HTMLAttributes}) {
        return ["ul", mergeAttributes(HTMLAttributes, {"data-type": "taskList"}), 0]
    },

    addCommands() {
        return {
            toggleTaskList: () => ({commands}) => {
                return commands.toggleList(this.name, "taskItem")
            },
        }
    },

    addKeyboardShortcuts() {
        return {
            "Mod-Shift-9": () => this.editor.commands.toggleTaskList(),
        }
    },
})

const inputRegex = /^\s*(\[([( |x])?\])\s$/

export const TaskItem = Node.create({
    name: "taskItem",
    content: "paragraph block*",
    defining: true,

    addAttributes() {
        return {
            checked: {
                default: false,
                keepOnSplit: false,
                parseHTML: element => {
                    const dataChecked = element.getAttribute("data-checked")
                    return dataChecked === "" || dataChecked === "true"
                },
                renderHTML: attributes => ({
                    "data-checked": attributes.checked,
                }),
            },
        }
    },

    parseHTML() {
        return [
            {
                tag: "li[data-type='taskItem']",
                priority: 51,
            },
        ]
    },

    renderHTML({HTMLAttributes}) {
        return [
            "li",
            mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
                "data-type": "taskItem",
            }),
            ["div", 0],
        ]
    },

    addNodeView() {
        return VueNodeViewRenderer(TaskItemComponent)
    },

    addKeyboardShortcuts() {
        return {
            Enter: () => {
                return this.editor.commands.splitListItem(this.name)
            },
            Tab: () => this.editor.commands.sinkListItem(this.name),
            "Shift-Tab": () => this.editor.commands.liftListItem(this.name),
            Backspace: () => {
                const {$anchor} = this.editor.state.selection

                if ($anchor.parentOffset === 0) {
                    const {parent} = $anchor
                    if (parent.type.name === "paragraph") {
                        const grandParent = $anchor.node(-1)
                        if (grandParent && grandParent.type.name === "taskItem") {
                            // Try to lift first (reduce indent)
                            if (this.editor.commands.liftListItem(this.name)) {
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

    addInputRules() {
        return [
            wrappingInputRule({
                find: inputRegex,
                type: this.type,
                getAttributes: match => {
                    return {checked: match[1] === "x"}
                },
            }),
        ]
    },
})
