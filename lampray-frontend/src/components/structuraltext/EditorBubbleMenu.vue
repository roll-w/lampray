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
import {useEditorActions} from "@/components/structuraltext/composables/useEditorActions";
import {useI18n} from "vue-i18n";
import LinkModal from "@/components/structuraltext/modals/LinkModal.vue";

interface Props {
    editor: Editor;
    editable: boolean;
}

const props = defineProps<Props>();
const {t} = useI18n();

const {
    toggleBold,
    toggleItalic,
    toggleStrike,
    toggleCode,
    toggleHighlight,
    setLink,
    unsetLink,
    getLinkHref,
    getSelectedText,
    copySelectedText,
    isBold,
    isItalic,
    isStrike,
    isCode,
    isHighlight,
    isLink,
    isCodeBlock,
} = useEditorActions(props.editor);

const isLinkModalOpen = ref(false);

const isCopied = ref(false);

const copyText = async () => {
    await copySelectedText();
    isCopied.value = true;
    setTimeout(() => {
        isCopied.value = false;
    }, 2000);
};


const openLinkModal = () => {
    isLinkModalOpen.value = true;
};

const openLink = () => {
    const href = getLinkHref();
    if (href) {
        window.open(href, "_blank");
    }
};

const handleLinkConfirm = ({url}: { url: string }) => {
    setLink(url);
};

const handleLinkRemove = () => {
    unsetLink();
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
                                 @click="openLinkModal"/>
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
                    <UButton :variant="isBold ? 'solid' : 'ghost'"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-bold"
                             @click="toggleBold"
                    />
                </UTooltip>
                <UTooltip :text="t('editor.toolbar.italic')">
                    <UButton :variant="isItalic ? 'solid' : 'ghost'"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-italic"
                             @click="toggleItalic"
                    />
                </UTooltip>
                <UTooltip :text="t('editor.toolbar.strikethrough')">
                    <UButton :variant="isStrike ? 'solid' : 'ghost'"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-strikethrough"
                             @click="toggleStrike"
                    />
                </UTooltip>
                <UTooltip v-if="!isCodeBlock" :text="t('editor.toolbar.inlineCode')">
                    <UButton :variant="isCode ? 'solid' : 'ghost'"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-code"
                             @click="toggleCode"
                    />
                </UTooltip>
                <UTooltip v-if="!isCodeBlock" :text="t('editor.toolbar.highlight')">
                    <UButton :variant="isHighlight ? 'solid' : 'ghost'"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-highlighter"
                             @click="toggleHighlight"
                    />
                </UTooltip>

                <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>

                <UTooltip v-if="!isLink && !isCodeBlock" :text="t('editor.toolbar.addLink')">
                    <UButton variant="ghost"
                             color="neutral"
                             size="xs"
                             icon="i-lucide-link"
                             @click="openLinkModal"
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
        </div>
    </BubbleMenu>

    <LinkModal
            v-model:open="isLinkModalOpen"
            :initial-url="getLinkHref()"
            :initial-text="getSelectedText()"
            :is-editing="isLink"
            @confirm="handleLinkConfirm"
            @remove="handleLinkRemove"
    />
</template>

