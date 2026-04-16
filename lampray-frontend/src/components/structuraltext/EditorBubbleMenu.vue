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
import {BubbleMenu} from "@tiptap/vue-3/menus";
import type {Editor} from "@tiptap/core";
import {ref} from "vue";
import {parseHttpUrl, useEditorActions} from "@/components/structuraltext/composables/useEditorActions";
import {useStructuralTextInsertController} from "@/components/structuraltext/composables/useStructuralTextInsertController";
import {useI18n} from "vue-i18n";

interface Props {
    editor: Editor;
    editable: boolean;
}

const props = defineProps<Props>();
const {t} = useI18n();
const insertController = useStructuralTextInsertController()

const {
    toggleBold,
    toggleItalic,
    toggleStrike,
    toggleCode,
    toggleHighlight,
    toggleUnderline,
    getLinkHref,
    copySelectedText,
    isBold,
    isItalic,
    isStrike,
    isUnderline,
    isCode,
    isHighlight,
    isLink,
    isCodeBlock,
} = useEditorActions(props.editor);

const isCopied = ref(false);

const copyText = async () => {
    await copySelectedText();
    isCopied.value = true;
    setTimeout(() => {
        isCopied.value = false;
    }, 2000);
};
const openLink = () => {
    const href = getLinkHref();
    const parsedUrl = parseHttpUrl(href)
    if (parsedUrl) {
        window.open(parsedUrl.toString(), "_blank", "noopener,noreferrer");
    }
};

const shouldShow = ({editor, from, to}: { editor: Editor; from: number; to: number }) => {
    return from !== to || editor.isActive("link");
};

</script>

<template>
    <BubbleMenu :editor="editor"
                class="z-20"
                :options="{ placement: 'bottom' }"
                :should-show="shouldShow">
        <div class="flex gap-1 p-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg">
            <template v-if="editable">
                <template v-if="isLink">
                    <UTooltip :text="t('editor.toolbar.editLink')">
                        <UButton variant="ghost"
                                 color="neutral"
                                 size="xs"
                                 icon="i-lucide-pencil"
                                 @click="insertController.openLinkModalFromSelection"/>
                    </UTooltip>
                    <UTooltip :text="t('editor.toolbar.openLink')">
                        <UButton variant="ghost"
                                 color="neutral"
                                 size="xs"
                                 icon="i-lucide-external-link"
                                 @click="openLink"
                        />
                    </UTooltip>
                    <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>
                </template>
                <UTooltip :text="t('editor.toolbar.bold')">
                    <UButton :variant="isBold ? 'soft' : 'ghost'"
                             :color="isBold ? 'primary' : 'neutral'"
                             size="xs"
                             icon="i-lucide-bold"
                             @click="toggleBold"
                    />
                </UTooltip>
                <UTooltip :text="t('editor.toolbar.italic')">
                    <UButton :variant="isItalic ? 'soft' : 'ghost'"
                             :color="isItalic ? 'primary' : 'neutral'"
                             size="xs"
                             icon="i-lucide-italic"
                             @click="toggleItalic"
                    />
                </UTooltip>
                <UTooltip :text="t('editor.toolbar.strikethrough')">
                    <UButton :variant="isStrike ? 'soft' : 'ghost'"
                             :color="isStrike ? 'primary' : 'neutral'"
                             size="xs"
                             icon="i-lucide-strikethrough"
                             @click="toggleStrike"
                    />
                </UTooltip>
                <UTooltip :text="t('editor.toolbar.underline')">
                    <UButton
                            :variant="isUnderline ? 'soft' : 'ghost'"
                            :color="isUnderline ? 'primary' : 'neutral'"
                            size="sm"
                            icon="i-lucide-underline"
                            @click="toggleUnderline"
                    />
                </UTooltip>
                <UTooltip v-if="!isCodeBlock" :text="t('editor.toolbar.highlight')">
                    <UButton :variant="isHighlight ? 'soft' : 'ghost'"
                             :color="isHighlight ? 'primary' : 'neutral'"
                             size="xs"
                             icon="i-lucide-highlighter"
                             @click="toggleHighlight"
                    />
                </UTooltip>
                <UTooltip v-if="!isCodeBlock" :text="t('editor.toolbar.inlineCode')">
                    <UButton :variant="isCode ? 'soft' : 'ghost'"
                             :color="isCode ? 'primary' : 'neutral'"
                             size="xs"
                             icon="i-lucide-code"
                             @click="toggleCode"
                    />
                </UTooltip>

                <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>

                <UTooltip v-if="!isLink && !isCodeBlock" :text="t('editor.toolbar.addLink')">
                    <UButton variant="ghost"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-link"
                             @click="insertController.openLinkModalFromSelection"
                     />
                </UTooltip>
            </template>
            <UTooltip :text="t('editor.toolbar.copy')">
                <UButton :icon="isCopied ? 'i-lucide-check' : 'i-lucide-copy'"
                         :color="isCopied ? 'success' : 'neutral'"
                         variant="ghost"
                         size="xs"
                         @click="copyText"
                />
            </UTooltip>
            <slot name="end" />
        </div>
    </BubbleMenu>
</template>
