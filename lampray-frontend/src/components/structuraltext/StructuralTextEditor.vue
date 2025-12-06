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
import {EditorContent, useEditor} from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import Highlight from "@tiptap/extension-highlight"
import {CodeBlock} from "@/components/structuraltext/extensions/CodeBlock";
import {TaskItem, TaskList} from "@/components/structuraltext/extensions/TaskList";
import {ListKeyboardShortcuts} from "@/components/structuraltext/extensions/ListKeyboardShortcuts";
import EditorToolbar from "@/components/structuraltext/EditorToolbar.vue";
import BubbleMenu from "@/components/structuraltext/EditorBubbleMenu.vue";
import EditorContextMenu from "@/components/structuraltext/EditorContextMenu.vue";
import type {StructuralText} from "@/components/structuraltext/types";
import {convertFromStructuralText, convertToStructuralText} from "@/components/structuraltext/converter";
import {onBeforeUnmount, watch} from "vue";
import {DefaultKeyboardShortcuts} from "@/components/structuraltext/extensions/DefaultKeyboardShortcuts.ts";
import {Table, TableCell, TableHeader, TableRow} from "@/components/structuraltext/extensions/TableExtension.ts";
import {HeadingWithId} from "@/components/structuraltext/extensions/HeadingWithId.ts";

interface Props {
    modelValue?: StructuralText
    editable?: boolean
    placeholder?: string
    showToolbar?: boolean
}

interface Emits {
    (e: "update:modelValue", value: StructuralText): void
    (e: "change", value: StructuralText): void
}

const props = withDefaults(defineProps<Props>(), {
    editable: true,
    placeholder: "",
    showToolbar: true
})

const emit = defineEmits<Emits>()

const editor = useEditor({
    extensions: [
        DefaultKeyboardShortcuts,
        //TableCellTrailingBreakPlugin,
        StarterKit.configure({
            heading: false, // Disable default heading, use HeadingWithId instead
            codeBlock: false,
            link: {
                openOnClick: false,
                HTMLAttributes: {
                    class: "text-blue-600 dark:text-blue-400 underline cursor-pointer " +
                            "hover:underline underline-offset-2 decoration-2 decoration-blue-500/50 " +
                            "hover:decoration-blue-500 transition-all transition-duration-500 ease-in-out"
                }
            },
            bulletList: {
                keepMarks: true,
                keepAttributes: false,
            },
            orderedList: {
                keepMarks: true,
                keepAttributes: false,
            }
        }),
        HeadingWithId.configure({
            levels: [1, 2, 3, 4, 5, 6]
        }),
        CodeBlock.configure({
            enableTabIndentation: true,
        }),
        Image.configure({
            HTMLAttributes: {
                class: "max-w-full h-auto rounded-lg"
            }
        }),
        Table.configure({
            resizable: true,
            allowTableNodeSelection: true,
            // HTMLAttributes: {
            //     class: "border-collapse table-auto w-full"
            // }
        }),
        TableRow,
        TableCell.configure({}),
        TableHeader,
        Highlight.configure({
            HTMLAttributes: {
                class: "bg-yellow-200 dark:bg-yellow-800 px-1 rounded"
            }
        }),
        TaskList,
        TaskItem,
        ListKeyboardShortcuts
    ],
    editable: props.editable,
    content: props.modelValue ? convertFromStructuralText(props.modelValue) : "",
    onUpdate: ({editor}) => {
        const structuralText = convertToStructuralText(editor)
        emit("update:modelValue", structuralText)
        emit("change", structuralText)
    },
    editorProps: {
        attributes: {
            class: "prose dark:prose-invert max-w-none focus:outline-none min-h-[300px] p-4"
        },
        handleDOMEvents: {
            // Prevent drag and drop within table cells to avoid accidental row/column creation
            drop: (view, event) => {
                const pos = view.posAtCoords({left: event.clientX, top: event.clientY})
                if (!pos) return false

                const $pos = view.state.doc.resolve(pos.pos)
                const inTable = $pos.node(-1)?.type.name === "tableCell" ||
                        $pos.node(-1)?.type.name === "tableHeader" ||
                        $pos.node(-2)?.type.name === "table"

                // If dropping inside table, only allow text content move
                if (inTable && event.dataTransfer?.effectAllowed === "move") {
                    return false // Let TipTap handle the text move
                }

                return false
            }
        }
    }
})

watch(() => props.editable, (newVal) => {
    if (editor.value) {
        editor.value.setEditable(newVal)
    }
})

watch(() => props.modelValue, (newVal) => {
    if (editor.value && newVal) {
        const currentContent = convertToStructuralText(editor.value)
        if (JSON.stringify(currentContent) !== JSON.stringify(newVal)) {
            editor.value.commands.setContent(convertFromStructuralText(newVal))
        }
    }
})

onBeforeUnmount(() => {
    editor.value?.destroy()
})
</script>

<template>
    <div class="editor border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden bg-white dark:bg-gray-950">
        <EditorToolbar v-if="showToolbar && editor" :editor="editor"/>
        <BubbleMenu v-if="editor" :editor="editor" :editable="editable"/>
        <EditorContextMenu v-if="editor" :editor="editor" :editable="editable">
            <EditorContent :editor="editor"/>
        </EditorContextMenu>
    </div>
</template>

<style>
@reference "@/assets/main.css"

.editor .ProseMirror {
    @apply min-h-[300px];
}

.editor .ProseMirror:focus {
    @apply outline-none;
}

.editor h1 {
    @apply text-4xl font-bold mt-6 mb-4;
}

.editor h2 {
    @apply text-3xl font-bold mt-5 mb-3;
}

.editor h3 {
    @apply text-2xl font-bold mt-4 mb-3;
}

.editor h4 {
    @apply text-xl font-bold mt-3 mb-2;
}

.editor h5 {
    @apply text-lg font-bold mt-3 mb-2;
}

.editor h6 {
    @apply text-base font-bold mt-2 mb-2;
}

.editor p {
    @apply mb-4;
}

.editor ul,
.editor ol {
    @apply pl-6 mb-2;
}

.editor ul {
    @apply list-disc;
}

.editor ol {
    @apply list-decimal;
}

.editor li {
    @apply mb-1;
}

.editor ul[data-type="taskList"] {
    @apply list-none pl-0;
}

.editor blockquote {
    @apply border-l-4 border-gray-300 dark:border-gray-600 pl-4 text-gray-600 dark:text-gray-400 my-4;
}

.editor code {
    @apply bg-gray-100 dark:bg-gray-800 rounded px-1 py-0.5 text-sm font-mono;
}

.editor pre code {
    @apply bg-transparent p-0;
}

.editor strong {
    @apply font-bold;
}

.editor em {
    @apply italic;
}

.editor u {
    @apply underline;
}

.editor s {
    @apply line-through;
}

.editor mark {
    @apply bg-yellow-200 dark:bg-yellow-800 px-1 rounded;
}

.editor hr {
    @apply border-t border-gray-300 dark:border-gray-600 my-6;
}

.editor img {
    @apply max-w-full h-auto rounded-lg my-4;
}

.editor table {
    @apply border-collapse w-full mb-4 relative;
}

.editor th,
.editor td {
    @apply border border-gray-300 dark:border-gray-600 px-4 py-2 relative;
}

.editor th {
    @apply bg-gray-100 dark:bg-gray-800 font-bold;
}

.editor .selectedCell {
    @apply bg-blue-100 dark:bg-blue-900;
}

.editor .ProseMirror-selectednode {
    @apply outline outline-2 outline-blue-500 outline-offset-[-2px];
}

.editor table.ProseMirror-selectednode {
    @apply outline outline-2 outline-blue-500;
}

.column-resize-handle {
    @apply bg-blue-500 transition-colors;
    bottom: -2px;
    pointer-events: none;
    position: absolute;
    right: -2px;
    top: 0;
    width: 4px;
}

.editor .resize-cursor {
    cursor: col-resize;
}

/* Math block styles */
.editor .math-block pre {
    @apply bg-transparent p-0 m-0;
}
</style>

