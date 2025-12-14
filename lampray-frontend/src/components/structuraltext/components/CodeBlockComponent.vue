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
import {lowlight} from "@/components/structuraltext/extensions/CodeBlock";
import {loadLanguage, supportedLanguages} from "@/components/structuraltext/extensions/LanguageLoader";
import {useColorMode, useDebounceFn, useElementSize, useEventListener, useResizeObserver} from "@vueuse/core";
import {useI18n} from "vue-i18n";

// TODO: may replace with CodeMirror for better editing experience

const colorMode = useColorMode();
const {t} = useI18n();

watch(() => colorMode.value, async (mode) => {
    if (mode === "dark") {
        await import("highlight.js/styles/atom-one-dark.css");
    } else {
        await import("highlight.js/styles/atom-one-light.css");
    }
}, {immediate: true});

interface Props extends NodeViewProps {
}

const props = defineProps<Props>();

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

watch(() => selectedLanguage.value?.value, async (newLang) => {
    if (newLang && newLang !== "plain") {
        await loadLanguage(lowlight, newLang);
        if (props.editor) {
            props.editor.view.dispatch(props.editor.state.tr);
        }
    }
}, {immediate: true});

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
const codeElementRef = ref<HTMLElement | null>(null);
const lineHeights = ref<number[]>([]);
const isCopied = ref(false);

// Active line set for highlighting line numbers
const activeLineSet = ref<Set<number>>(new Set());

// Caches to avoid repeated computations
const cachedLines = ref<string[]>([]);
const cachedStarts = ref<number[]>([]);
const charWidthRef = ref<number>(8);
const measuredFontRef = ref<string | null>(null);

const buildStarts = (lines: string[]) => {
    const starts: number[] = [];
    let acc = 0;
    for (let i = 0; i < lines.length; i++) {
        starts.push(acc);
        const ln = lines[i] ?? "";
        acc += ln.length + (i < lines.length - 1 ? 1 : 0);
    }
    return starts;
};

// Measure char width once and cache it
const measureCharWidth = (font: string | undefined) => {
    try {
        // If font is unchanged, keep cached value
        if (font && measuredFontRef.value === font && charWidthRef.value > 0) return charWidthRef.value;
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        if (!ctx) return 8; // fallback
        if (font) ctx.font = font;
        const metrics = ctx.measureText('0');
        const w = metrics.width || 8;
        charWidthRef.value = w;
        measuredFontRef.value = font ?? null;
        return w;
    } catch (e) {
        return 8;
    }
};

watch(() => props.node?.textContent, (val) => {
    const code = val || "";
    const lines = code.split('\n');
    cachedLines.value = lines;
    cachedStarts.value = buildStarts(lines);
    updateLineCountDebounced();
    updateActiveLinesDebounced();
});

const updateActiveLines = () => {
    activeLineSet.value.clear();

    if (!props.editor || typeof props.getPos !== 'function' || !props.node) return;

    try {
        const sel = props.editor.state.selection;
        const nodePos = props.getPos();
        if (typeof nodePos !== 'number') return;

        const contentStart = nodePos + 1;
        const nodeSize = props.node.nodeSize ?? 0;
        if (sel.to <= contentStart || sel.from >= contentStart + Math.max(0, nodeSize - 2)) return;

        const relativeFrom = Math.max(0, sel.from - contentStart);
        const relativeTo = Math.max(0, sel.to - contentStart);

        const lines = cachedLines.value.length ? cachedLines.value : (props.node?.textContent || "").split('\n');
        const starts = cachedStarts.value.length ? cachedStarts.value : buildStarts(lines);
        const lastLineLen = lines.length ? (lines[lines.length - 1]?.length ?? 0) : 0;
        const acc = starts.length ? ((starts[starts.length - 1] ?? 0) + lastLineLen) : 0;

        const findLineIndex = (offset: number) => {
            // binary search on starts
            let lo = 0;
            let hi = Math.max(0, starts.length - 1);
            if (starts.length === 0) return 0;
            if (offset >= acc) return Math.max(0, lines.length - 1);
            while (lo <= hi) {
                const mid = Math.floor((lo + hi) / 2);
                const s = starts[mid] ?? 0;
                const e = (mid < starts.length - 1) ? (starts[mid + 1] ?? acc) : acc;
                if (offset < s) {
                    hi = mid - 1;
                } else if (offset >= e) {
                    lo = mid + 1;
                } else {
                    return mid;
                }
            }
            return Math.max(0, Math.min(lines.length - 1, lo));
        };

        const startIndex = findLineIndex(relativeFrom);
        const endIndex = findLineIndex(Math.max(0, relativeTo - 1));

        for (let i = startIndex; i <= endIndex; i++) activeLineSet.value.add(i + 1);
    } catch (e) {
        console.error('updateActiveLines error', e);
    }
};

const toggleCollapse = () => {
    isCollapsed.value = !isCollapsed.value;
};

const copyCode = async () => {
    try {
        const code = props.node?.textContent || "";
        await navigator.clipboard.writeText(code);
        isCopied.value = true;
        setTimeout(() => {
            isCopied.value = false;
        }, 2000);
    } catch (err) {
        console.error("Failed to copy code:", err);
    }
};

// TODO: Optimize line height calculation
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

    const elSize = useElementSize(codeElementRef);
    const contentWidth = (elSize.width.value && elSize.width.value > 0) ? elSize.width.value : undefined;

    const lines = code.split("\n");
    if (!contentWidth) {
        // Fallback to simple per-line fixed height
        lineCount.value = Math.max(1, lines.length);
        lineHeights.value = Array(lineCount.value).fill(24);
        return;
    }

    const tabSize = 4;
    const charsPerLine = Math.max(10, Math.floor(contentWidth / Math.max(1, charWidthRef.value)));

    const baseLineHeight =  (() => {
        const preEl = codeElementRef.value?.querySelector('pre') as HTMLElement | null;
        const computedStyle = preEl ? window.getComputedStyle(preEl) : undefined;
        if (computedStyle && computedStyle.lineHeight && computedStyle.lineHeight !== 'normal') {
            const v = parseFloat(computedStyle.lineHeight as string);
            return isNaN(v) ? (parseFloat(computedStyle.fontSize || '16') * 1.2) : v;
        }
        return computedStyle ? (parseFloat(computedStyle.fontSize || '16') * 1.2) : 20;
    })();

    const heights: number[] = [];
    let visualLineCount = 0;
    for (const ln of lines) {
        // Expand tabs to spaces for length estimation without regex
        const parts = ln.split('\t');
        const expanded = parts.join(' '.repeat(tabSize));
        const length = expanded.length;
        const vLines = Math.max(1, Math.ceil(length / charsPerLine));
        const h = Math.max( Math.round(vLines * baseLineHeight), 16);
        heights.push(h);
        visualLineCount += vLines;
    }

    lineCount.value = lines.length;
    lineHeights.value = heights;
};

watch([
    () => props.node?.textContent,
    () => wrapLines.value
], () => {
    updateLineCount();
    updateActiveLines();
}, {immediate: true});

onMounted(() => {
    updateLineCount();
    updateActiveLinesDebounced();

    useEventListener(document, 'selectionchange', () => {
        updateActiveLinesDebounced();
    });

    const editorDom = props.editor?.view?.dom;
    if (editorDom) {
        useEventListener(editorDom, 'mouseup', () => updateActiveLinesDebounced());
        useEventListener(editorDom, 'keyup', () => updateActiveLinesDebounced());
    }

    // Ensure code area is selectable when editor is read-only by stopping
    // ProseMirror's node-selection behavior on mousedown/touchstart. Use
    // vueuse's useEventListener on the code container ref and check editor state.
    const stopIfReadOnly = (e: Event) => {
        try {
            if (!props.editor?.isEditable) {
                e.stopPropagation();
                // Allow the browser to handle selection inside the pre element
            }
        } catch (err) {
            // ignore
        }
    };

    // Use capture phase so we intercept the event before editor handlers.
    useEventListener(codeElementRef, 'mousedown', stopIfReadOnly, {passive: false, capture: true});
    useEventListener(codeElementRef, 'touchstart', stopIfReadOnly, {passive: false, capture: true});

    // measure char width initially
    const preEl = codeElementRef.value?.querySelector('pre') as HTMLElement | null;
    const computedStyle = preEl ? window.getComputedStyle(preEl) : undefined;
    const font = computedStyle ? `${computedStyle.fontWeight} ${computedStyle.fontSize} ${computedStyle.fontFamily}` : undefined;
    measureCharWidth(font);

    useResizeObserver(codeElementRef, () => {
        // On resize, recompute char width & line counts
        const pre = codeElementRef.value?.querySelector('pre') as HTMLElement | null;
        const style = pre ? window.getComputedStyle(pre) : undefined;
        const f = style ? `${style.fontWeight} ${style.fontSize} ${style.fontFamily}` : undefined;
        measureCharWidth(f);
        updateLineCountDebounced();
    });
});

const lineNumbers = computed(() => {
    return Array.from({length: lineCount.value}, (_, i) => ({
        number: i + 1,
        height: lineHeights.value[i] || 24
    }));
});

// Debounce heavy operations (declare before usage)
const updateActiveLinesDebounced = useDebounceFn(updateActiveLines, 50);
const updateLineCountDebounced = useDebounceFn(updateLineCount, 120);

const selected = computed(() => props.selected || false);
const isEditorEditable = computed(() => props.editor?.isEditable ?? false);
</script>

<template>
    <NodeViewWrapper>
        <div class="border rounded-md overflow-hidden my-4 transition-all duration-200"
             :class="{
                 'border-gray-300 dark:border-gray-600': !selected,
                 'border-primary-500 dark:border-primary-400 ring-2 ring-primary-500/20 dark:ring-primary-400/20': selected
             }">
            <div contenteditable="false"
                 class="flex items-center justify-between px-3 py-2"
                 :class="{
                     'border-b border-gray-300 dark:border-gray-600': !isCollapsed,
                 }">
                <div class="flex items-center gap-2">
                    <UTooltip :text="isCollapsed ? t('editor.codeBlock.expand') : t('editor.codeBlock.collapse')">
                        <UButton :icon="isCollapsed ? 'i-lucide-chevron-right' : 'i-lucide-chevron-down'"
                                 color="neutral"
                                 variant="ghost"
                                 size="xs"
                                 @click="toggleCollapse"
                        />
                    </UTooltip>
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
                        {{ selectedLanguage?.label || t('editor.codeBlock.plainText') }}
                    </span>
                </div>

                <div class="flex items-center gap-1">
                    <UTooltip :text="showLineNumbers ? t('editor.codeBlock.hideLineNumbers') : t('editor.codeBlock.showLineNumbers')">
                        <UButton
                                :icon="showLineNumbers ? 'i-lucide-list-ordered' : 'i-lucide-list'"
                                color="neutral"
                                variant="ghost"
                                size="xs"
                                @click="showLineNumbers = !showLineNumbers"
                        />
                    </UTooltip>
                    <UTooltip :text="wrapLines ? t('editor.codeBlock.disableLineWrap') : t('editor.codeBlock.enableLineWrap')">
                        <UButton
                                :icon="wrapLines ? 'i-lucide-wrap-text' : 'i-lucide-align-left'"
                                color="neutral"
                                variant="ghost"
                                size="xs"
                                @click="wrapLines = !wrapLines"
                        />
                    </UTooltip>
                    <UTooltip :text="isCopied ? t('editor.codeBlock.copied') : t('editor.codeBlock.copyCode')">
                        <UButton :icon="isCopied ? 'i-lucide-check' : 'i-lucide-copy'"
                                 :color="isCopied ? 'success' : 'neutral'"
                                 variant="ghost"
                                 size="xs"
                                 @click="copyCode"
                        />
                    </UTooltip>
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
                     class="select-none shrink-0 border-r border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-900 px-3 py-4 text-right sticky left-0 z-10"
                     :class="{
                        'overflow-y-auto': !wrapLines,
                        'overflow-y-hidden': wrapLines
                    }"
                     contenteditable="false"
                >
                    <div v-for="line in lineNumbers"
                         :key="line.number"
                         class="text-xs text-gray-500 dark:text-gray-400 font-mono leading-6 min-h-6"
                         :class="{
                             'text-primary-800 dark:text-primary-200': activeLineSet.has(line.number),
                             'text-gray-500 dark:text-gray-400': !activeLineSet.has(line.number)
                         }"
                         :style="{ height: line.height + 'px' }"
                    >
                        {{ line.number }}
                    </div>
                </div>

                <div class="flex-1 relative"
                     ref="codeElementRef"
                     :class="{
                        'overflow-x-auto': !wrapLines,
                        'overflow-x-hidden': wrapLines
                    }"
                >
                    <NodeViewContent
                            as="pre"
                            class="p-4 text-sm font-mono leading-6 focus:outline-none min-h-12 relative z-5"
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