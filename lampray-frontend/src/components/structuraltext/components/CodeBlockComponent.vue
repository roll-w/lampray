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
import {NodeViewContent, type NodeViewProps, NodeViewWrapper} from "@tiptap/vue-3";
import {computed, nextTick, onMounted, ref, watch} from "vue";
import hljs from "highlight.js/lib/core";
import "highlight.js/styles/atom-one-light.css";

interface Props extends NodeViewProps {
}

const props = defineProps<Props>();

// Language loader map for dynamic import
const languageLoaders: Record<string, () => Promise<any>> = {
    javascript: () => import("highlight.js/lib/languages/javascript"),
    typescript: () => import("highlight.js/lib/languages/typescript"),
    python: () => import("highlight.js/lib/languages/python"),
    java: () => import("highlight.js/lib/languages/java"),
    cpp: () => import("highlight.js/lib/languages/cpp"),
    c: () => import("highlight.js/lib/languages/c"),
    csharp: () => import("highlight.js/lib/languages/csharp"),
    go: () => import("highlight.js/lib/languages/go"),
    rust: () => import("highlight.js/lib/languages/rust"),
    php: () => import("highlight.js/lib/languages/php"),
    ruby: () => import("highlight.js/lib/languages/ruby"),
    swift: () => import("highlight.js/lib/languages/swift"),
    kotlin: () => import("highlight.js/lib/languages/kotlin"),
    xml: () => import("highlight.js/lib/languages/xml"),
    html: () => import("highlight.js/lib/languages/xml"),
    css: () => import("highlight.js/lib/languages/css"),
    json: () => import("highlight.js/lib/languages/json"),
    yaml: () => import("highlight.js/lib/languages/yaml"),
    toml: () => import("highlight.js/lib/languages/ini"),
    markdown: () => import("highlight.js/lib/languages/markdown"),
    sql: () => import("highlight.js/lib/languages/sql"),
    bash: () => import("highlight.js/lib/languages/bash"),
    shell: () => import("highlight.js/lib/languages/shell"),
    powershell: () => import("highlight.js/lib/languages/powershell"),
};

const loadedLanguages = new Set<string>();

const supportedLanguages = [
    {value: "plain", label: "Plain Text"},
    {value: "javascript", label: "JavaScript"},
    {value: "typescript", label: "TypeScript"},
    {value: "python", label: "Python"},
    {value: "java", label: "Java"},
    {value: "cpp", label: "C++"},
    {value: "c", label: "C"},
    {value: "csharp", label: "C#"},
    {value: "go", label: "Go"},
    {value: "rust", label: "Rust"},
    {value: "php", label: "PHP"},
    {value: "ruby", label: "Ruby"},
    {value: "swift", label: "Swift"},
    {value: "kotlin", label: "Kotlin"},
    {value: "html", label: "HTML"},
    {value: "css", label: "CSS"},
    {value: "xml", label: "XML"},
    {value: "json", label: "JSON"},
    {value: "yaml", label: "YAML"},
    {value: "toml", label: "TOML"},
    {value: "markdown", label: "Markdown"},
    {value: "sql", label: "SQL"},
    {value: "bash", label: "Bash"},
    {value: "shell", label: "Shell"},
    {value: "powershell", label: "PowerShell"},
];

const selectedLanguage = computed({
    get: () => {
        const language = props.node?.attrs.language || "plain";
        return supportedLanguages.find(lang => lang.value === language) || supportedLanguages[0];
    },
    set: (value: typeof supportedLanguages[number]) => {
        if (props.updateAttributes) {
            props.updateAttributes({language: value.value});
        }
    }
});

const showLineNumbers = computed({
    get: () => props.node?.attrs.showLineNumbers ?? true,
    set: (value: boolean) => {
        if (props.updateAttributes) {
            props.updateAttributes({showLineNumbers: value});
        }
    }
});

const wrapLines = computed({
    get: () => props.node?.attrs.wrapLines ?? false,
    set: (value: boolean) => {
        if (props.updateAttributes) {
            props.updateAttributes({wrapLines: value});
        }
    }
});

const isCollapsed = ref(false);
const lineCount = ref(1);
const highlightedCode = ref("");
const codeElementRef = ref<HTMLElement | null>(null);
const lineHeights = ref<number[]>([]);
const lineNumbersRef = ref<HTMLElement | null>(null);

const toggleCollapse = () => {
    isCollapsed.value = !isCollapsed.value;
};

const copyCode = async () => {
    try {
        const code = props.node?.textContent || "";
        await navigator.clipboard.writeText(code);
    } catch (err) {
        console.error("Failed to copy code:", err);
    }
};

const loadLanguage = async (language: string): Promise<boolean> => {
    if (language === "plain" || loadedLanguages.has(language)) {
        return true;
    }

    const loader = languageLoaders[language];
    if (!loader) {
        console.warn(`No loader found for language: ${language}`);
        return false;
    }

    try {
        const module = await loader();
        hljs.registerLanguage(language, module.default);
        loadedLanguages.add(language);
        return true;
    } catch (err) {
        console.error(`Failed to load language ${language}:`, err);
        return false;
    }
};

const highlightCode = async () => {
    const code = props.node?.textContent || "";
    const language = selectedLanguage.value?.value || "plain";

    if (language === "plain" || !code) {
        highlightedCode.value = escapeHtml(code);
        return;
    }

    const loaded = await loadLanguage(language);
    if (!loaded) {
        highlightedCode.value = escapeHtml(code);
        return;
    }

    try {
        const result = hljs.highlight(code, {
            language: language,
            ignoreIllegals: true
        });
        highlightedCode.value = result.value;
    } catch (err) {
        console.warn("Failed to highlight code:", err);
        highlightedCode.value = escapeHtml(code);
    }
};

// Escape HTML to prevent XSS
const escapeHtml = (text: string): string => {
    const map: Record<string, string> = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    };
    return text.replace(/[&<>"']/g, (m) => map[m] || m);
};

// Calculate line count and heights based on actual rendered lines
const updateLineCount = async () => {
    await nextTick();

    const code = props.node?.textContent || "";
    if (!code) {
        lineCount.value = 1;
        lineHeights.value = [24]; // default line height
        return;
    }

    // If wrap is disabled, just count newlines
    if (!wrapLines.value) {
        const lines = code.split("\n");
        lineCount.value = Math.max(1, lines.length);
        lineHeights.value = Array(lineCount.value).fill(24); // 24px = leading-6
        return;
    }

    // For wrapped lines, calculate actual heights
    if (!codeElementRef.value) {
        const lines = code.split("\n");
        lineCount.value = Math.max(1, lines.length);
        lineHeights.value = Array(lineCount.value).fill(24);
        return;
    }

    // Get the pre element (either the overlay or the editable one)
    const preElement = codeElementRef.value.querySelector('pre');
    if (!preElement) {
        const lines = code.split("\n");
        lineCount.value = Math.max(1, lines.length);
        lineHeights.value = Array(lineCount.value).fill(24);
        return;
    }

    // Create a temporary element to measure each line with proper wrapping
    const lines = code.split("\n");
    const computedStyle = window.getComputedStyle(preElement);

    // Get the actual content width (excluding padding)
    const paddingLeft = parseFloat(computedStyle.paddingLeft) || 0;
    const paddingRight = parseFloat(computedStyle.paddingRight) || 0;
    const contentWidth = preElement.clientWidth - paddingLeft - paddingRight;

    const tempContainer = document.createElement('div');
    tempContainer.style.cssText = `
        position: absolute;
        visibility: hidden;
        width: ${contentWidth}px;
        font-family: ${computedStyle.fontFamily};
        font-size: ${computedStyle.fontSize};
        line-height: ${computedStyle.lineHeight};
        padding: 0;
        margin: 0;
        border: 0;
        box-sizing: content-box;
    `;

    const tempPre = document.createElement('pre');
    tempPre.style.cssText = `
        font-family: inherit;
        font-size: inherit;
        line-height: inherit;
        white-space: pre-wrap;
        word-wrap: break-word;
        overflow-wrap: break-word;
        word-break: break-word;
        padding: 0;
        margin: 0;
        tab-size: 4;
        -moz-tab-size: 4;
        box-sizing: content-box;
    `;

    tempContainer.appendChild(tempPre);
    document.body.appendChild(tempContainer);

    const heights: number[] = [];
    for (const line of lines) {
        tempPre.textContent = line || ' '; // Empty lines need at least a space
        const height = tempPre.offsetHeight;
        heights.push(height > 0 ? height : 24); // Fallback to 24px if height is 0
    }

    document.body.removeChild(tempContainer);

    lineCount.value = lines.length;
    lineHeights.value = heights;
};

// Sync vertical scroll between line numbers and code content
const syncScroll = (event: Event) => {
    const target = event.target as HTMLElement;
    if (!lineNumbersRef.value) return;

    lineNumbersRef.value.scrollTop = target.scrollTop;
};

watch([
    () => props.node?.textContent,
    () => selectedLanguage.value?.value,
    () => wrapLines.value
], () => {
    highlightCode();
    updateLineCount();
}, {immediate: true});

onMounted(() => {
    highlightCode();
    updateLineCount();
});

// Return line numbers with their corresponding heights
const lineNumbers = computed(() => {
    return Array.from({length: lineCount.value}, (_, i) => ({
        number: i + 1,
        height: lineHeights.value[i] || 24
    }));
});
</script>

<template>
    <NodeViewWrapper>
        <div class="border rounded-md border-gray-300 dark:border-gray-600 overflow-hidden my-4">
            <div contenteditable="false"
                 class="flex items-center justify-between px-3 py-2  bg-gray-100 dark:bg-gray-700"
                 :class="{
                     'border-b border-gray-300 dark:border-gray-600': !isCollapsed,
                 }">
                <div class="flex items-center gap-2">
                    <UButton :icon="isCollapsed ? 'i-lucide-chevron-right' : 'i-lucide-chevron-down'"
                             color="neutral"
                             variant="ghost"
                             size="xs"
                             square
                             @click="toggleCollapse"
                             :title="isCollapsed ? 'Expand' : 'Collapse'"
                    />
                    <USelectMenu v-if="editor?.isEditable"
                                 v-model="selectedLanguage"
                                 :items="supportedLanguages"
                                 size="xs"
                                 color="neutral"
                                 variant="outline"
                                 class="w-40"
                    />
                    <span v-else
                          class="text-xs font-medium text-gray-700 dark:text-gray-300 select-none"
                    >
                        {{ selectedLanguage?.label || "Plain Text" }}
                    </span>
                </div>

                <div class="flex items-center gap-1">
                    <UButton v-if="editor?.isEditable"
                             :icon="showLineNumbers ? 'i-lucide-list-ordered' : 'i-lucide-list'"
                             color="neutral"
                             variant="ghost"
                             size="xs"
                             square
                             @click="showLineNumbers = !showLineNumbers"
                             :title="showLineNumbers ? 'Hide line numbers' : 'Show line numbers'"
                    />
                    <UButton v-if="editor?.isEditable"
                             :icon="wrapLines ? 'i-lucide-wrap-text' : 'i-lucide-align-left'"
                             color="neutral"
                             variant="ghost"
                             size="xs"
                             square
                             @click="wrapLines = !wrapLines"
                             :title="wrapLines ? 'Disable line wrap' : 'Enable line wrap'"
                    />
                    <UButton icon="i-lucide-copy"
                             color="neutral"
                             variant="ghost"
                             size="xs"
                             square
                             @click="copyCode"
                             title="Copy code"
                    />
                </div>
            </div>

            <div v-show="!isCollapsed"
                 class="flex items-stretch"
                 :class="{
                    'overflow-x-auto': !wrapLines,
                    'overflow-x-hidden': wrapLines
                }"
            >
                <div v-if="showLineNumbers"
                     ref="lineNumbersRef"
                     class="select-none shrink-0 border-r border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-900 px-3 py-4 text-right sticky left-0 z-20"
                     :class="{
                        'overflow-y-auto': !wrapLines,
                        'overflow-y-hidden': wrapLines
                    }"
                     contenteditable="false"
                >
                    <div v-for="line in lineNumbers"
                         :key="line.number"
                         class="text-xs text-gray-500 dark:text-gray-400 font-mono leading-6 min-h-6"
                         :style="{ height: line.height + 'px' }"
                    >
                        {{ line.number }}
                    </div>
                </div>

                <div class="flex-1 relative bg-white dark:bg-gray-800"
                     ref="codeElementRef"
                     :class="{
                        'overflow-x-auto': !wrapLines,
                        'overflow-x-hidden': wrapLines
                    }"
                     @scroll="syncScroll"
                >
                    <pre class="absolute inset-0 p-4 text-sm font-mono leading-6 min-h-12 pointer-events-none z-0"
                         :class="{
                            '!whitespace-pre-wrap !break-words !overflow-wrap-break-word overflow-hidden': wrapLines,
                            '!whitespace-pre overflow-x-visible': !wrapLines
                        }"
                         style="tab-size: 4; -moz-tab-size: 4;"
                         v-html="highlightedCode"
                    />
                    <NodeViewContent
                            as="pre"
                            class="p-4 text-sm font-mono leading-6 focus:outline-none min-h-12 relative z-10 text-transparent caret-black dark:caret-white selection:text-transparent selection:bg-blue-300 selection:bg-transparent"
                            :class="{
                              '!whitespace-pre-wrap !break-words !overflow-wrap-break-word': wrapLines,
                              '!whitespace-pre': !wrapLines,
                            }"
                            style="tab-size: 4; -moz-tab-size: 4;"
                    />
                </div>
            </div>
        </div>
    </NodeViewWrapper>
</template>

<style scoped>
</style>