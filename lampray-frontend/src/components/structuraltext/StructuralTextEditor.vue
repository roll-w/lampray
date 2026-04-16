<!--
  - Copyright (C) 2023-2026 RollW
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

<script lang="ts" setup>
import {EditorContent, useEditor} from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import Highlight from "@tiptap/extension-highlight";
import {CodeBlock} from "@/components/structuraltext/extensions/CodeBlock";
import {TaskItem, TaskList} from "@/components/structuraltext/extensions/TaskList";
import {ListKeyboardShortcuts} from "@/components/structuraltext/extensions/ListKeyboardShortcuts";
import EditorToolbar from "@/components/structuraltext/EditorToolbar.vue";
import EditorBubbleMenu from "@/components/structuraltext/EditorBubbleMenu.vue";
import EditorContextMenu from "@/components/structuraltext/EditorContextMenu.vue";
import TableEdgeAffordances from "@/components/structuraltext/TableEdgeAffordances.vue";
import LinkModal from "@/components/structuraltext/modals/LinkModal.vue";
import ImageModal from "@/components/structuraltext/modals/ImageModal.vue";
import TableInsertModal from "@/components/structuraltext/modals/TableInsertModal.vue";
import type {ContentLocationRange, StructuralText} from "@/components/structuraltext/types";
import {convertFromStructuralText, convertToStructuralText} from "@/components/structuraltext/converter";
import {computed, getCurrentInstance, onBeforeUnmount, ref, watch} from "vue";
import {DefaultKeyboardShortcuts} from "@/components/structuraltext/extensions/DefaultKeyboardShortcuts.ts";
import {createSlashCommandExtension} from "@/components/structuraltext/extensions/SlashCommand";
import {Table, TableCell, TableHeader, TableRow} from "@/components/structuraltext/extensions/TableExtension.ts";
import {HeadingWithId} from "@/components/structuraltext/extensions/HeadingWithId.ts";
import {Plugin, PluginKey} from "@tiptap/pm/state";
import {CellSelection} from "@tiptap/pm/tables";
import StructuralTextOutline from "@/components/structuraltext/StructuralTextOutline.vue";
import type {Editor} from "@tiptap/core";
import {Extension} from "@tiptap/core";
import {Decoration, DecorationSet} from "@tiptap/pm/view";
import {useI18n} from "vue-i18n";
import {
    applyLink,
    insertLinkedText,
    isBareHttpUrl,
    isRemoteImageUrl,
    isSelectionInEmptyParagraph,
} from "@/components/structuraltext/composables/useEditorActions";
import {
    createStructuralTextInsertController,
    provideStructuralTextInsertController,
    type StructuralTextInsertController,
} from "@/components/structuraltext/composables/useStructuralTextInsertController";

interface Props {
    modelValue?: StructuralText
    editable: boolean
    placeholder?: string
    showToolbar?: boolean
    showOutline?: boolean
    outlineTitle?: string
    outlineColor?: "primary" | "secondary" | "success" | "info" | "warning" | "error" | "neutral"
    variant?: "ghost" | "outline"
    ui?: {
        content?: {
            root?: string
        },
        toolbar?: {
            root?: string
            // When true, toolbar menu items will be centered horizontally
            centered?: boolean
        },
        outline?: {
            root?: string
        }
    }
    extensions?: any[]
    highlights?: {
        location: ContentLocationRange;
        info?: string;
        severity?: "critical" | "major" | "minor" | "info" | string;
    }[]
}

const props = withDefaults(defineProps<Props>(), {
    editable: true,
    placeholder: "",
    showToolbar: true,
    showOutline: false,
    outlineTitle: "Outline",
})

const {t} = useI18n()
const appContext = getCurrentInstance()?.appContext || null
const editorSurface = ref<HTMLElement | null>(null)

let insertController: StructuralTextInsertController | null = null

function insertImageFromUrl(editor: Editor, url: string) {
    return editor.chain().focus().setImage({src: url}).run()
}

function handleBareUrlPaste(editor: Editor, pastedText: string) {
    if (!isBareHttpUrl(pastedText)) {
        return false
    }

    if (!editor.state.selection.empty) {
        return applyLink(editor, pastedText)
    }

    if (!isSelectionInEmptyParagraph(editor)) {
        return false
    }

    if (isRemoteImageUrl(pastedText)) {
        return insertImageFromUrl(editor, pastedText)
    }

    return insertLinkedText(editor, pastedText, pastedText)
}

function buildSlashCommandItems() {
    return [
        {
            id: "paragraph",
            label: t("editor.toolbar.paragraph"),
            description: t("editor.slash.description.paragraph"),
            icon: "i-lucide-pilcrow",
            keywords: ["paragraph", "text", "normal"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).setParagraph().run()
            },
        },
        {
            id: "heading-1",
            label: t("editor.toolbar.heading1"),
            description: t("editor.slash.description.heading1"),
            icon: "i-lucide-heading-1",
            keywords: ["heading", "title", "h1"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).setHeading({level: 1}).run()
            },
        },
        {
            id: "heading-2",
            label: t("editor.toolbar.heading2"),
            description: t("editor.slash.description.heading2"),
            icon: "i-lucide-heading-2",
            keywords: ["heading", "section", "h2"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).setHeading({level: 2}).run()
            },
        },
        {
            id: "heading-3",
            label: t("editor.toolbar.heading3"),
            description: t("editor.slash.description.heading3"),
            icon: "i-lucide-heading-3",
            keywords: ["heading", "subsection", "h3"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).setHeading({level: 3}).run()
            },
        },
        {
            id: "bullet-list",
            label: t("editor.toolbar.bulletList"),
            description: t("editor.slash.description.bulletList"),
            icon: "i-lucide-list",
            keywords: ["bullet", "list", "unordered"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).toggleBulletList().run()
            },
        },
        {
            id: "ordered-list",
            label: t("editor.toolbar.orderedList"),
            description: t("editor.slash.description.orderedList"),
            icon: "i-lucide-list-ordered",
            keywords: ["numbered", "ordered", "list"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).toggleOrderedList().run()
            },
        },
        {
            id: "task-list",
            label: t("editor.toolbar.taskList"),
            description: t("editor.slash.description.taskList"),
            icon: "i-lucide-list-checks",
            keywords: ["task", "todo", "checklist"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).toggleTaskList().run()
            },
        },
        {
            id: "blockquote",
            label: t("editor.toolbar.blockquote"),
            description: t("editor.slash.description.blockquote"),
            icon: "i-lucide-quote",
            keywords: ["quote", "blockquote", "callout"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).toggleBlockquote().run()
            },
        },
        {
            id: "code-block",
            label: t("editor.toolbar.codeBlock"),
            description: t("editor.slash.description.codeBlock"),
            icon: "i-lucide-code-2",
            keywords: ["code", "snippet", "pre"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).setCodeBlock().run()
            },
        },
        {
            id: "divider",
            label: t("editor.toolbar.horizontalRule"),
            description: t("editor.slash.description.divider"),
            icon: "i-lucide-minus",
            keywords: ["divider", "separator", "rule"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).setHorizontalRule().run()
            },
        },
        {
            id: "table",
            label: t("editor.toolbar.insertTable"),
            description: t("editor.slash.description.table"),
            icon: "i-lucide-table",
            keywords: ["table", "grid", "columns", "rows"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).run()
                insertController?.openTableInsertModal()
            },
        },
        {
            id: "link",
            label: t("editor.toolbar.insertLink"),
            description: t("editor.slash.description.link"),
            icon: "i-lucide-link",
            keywords: ["link", "url", "hyperlink"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).run()
                insertController?.openLinkModalFromSelection()
            },
        },
        {
            id: "image",
            label: t("editor.toolbar.insertImage"),
            description: t("editor.slash.description.image"),
            icon: "i-lucide-image",
            keywords: ["image", "photo", "picture"],
            onSelect: (editor: Editor, range: { from: number; to: number }) => {
                editor.chain().focus().deleteRange(range).run()
                insertController?.openImageModal()
            },
        },
    ]
}

const emit = defineEmits<Emits>()

interface Emits {
    (e: "update:modelValue", value: StructuralText): void

    (e: "change", value: StructuralText): void

    (e: "select-range", editor: Editor): void
}

/**
 * Plugin to preserve table cell selection on context menu.
 * Prevents losing selection when right-clicking on selected cells.
 */
const preserveSelectionPlugin = new Plugin({
    key: new PluginKey("preserveSelection"),
    props: {
        handleDOMEvents: {
            contextmenu: (view, event) => {
                // Check if we have a cell selection
                const {selection} = view.state
                if (selection instanceof CellSelection) {
                    // Prevent default editor behavior that might clear selection
                    event.preventDefault()
                    return true
                }
                return false
            }
        }
    }
})

// --- Location Mapping & Highlighting logic ---
type SegmentMapping = {
    path: string;
    text: string;
    start: number;
    end: number;
}

type StructuralSegment = {
    path: string;
    text: string;
}

const mappings = ref<SegmentMapping[]>([]);

const generateTextSegments = (node: StructuralText, currentPath: string, segments: StructuralSegment[]) => {
    if (node.content && node.content.length > 0) {
        segments.push({path: `${currentPath}.content`, text: node.content});
    }
    if (node.children && node.children.length > 0) {
        node.children.forEach((child, index) => {
            generateTextSegments(child as StructuralText, `${currentPath}.children[${index}]`, segments);
        });
    }
};

const structuralSegments = computed(() => {
    const segments: StructuralSegment[] = [];
    if (props.modelValue) {
        generateTextSegments(props.modelValue, "$", segments);
    }
    return segments;
});

const refreshMappings = () => {
    const editorVal = editor.value;
    if (!editorVal || !structuralSegments.value.length) return;

    const doc = editorVal.state.doc;
    const newMappings: SegmentMapping[] = [];
    let currentSegmentIndex = 0;
    let currentSegmentOffset = 0;

    doc.descendants((node, pos) => {
        if (!node.isText) return;

        const nodeText = node.text || "";
        let nodeOffset = 0;

        while (nodeOffset < nodeText.length && currentSegmentIndex < structuralSegments.value.length) {
            const segment = structuralSegments.value[currentSegmentIndex]!;
            const segmentRemaining = segment.text.length - currentSegmentOffset;
            const nodeRemaining = nodeText.length - nodeOffset;

            const consumeLength = Math.min(segmentRemaining, nodeRemaining);

            if (currentSegmentOffset === 0) {
                newMappings.push({
                    path: segment.path,
                    text: segment.text,
                    start: pos + nodeOffset,
                    end: 0
                });
            }

            currentSegmentOffset += consumeLength;
            nodeOffset += consumeLength;

            if (currentSegmentOffset >= segment.text.length) {
                const lastMap = newMappings[newMappings.length - 1];
                if (lastMap) {
                    lastMap.end = pos + nodeOffset;
                }
                currentSegmentIndex++;
                currentSegmentOffset = 0;
            }
        }
    });

    mappings.value = newMappings;
};

const resolveLocationToRange = (loc: ContentLocationRange) => {
    if (!loc.startPath) return null;
    const startMap = mappings.value.find(m => m.path === loc.startPath);
    if (!startMap) return null;

    let from = startMap.start + loc.startInNode;
    let to = from;

    if (loc.endPath) {
        const endMap = mappings.value.find(m => m.path === loc.endPath);
        if (endMap) {
            to = endMap.start + loc.endInNode;
        }
    }

    if (from === to) to = from + 1;
    return {from, to};
};

const locationHighlightExtension = Extension.create({
    name: "locationHighlight",
    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey("locationHighlight"),
                props: {
                    decorations: (state) => {
                        if (!props.highlights || !mappings.value.length) return DecorationSet.empty;

                        const decos: Decoration[] = [];
                        props.highlights.forEach(hl => {
                            const range = resolveLocationToRange(hl.location);
                            if (!range) return;

                            decos.push(Decoration.inline(range.from, range.to, {
                                class: `structural-location-highlight highlight-severity-${hl.severity || "info"}`,
                                title: hl.info || ""
                            }));
                        });

                        return DecorationSet.create(state.doc, decos);
                    }
                }
            })
        ];
    }
});

const editor = useEditor({
    extensions: [
        DefaultKeyboardShortcuts,
        StarterKit.configure({
            heading: false,
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
        }),
        TableRow,
        TableCell,
        TableHeader,
        Highlight.configure({
            HTMLAttributes: {
                class: "bg-yellow-200 dark:bg-yellow-800 px-1 rounded"
            }
        }),
        TaskList,
        TaskItem,
        ListKeyboardShortcuts,
        createSlashCommandExtension({
            appContext,
            items: buildSlashCommandItems(),
        }),
        locationHighlightExtension,
        ...(props.extensions || [])
    ],
    editorProps: {
        attributes: {
            class: "prose dark:prose-invert max-w-none focus:outline-none min-h-[300px]"
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
            },
            paste: (_view, event) => {
                const pastedText = event.clipboardData?.getData("text/plain")?.trim() || ""
                if (!pastedText) {
                    return false
                }

                const currentEditor = editor.value
                if (!currentEditor) {
                    return false
                }

                const handled = handleBareUrlPaste(currentEditor, pastedText)
                if (!handled) {
                    return false
                }

                event.preventDefault()
                return true
            }
        }
    },
    editable: props.editable,
    content: props.modelValue ? convertFromStructuralText(props.modelValue) : "",
    onUpdate: ({editor}) => {
        const structuralText = convertToStructuralText(editor)
        emit("update:modelValue", structuralText)
        emit("change", structuralText)
    },
    onCreate: ({editor}) => {
        const state = editor.state
        const plugins = [preserveSelectionPlugin, ...state.plugins]
        editor.view.updateState(state.reconfigure({plugins}))
        refreshMappings()
    },
    onSelectionUpdate: ({editor}) => {
        emit("select-range", editor)
    }
})

insertController = provideStructuralTextInsertController(createStructuralTextInsertController(editor))


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
            refreshMappings()
        }
    }
})

watch(() => props.extensions, () => {
    // Note: Tiptap doesn't support easy hot-reloading of extensions.
}, {deep: true})

watch([() => props.highlights, mappings], () => {
    if (editor.value) {
        // Trigger a re-render of decorations by dispatching a metadata-only transaction
        editor.value.view.dispatch(editor.value.state.tr.setMeta("locationHighlightRefresh", true));
    }
}, {deep: true});

onBeforeUnmount(() => {
    editor.value?.destroy()
})

defineExpose({
    editor,
    getLocationFromSelection: (selection: any) => {
        const startMap = mappings.value.find(m => m.start <= selection.from && m.end > selection.from);
        const endMap = mappings.value.find(m => m.start < selection.to && m.end >= selection.to);

        if (startMap && endMap) {
            return {
                startInNode: selection.from - startMap.start,
                endInNode: selection.to - endMap.start,
                startPath: startMap.path,
                endPath: endMap.path
            };
        }
        return null;
    },
    scrollToLocation: (loc: ContentLocationRange) => {
        const range = resolveLocationToRange(loc);
        if (range && editor.value) {
            // Scroll to center
            const {view} = editor.value;
            let dom = view.nodeDOM(range.from) as HTMLElement || view.domAtPos(range.from).node as HTMLElement;
            if (dom && dom.nodeType === 3) {
                dom = dom.parentElement as HTMLElement;
            }
            if (dom.scrollIntoView) {
                dom.scrollIntoView({block: "center", behavior: "smooth"});
            }
        }
    },
    refreshMappings
});
</script>

<template>
    <div :class="{'border border-gray-300 dark:border-gray-700 rounded-md': variant === 'outline'}">
        <EditorToolbar v-if="showToolbar && editor" :centered="ui && ui.toolbar ? ui.toolbar.centered : false"
                       :class="{[ui?.toolbar?.root || '']: ui && ui!.toolbar && ui!.toolbar.root,
                       'rounded-md border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900': variant === 'outline'
                       }"
                       :editor="editor"
                       :sticky="true">
            <template #menu-end>
                <slot name="toolbar-menu-end"/>
            </template>
        </EditorToolbar>
        <div class="flex relative">
            <div class="w-full mx-auto">
                <slot name="before-content"/>
                <EditorBubbleMenu v-if="editor" :editable="editable" :editor="editor">
                    <template #end>
                        <slot name="bubble-menu-end"/>
                    </template>
                </EditorBubbleMenu>
                <EditorDragHandle v-if="editor" :editor="editor"/>
                <EditorContextMenu v-if="editor" :editable="editable" :editor="editor">
                    <div ref="editorSurface" class="relative"
                         :class="{[ui?.content?.root || '']: ui && ui!.content && ui!.content.root}">
                        <EditorContent :editor="editor" class="editor"/>
                        <TableEdgeAffordances v-if="editor" :editor="editor" :editable="editable" :surface="editorSurface"/>
                    </div>
                </EditorContextMenu>
            </div>

            <!--TODO: fix aside-->
            <aside v-if="showOutline" :class="ui?.outline?.root || ''" class="hidden xl:block fixed right-4 w-72 z-30">
                <StructuralTextOutline :document="modelValue"
                                       :title="outlineTitle"
                                       color="primary">
                    <template #section-bottom>
                        <slot name="outline-bottom"/>
                    </template>
                    <template #footer>
                        <slot name="outline-footer"/>
                    </template>
                </StructuralTextOutline>
            </aside>
        </div>

        <LinkModal
                v-if="insertController"
                v-model:open="insertController.isLinkModalOpen.value"
                :initial-url="insertController.linkInitialUrl.value"
                :initial-text="insertController.linkInitialText.value"
                :is-editing="insertController.linkIsEditing.value"
                @confirm="insertController.confirmLink"
                @remove="insertController.removeLink"
        />

        <ImageModal
                v-if="insertController"
                v-model:open="insertController.isImageModalOpen.value"
                :initial-url="insertController.imageInitialUrl.value"
                :initial-alt="insertController.imageInitialAlt.value"
                @confirm="insertController.confirmImage"
        />

        <TableInsertModal
                v-if="insertController"
                v-model:open="insertController.isTableInsertModalOpen.value"
                :initial-rows="insertController.tableInitialRows.value"
                :initial-cols="insertController.tableInitialCols.value"
                :initial-with-header-row="insertController.tableInitialWithHeaderRow.value"
                @confirm="insertController.confirmTableInsert"
        />

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

.editor .math-block pre {
    @apply bg-transparent p-0 m-0;
}

.editor .ProseMirror-selectednode:not(img):not(pre):not([data-node-view-wrapper]) {
    @apply bg-primary/20;
}

/**
 * Structural Location Highlight styles
 */
.structural-location-highlight {
    @apply transition-all duration-300;
    box-decoration-break: clone;
    -webkit-box-decoration-break: clone;
}

.highlight-severity-active {
    @apply border-b-2 border-primary-500 bg-primary-500/10;
    box-decoration-break: clone;
    -webkit-box-decoration-break: clone;
}

</style>
