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

import {Node} from '@tiptap/core'

export const Math = Node.create({
    name: 'math',
    group: 'block',
    content: 'text*',
    marks: '',
    atom: true,

    addAttributes() {
        return {
            content: {
                default: ''
            }
        }
    },

    parseHTML() {
        return [
            {
                tag: 'div[data-type="math"]'
            }
        ]
    },

    renderHTML({node, HTMLAttributes}) {
        return [
            'div',
            {'data-type': 'math', class: 'math-block'},
            ['pre', {}, node.attrs.content || '']
        ]
    }
})

