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

import {mergeAttributes, Node, wrappingInputRule} from '@tiptap/core'
import {VueNodeViewRenderer} from '@tiptap/vue-3'
import TaskItemComponent from '../components/TaskItemComponent.vue'

/**
 * Custom TaskList extension for TipTap editor.
 * Renders as a BulletList with task items that have checkboxes.
 *
 * @author RollW
 */
export const TaskList = Node.create({
    name: 'taskList',

    group: 'block list',

    content: 'taskItem+',

    parseHTML() {
        return [
            {
                tag: 'ul[data-type="taskList"]',
                priority: 51,
            },
        ]
    },

    renderHTML({HTMLAttributes}) {
        return ['ul', mergeAttributes(HTMLAttributes, {'data-type': 'taskList'}), 0]
    },

    addCommands() {
        return {
            toggleTaskList: () => ({commands}) => {
                return commands.toggleList(this.name, 'taskItem')
            },
        }
    },

    addKeyboardShortcuts() {
        return {
            'Mod-Shift-9': () => this.editor.commands.toggleTaskList(),
        }
    },
})

/**
 * Custom TaskItem extension for TipTap editor.
 * Represents an item in a task list with checkbox state.
 *
 * @author RollW
 */
export const TaskItem = Node.create({
    name: 'taskItem',

    content: 'paragraph block*',

    defining: true,

    addAttributes() {
        return {
            checked: {
                default: false,
                keepOnSplit: false,
                parseHTML: element => {
                    return element.getAttribute('data-checked') === 'true'
                },
                renderHTML: attributes => {
                    return {
                        'data-checked': attributes.checked,
                    }
                },
            },
        }
    },

    parseHTML() {
        return [
            {
                tag: 'li[data-type="taskItem"]',
                priority: 51,
            },
        ]
    },

    renderHTML({HTMLAttributes}) {
        return [
            'li',
            mergeAttributes(HTMLAttributes, {'data-type': 'taskItem'}),
            0,
        ]
    },

    addNodeView() {
        return VueNodeViewRenderer(TaskItemComponent as any)
    },

    addKeyboardShortcuts() {
        return {
            Enter: () => this.editor.commands.splitListItem(this.name),
            Tab: () => this.editor.commands.sinkListItem(this.name),
            'Shift-Tab': () => this.editor.commands.liftListItem(this.name),
            Backspace: () => {
                const {$anchor} = this.editor.state.selection

                // Check if cursor is at the start of the list item
                if ($anchor.parentOffset === 0) {
                    const {parent} = $anchor

                    // Check if parent is a paragraph (first child of list item)
                    if (parent.type.name === 'paragraph') {
                        const grandParent = $anchor.node(-1)

                        // Check if grandparent is a taskItem
                        if (grandParent && grandParent.type.name === 'taskItem') {
                            // Try to lift first (reduce indent)
                            if (this.editor.commands.liftListItem(this.name)) {
                                return true
                            }
                            // If can't lift, convert to paragraph (cancel list)
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
                // eslint-disable-next-line no-useless-escape
                find: /^\s*-\s\[([ x])\]\s$/,
                type: this.type,
                getAttributes: match => {
                    return {checked: match[1] === 'x'}
                },
            }),
        ]
    },
})

