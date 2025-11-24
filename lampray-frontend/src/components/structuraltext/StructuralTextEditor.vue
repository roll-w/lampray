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
import {EditorContent, useEditor} from "@tiptap/vue-3"
import StarterKit from "@tiptap/starter-kit"
import Image from "@tiptap/extension-image"
import {Table, TableCell, TableHeader, TableRow} from "@tiptap/extension-table"
import Mention from "@tiptap/extension-mention"
import {Math} from "./extensions/Math"
import EditorToolbar from "./EditorToolbar.vue"
import type {StructuralText} from "./types"
import {convertFromStructuralText, convertToStructuralText} from "./converter"
import {onBeforeUnmount, watch} from "vue";
import Highlight from "@tiptap/extension-highlight";

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
    placeholder: "Start writing...",
    showToolbar: true
})

const emit = defineEmits<Emits>()

const editor = useEditor({
    extensions: [
        StarterKit.configure({
            heading: {
                levels: [1, 2, 3, 4, 5, 6]
            },
            codeBlock: {
                HTMLAttributes: {
                    class: "code-block"
                }
            },
            link: {
                openOnClick: false,
                HTMLAttributes: {
                    class: "text-blue-600 dark:text-blue-400 underline cursor-pointer hover:text-blue-700"
                }
            },
        }),
        Image.configure({
            HTMLAttributes: {
                class: "max-w-full h-auto rounded-lg"
            }
        }),
        Table.configure({
            resizable: true,
            HTMLAttributes: {
                class: "border-collapse table-auto w-full"
            }
        }),
        TableRow,
        TableCell.configure({
            HTMLAttributes: {
                class: "border border-gray-300 dark:border-gray-600 px-4 py-2"
            }
        }),
        TableHeader.configure({
            HTMLAttributes: {
                class: "border border-gray-300 dark:border-gray-600 px-4 py-2 bg-gray-100 dark:bg-gray-800 font-bold"
            }
        }),
        Highlight.configure({
            HTMLAttributes: {
                class: "bg-yellow-200 dark:bg-yellow-800 px-1 rounded"
            }
        }),
        Math,
        Mention.configure({
            HTMLAttributes: {
                class: "mention text-blue-600 dark:text-blue-400"
            }
        }),
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
    <div class="structural-text-editor border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden bg-white dark:bg-gray-950">
        <EditorToolbar v-if="showToolbar && editor" :editor="editor"/>
        <EditorContent :editor="editor"/>
    </div>
</template>

<style>
@reference "../../assets/main.css"

.structural-text-editor .ProseMirror {
    @apply min-h-[300px];
}

.structural-text-editor .ProseMirror:focus {
    @apply outline-none;
}

/* Heading styles */
.structural-text-editor h1 {
    @apply text-4xl font-bold mt-6 mb-4;
}

.structural-text-editor h2 {
    @apply text-3xl font-bold mt-5 mb-3;
}

.structural-text-editor h3 {
    @apply text-2xl font-bold mt-4 mb-3;
}

.structural-text-editor h4 {
    @apply text-xl font-bold mt-3 mb-2;
}

.structural-text-editor h5 {
    @apply text-lg font-bold mt-3 mb-2;
}

.structural-text-editor h6 {
    @apply text-base font-bold mt-2 mb-2;
}

/* Paragraph */
.structural-text-editor p {
    @apply mb-4;
}

/* Lists */
.structural-text-editor ul,
.structural-text-editor ol {
    @apply pl-6 mb-4;
}

.structural-text-editor ul {
    @apply list-disc;
}

.structural-text-editor ol {
    @apply list-decimal;
}

.structural-text-editor li {
    @apply mb-1;
}

/* Blockquote */
.structural-text-editor blockquote {
    @apply border-l-4 border-gray-300 dark:border-gray-600 pl-4 text-gray-600 dark:text-gray-400 my-4;
}

/* Code block */
.structural-text-editor .code-block,
.structural-text-editor pre {
    @apply bg-gray-100 dark:bg-gray-800 rounded-lg p-4 mb-4 overflow-x-auto;
}

.structural-text-editor code {
    @apply bg-gray-100 dark:bg-gray-800 rounded px-2 py-1 text-sm font-mono;
}

.structural-text-editor pre code {
    @apply bg-transparent p-0;
}

/* Inline formatting */
.structural-text-editor strong {
    @apply font-bold;
}

.structural-text-editor em {
    @apply italic;
}

.structural-text-editor u {
    @apply underline;
}

.structural-text-editor s {
    @apply line-through;
}

.structural-text-editor mark {
    @apply bg-yellow-200 dark:bg-yellow-800 px-1 rounded;
}

/* Horizontal rule */
.structural-text-editor hr {
    @apply border-t border-gray-300 dark:border-gray-600 my-6;
}

/* Image */
.structural-text-editor img {
    @apply max-w-full h-auto rounded-lg my-4;
}

/* Table */
.structural-text-editor table {
    @apply border-collapse w-full mb-4;
}

.structural-text-editor th,
.structural-text-editor td {
    @apply border border-gray-300 dark:border-gray-600 px-4 py-2;
}

.structural-text-editor th {
    @apply bg-gray-100 dark:bg-gray-800 font-bold;
}

/* Link */
.structural-text-editor a {
    @apply text-blue-600 dark:text-blue-400 underline cursor-pointer;
}

.structural-text-editor a:hover {
    @apply text-blue-700 dark:text-blue-300;
}

/* Mention */
.structural-text-editor .mention {
    @apply text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950 px-1 rounded;
}

/* Math block */
.structural-text-editor .math-block {
    @apply bg-gray-100 dark:bg-gray-800 rounded-lg p-4 mb-4 overflow-x-auto;
}

.structural-text-editor .math-block pre {
    @apply bg-transparent p-0 m-0;
}

/* Placeholder */
.structural-text-editor .ProseMirror p.is-editor-empty:first-child::before {
    content: attr(data-placeholder);
    @apply text-gray-400 dark:text-gray-500 float-left h-0 pointer-events-none;
}
</style>

