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
import {useColorMode} from "@vueuse/core";
import {useI18n} from "vue-i18n";

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

watch([
    () => props.node?.textContent,
    () => wrapLines.value
], () => {
    updateLineCount();
}, {immediate: true});

onMounted(() => {
    updateLineCount();
});

const lineNumbers = computed(() => {
    return Array.from({length: lineCount.value}, (_, i) => ({
        number: i + 1,
        height: lineHeights.value[i] || 24
    }));
});

const selected = computed(() => props.selected || false);
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
                    <UTooltip v-if="editor?.isEditable" :text="showLineNumbers ? t('editor.codeBlock.hideLineNumbers') : t('editor.codeBlock.showLineNumbers')">
                        <UButton
                                :icon="showLineNumbers ? 'i-lucide-list-ordered' : 'i-lucide-list'"
                                color="neutral"
                                variant="ghost"
                                size="xs"
                                @click="showLineNumbers = !showLineNumbers"
                        />
                    </UTooltip>
                    <UTooltip v-if="editor?.isEditable" :text="wrapLines ? t('editor.codeBlock.disableLineWrap') : t('editor.codeBlock.enableLineWrap')">
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