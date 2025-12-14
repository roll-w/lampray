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
import type {Editor} from "@tiptap/core";
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import {useEditorActions} from "@/components/structuraltext/composables/useEditorActions";
import {useI18n} from "vue-i18n";
import LinkModal from "@/components/structuraltext/modals/LinkModal.vue";
import ImageModal from "@/components/structuraltext/modals/ImageModal.vue";

interface Props {
    editor: Editor;
    // When true, toolbar menu items will be centered horizontally
    centered?: boolean;
    // When true, toolbar will stick to top of viewport when scrolling
    sticky?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    centered: false,
    sticky: false
});
const {t} = useI18n();

const {
    toggleBold,
    toggleItalic,
    toggleStrike,
    toggleUnderline,
    toggleCode,
    toggleHighlight,
    setLink,
    getLinkHref,
    isBold,
    isItalic,
    isStrike,
    isUnderline,
    isCode,
    isHighlight,
} = useEditorActions(props.editor);

const isLinkModalOpen = ref(false);
const isImageModalOpen = ref(false);

const headingOptions = [
    {label: t("editor.toolbar.paragraph"), value: 0, icon: "i-lucide-pilcrow"},
    {label: t("editor.toolbar.heading1"), value: 1, icon: "i-lucide-heading-1"},
    {label: t("editor.toolbar.heading2"), value: 2, icon: "i-lucide-heading-2"},
    {label: t("editor.toolbar.heading3"), value: 3, icon: "i-lucide-heading-3"},
    {label: t("editor.toolbar.heading4"), value: 4, icon: "i-lucide-heading-4"},
    {label: t("editor.toolbar.heading5"), value: 5, icon: "i-lucide-heading-5"},
    {label: t("editor.toolbar.heading6"), value: 6, icon: "i-lucide-heading-6"},
];

const currentHeading = computed({
    get() {
        for (let level = 1; level <= 6; level++) {
            if (props.editor.isActive("heading", {level})) {
                return headingOptions[level];
            }
        }
        return headingOptions[0];
    },
    set(option: any) {
        // Delay to allow dropdown to close first, then apply heading
        setTimeout(() => {
            if (option.value === 0) {
                props.editor.chain().focus().setParagraph().run();
            } else {
                props.editor.chain().focus().setHeading({level: option.value as 1 | 2 | 3 | 4 | 5 | 6}).run();
            }
        }, 10);
    }
});

const toggleBulletList = () => {
    props.editor.chain().focus().toggleBulletList().run();
};
const toggleOrderedList = () => {
    props.editor.chain().focus().toggleOrderedList().run();
};
const toggleTaskList = () => {
    props.editor.chain().focus().toggleTaskList().run();
};
const toggleBlockquote = () => {
    props.editor.chain().focus().toggleBlockquote().run();
};
const toggleCodeBlock = () => {
    const {state} = props.editor;
    if (props.editor.isActive("codeBlock")) {
        return;
    }
    const {from, to} = state.selection;
    if (from !== to) {
        props.editor.chain().focus().setCodeBlock().run();
    } else {
        props.editor.chain()
                .insertContent({
                    type: "codeBlock",
                })
                .focus()
                .run();
    }
};

const insertTable = () => {
    props.editor.chain().focus().insertTable({rows: 3, cols: 3, withHeaderRow: true}).run();
};

const insertHorizontalRule = () => {
    props.editor.chain().focus().setHorizontalRule().run();
};

const openImageModal = () => {
    isImageModalOpen.value = true;
};

const handleImageConfirm = ({url, alt}: { url: string; alt?: string }) => {
    props.editor.chain().focus().setImage({
        src: url,
        alt: alt || undefined
    }).run();
};

const openLinkModal = () => {
    isLinkModalOpen.value = true;
};

const handleLinkConfirm = ({url}: { url: string }) => {
    setLink(url);
};

const isBulletList = computed(() => props.editor.isActive("bulletList"));
const isOrderedList = computed(() => props.editor.isActive("orderedList"));
const isTaskList = computed(() => props.editor.isActive("taskList"));
const isBlockquote = computed(() => props.editor.isActive("blockquote"));
const isCodeBlock = computed(() => props.editor.isActive("codeBlock"));

const toolbarClasses = computed(() => {
    return [
        "flex flex-wrap gap-1 p-2",
        {"justify-center": props.centered, "bg-default/75 backdrop-blur sticky z-40": props.sticky,}
    ];
});

const toolbarRoot = ref<HTMLElement | null>(null)
let resizeObserver: ResizeObserver | null = null

function setToolbarHeight(height: number) {
    if (typeof document === 'undefined') {
        return
    }
    document.documentElement.style.setProperty('--toolbar-height', `${Math.ceil(height)}px`)
}

function measureToolbar() {
    const el = toolbarRoot.value
    if (!el) return
    const rect = el.getBoundingClientRect()
    setToolbarHeight(rect.height)
}

onMounted(() => {
    nextTick(() => {
        // Initial measure
        try {
            measureToolbar()
        } catch (e) {
        }
        if (typeof ResizeObserver !== 'undefined' && toolbarRoot.value) {
            resizeObserver = new ResizeObserver(() => measureToolbar())
            resizeObserver.observe(toolbarRoot.value)
        }
    })
})

onBeforeUnmount(() => {
    if (resizeObserver) {
        try {
            resizeObserver.disconnect()
        } catch (e) {
        }
        resizeObserver = null
    }
    // When toolbar is removed, set toolbar-height to 0 to avoid leaving stale value
    try {
        setToolbarHeight(0)
    } catch (e) {
    }
})
</script>

<template>
    <div :class="toolbarClasses" ref="toolbarRoot">
        <div class="flex gap-1">
            <UTooltip :text="t('editor.toolbar.bold')">
                <UButton
                        :variant="isBold ? 'soft' : 'ghost'"
                        :color="isBold ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-bold"
                        @click="toggleBold"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.italic')">
                <UButton
                        :variant="isItalic ? 'soft' : 'ghost'"
                        :color="isItalic ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-italic"
                        @click="toggleItalic"
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
            <UTooltip :text="t('editor.toolbar.strikethrough')">
                <UButton
                        :variant="isStrike ? 'soft' : 'ghost'"
                        :color="isStrike ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-strikethrough"
                        @click="toggleStrike"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.highlight')">
                <UButton
                        :variant="isHighlight ? 'soft' : 'ghost'"
                        :color="isHighlight ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-highlighter"
                        @click="toggleHighlight"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.inlineCode')">
                <UButton
                        :variant="isCode ? 'soft' : 'ghost'"
                        :color="isCode ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-code"
                        @click="toggleCode"
                />
            </UTooltip>

            <USelectMenu
                    v-model="currentHeading"
                    :items="headingOptions"
                    size="sm"
                    color="neutral"
                    variant="ghost"
                    class="w-36"
            >
                <template #default>
                    <div class="flex items-center gap-2">
                        <UIcon :name="currentHeading!.icon" class="w-4 h-4"/>
                        <span class="text-xs">{{ currentHeading!.label }}</span>
                    </div>
                </template>
            </USelectMenu>

            <UTooltip :text="t('editor.toolbar.bulletList')">
                <UButton
                        :variant="isBulletList ? 'soft' : 'ghost'"
                        :color="isBulletList ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-list"
                        @click="toggleBulletList"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.orderedList')">
                <UButton
                        :variant="isOrderedList ? 'soft' : 'ghost'"
                        :color="isOrderedList ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-list-ordered"
                        @click="toggleOrderedList"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.taskList')">
                <UButton
                        :variant="isTaskList ? 'soft' : 'ghost'"
                        :color="isTaskList ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-list-checks"
                        @click="toggleTaskList"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.blockquote')">
                <UButton
                        :variant="isBlockquote ? 'soft' : 'ghost'"
                        :color="isBlockquote ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-quote"
                        @click="toggleBlockquote"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.codeBlock')">
                <UButton
                        :variant="isCodeBlock ? 'soft' : 'ghost'"
                        :color="isCodeBlock ? 'primary' : 'neutral'"
                        size="sm"
                        icon="i-lucide-code-2"
                        @click="toggleCodeBlock"
                />
            </UTooltip>
        </div>

        <div class="w-px h-6 bg-gray-300 dark:bg-gray-600"/>

        <div class="flex gap-1">
            <UTooltip :text="t('editor.toolbar.insertLink')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-link"
                        @click="openLinkModal"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.insertImage')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-image"
                        @click="openImageModal"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.insertTable')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-table"
                        @click="insertTable"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.horizontalRule')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-minus"
                        @click="insertHorizontalRule"
                />
            </UTooltip>
            <slot name="menu-end"/>
        </div>

        <LinkModal
                v-model:open="isLinkModalOpen"
                :initial-url="getLinkHref()"
                @confirm="handleLinkConfirm"
        />

        <ImageModal
                v-model:open="isImageModalOpen"
                @confirm="handleImageConfirm"
        />
    </div>
</template>
