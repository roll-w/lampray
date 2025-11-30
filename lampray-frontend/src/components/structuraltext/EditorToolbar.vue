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
import type {Editor} from '@tiptap/core'
import {computed} from 'vue'

/**
 * Toolbar component for structural text editor.
 *
 * @author RollW
 */

interface Props {
    editor: Editor
}

const props = defineProps<Props>()

const toggleBold = () => {
    props.editor.chain().focus().toggleBold().run()
}
const toggleItalic = () => {
    props.editor.chain().focus().toggleItalic().run()
}
const toggleStrike = () => {
    props.editor.chain().focus().toggleStrike().run()
}
const toggleUnderline = () => {
    props.editor.chain().focus().toggleUnderline().run()
}
const toggleHighlight = () => {
    props.editor.chain().focus().toggleMark('highlight').run()
}
const toggleCode = () => {
    props.editor.chain().focus().toggleCode().run()
}

const setHeading = (level: 1 | 2 | 3 | 4 | 5 | 6) => {
    props.editor.chain().focus().toggleHeading({level}).run()
}

const toggleBulletList = () => {
    props.editor.chain().focus().toggleBulletList().run()
}
const toggleOrderedList = () => {
    props.editor.chain().focus().toggleOrderedList().run()
}
const toggleTaskList = () => {
    props.editor.chain().focus().toggleTaskList().run()
}
const toggleBlockquote = () => {
    props.editor.chain().focus().toggleBlockquote().run()
}
const toggleCodeBlock = () => {
    const {state} = props.editor
    if (props.editor.isActive('codeBlock')) {
        return
    }
    const {from, to} = state.selection
    if (from !== to) {
        props.editor.chain().focus().setCodeBlock().run()
    } else {
        props.editor.chain()
                .insertContent({
                    type: 'codeBlock',
                })
                .focus()
                .run();
    }
}

const insertTable = () => {
    props.editor.chain().focus().insertTable({rows: 3, cols: 3, withHeaderRow: true}).run()
}

const insertHorizontalRule = () => {
    props.editor.chain().focus().setHorizontalRule().run()
}

const addImage = () => {
    const url = window.prompt('Image URL:')
    if (url) {
        props.editor.chain().focus().setImage({src: url}).run()
    }
}

const addLink = () => {
    const url = window.prompt('Link URL:')
    if (url) {
        props.editor.chain().focus().setLink({href: url}).run()
    }
}

const isBold = computed(() => props.editor.isActive('bold'))
const isItalic = computed(() => props.editor.isActive('italic'))
const isStrike = computed(() => props.editor.isActive('strike'))
const isUnderline = computed(() => props.editor.isActive('underline'))
const isHighlight = computed(() => props.editor.isActive('highlight'))
const isCode = computed(() => props.editor.isActive('code'))
const isBulletList = computed(() => props.editor.isActive('bulletList'))
const isOrderedList = computed(() => props.editor.isActive('orderedList'))
const isTaskList = computed(() => props.editor.isActive('taskList'))
const isBlockquote = computed(() => props.editor.isActive('blockquote'))
const isCodeBlock = computed(() => props.editor.isActive('codeBlock'))
</script>

<template>
    <div class="flex flex-wrap gap-1 p-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
        <!-- Text formatting -->
        <div class="flex gap-1">
            <UButton
                    :variant="isBold ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-bold"
                    @click="toggleBold"
                    title="Bold"
            />
            <UButton
                    :variant="isItalic ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-italic"
                    @click="toggleItalic"
                    title="Italic"
            />
            <UButton
                    :variant="isUnderline ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-underline"
                    @click="toggleUnderline"
                    title="Underline"
            />
            <UButton
                    :variant="isStrike ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-strikethrough"
                    @click="toggleStrike"
                    title="Strikethrough"
            />
            <UButton
                    :variant="isHighlight ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-highlighter"
                    @click="toggleHighlight"
                    title="Highlight"
            />
            <UButton
                    :variant="isCode ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-code"
                    @click="toggleCode"
                    title="Inline Code"
            />
        </div>

        <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>

        <!-- Headings -->
        <div class="flex gap-1">
            <UButton
                    :variant="editor.isActive('heading', { level: 1 }) ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    @click="setHeading(1)"
                    title="Heading 1"
            >
                H1
            </UButton>
            <UButton
                    :variant="editor.isActive('heading', { level: 2 }) ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    @click="setHeading(2)"
                    title="Heading 2"
            >
                H2
            </UButton>
            <UButton
                    :variant="editor.isActive('heading', { level: 3 }) ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    @click="setHeading(3)"
                    title="Heading 3"
            >
                H3
            </UButton>
        </div>

        <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>

        <!-- Lists and blocks -->
        <div class="flex gap-1">
            <UButton
                    :variant="isBulletList ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-list"
                    @click="toggleBulletList"
                    title="Bullet List"
            />
            <UButton
                    :variant="isOrderedList ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-list-ordered"
                    @click="toggleOrderedList"
                    title="Ordered List"
            />
            <UButton
                    :variant="isTaskList ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-list-checks"
                    @click="toggleTaskList"
                    title="Task List"
            />
            <UButton
                    :variant="isBlockquote ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-quote"
                    @click="toggleBlockquote"
                    title="Blockquote"
            />
            <UButton
                    :variant="isCodeBlock ? 'solid' : 'ghost'"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-code-2"
                    @click="toggleCodeBlock"
                    title="Code Block"
            />
        </div>

        <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>

        <!-- Insert elements -->
        <div class="flex gap-1">
            <UButton
                    variant="ghost"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-link"
                    @click="addLink"
                    title="Insert Link"
            />
            <UButton
                    variant="ghost"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-image"
                    @click="addImage"
                    title="Insert Image"
            />
            <UButton
                    variant="ghost"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-table"
                    @click="insertTable"
                    title="Insert Table"
            />
            <UButton
                    variant="ghost"
                    color="neutral"
                    size="sm"
                    icon="i-lucide-minus"
                    @click="insertHorizontalRule"
                    title="Horizontal Rule"
            />
        </div>
    </div>
</template>

