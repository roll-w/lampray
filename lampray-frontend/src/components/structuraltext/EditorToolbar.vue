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

<script setup lang="ts">
import type {Editor} from "@tiptap/core";
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import {useEditorActions} from "@/components/structuraltext/composables/useEditorActions";
import {
    useStructuralTextInsertController
} from "@/components/structuraltext/composables/useStructuralTextInsertController";
import {useI18n} from "vue-i18n";
import {useTableActions} from "@/components/structuraltext/composables/useTableActions";
import type {AttributeColor} from "@/components/structuraltext/types";
import {
    editorColorButtonClass,
    editorDropdownMenuUi,
    editorMenuSurfaceClass,
    editorSelectMenuUi,
    editorTableToolbarGroupClass,
    editorToolbarGroupClass,
} from "@/components/structuraltext/editorUi";

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
type HeadingLevel = 0 | 1 | 2 | 3 | 4 | 5 | 6

interface HeadingOption {
    label: string
    value: HeadingLevel
    icon: string
}

const {
    colors: tableColors,
    selectColor: selectTableColor,
    isInTable,
    isInCell,
    primaryToolbarActions,
    dropdownMenuItems,
} = useTableActions(props.editor, t)
const insertController = useStructuralTextInsertController()

const {
    toggleBold,
    toggleItalic,
    toggleStrike,
    toggleUnderline,
    toggleCode,
    toggleHighlight,
    isBold,
    isItalic,
    isStrike,
    isUnderline,
    isCode,
    isHighlight,
} = useEditorActions(props.editor);

const headingOptions: HeadingOption[] = [
    {label: t("editor.toolbar.paragraph"), value: 0, icon: "i-lucide-pilcrow"},
    {label: t("editor.toolbar.heading1"), value: 1, icon: "i-lucide-heading-1"},
    {label: t("editor.toolbar.heading2"), value: 2, icon: "i-lucide-heading-2"},
    {label: t("editor.toolbar.heading3"), value: 3, icon: "i-lucide-heading-3"},
    {label: t("editor.toolbar.heading4"), value: 4, icon: "i-lucide-heading-4"},
    {label: t("editor.toolbar.heading5"), value: 5, icon: "i-lucide-heading-5"},
    {label: t("editor.toolbar.heading6"), value: 6, icon: "i-lucide-heading-6"},
];

const currentHeading = computed<HeadingOption>({
    get() {
        for (let level = 1; level <= 6; level++) {
            if (props.editor.isActive("heading", {level})) {
                return headingOptions[level];
            }
        }
        return headingOptions[0];
    },
    set(option) {
        // Delay to allow dropdown to close first, then apply heading
        setTimeout(() => {
            const headingLevel = option.value

            if (headingLevel === 0) {
                props.editor.chain().focus().setParagraph().run();
            } else {
                props.editor.chain().focus().setHeading({level: headingLevel}).run();
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

const insertHorizontalRule = () => {
    props.editor.chain().focus().setHorizontalRule().run();
};

const handleTableColorSelect = (color: AttributeColor | null) => {
    selectTableColor(color)
}

const isBulletList = computed(() => props.editor.isActive("bulletList"));
const isOrderedList = computed(() => props.editor.isActive("orderedList"));
const isTaskList = computed(() => props.editor.isActive("taskList"));
const isBlockquote = computed(() => props.editor.isActive("blockquote"));
const isCodeBlock = computed(() => props.editor.isActive("codeBlock"));

const toolbarClasses = computed(() => {
    return [
        "flex flex-wrap items-center gap-2 px-2 py-2",
        {"justify-center": props.centered, "sticky z-40 bg-default": props.sticky,}
    ];
});

const toolbarGroupClass = editorToolbarGroupClass
const tableToolbarGroupClass = editorTableToolbarGroupClass
const menuSurfaceClass = editorMenuSurfaceClass
const colorButtonClass = editorColorButtonClass
const selectMenuUi = editorSelectMenuUi
const dropdownMenuUi = editorDropdownMenuUi

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
        <div :class="toolbarGroupClass">
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
                    :content="{sideOffset: 6}"
                    :ui="selectMenuUi"
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

        <div :class="toolbarGroupClass">
            <UTooltip :text="t('editor.toolbar.insertLink')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-link"
                        @click="insertController.openLinkModalFromSelection"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.insertImage')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-image"
                        @click="insertController.openImageModal()"
                />
            </UTooltip>
            <UTooltip :text="t('editor.toolbar.insertTable')">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-table"
                        @click="insertController.openTableInsertModal()"
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

        <div v-if="isInTable" :class="tableToolbarGroupClass">
            <UTooltip v-for="action in primaryToolbarActions"
                       :key="action.key"
                       :text="action.label">
                <UButton
                        :variant="action.disabled ? 'ghost' : 'soft'"
                        :color="action.disabled ? 'neutral' : 'primary'"
                        size="sm"
                        :icon="action.icon"
                        :disabled="action.disabled"
                        class="rounded-lg"
                        @click="action.onSelect"
                />
            </UTooltip>

            <UPopover v-if="isInCell" :content="{sideOffset: 6}">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-palette"
                        :aria-label="t('editor.table.backgroundColor')"
                        class="rounded-md"
                />

                <template #content>
                    <div :class="menuSurfaceClass">
                        <div class="mb-2 flex items-center gap-2 px-1 text-[11px] font-medium text-muted">
                            <UIcon name="i-lucide-palette" class="h-3.5 w-3.5"/>
                            <span>{{ t('editor.table.backgroundColor') }}</span>
                        </div>
                        <div class="grid grid-cols-6 gap-1.5">
                            <UButton
                                    type="button"
                                    class="flex items-center justify-center"
                                    :class="colorButtonClass"
                                    :aria-label="t('editor.table.noColor')"
                                    variant="ghost"
                                    color="neutral"
                                    size="sm"
                                    square
                                    @click="handleTableColorSelect(null)">
                                <span class="text-xs text-muted">×</span>
                            </UButton>
                            <UButton
                                    v-for="color in tableColors"
                                    :key="color.name"
                                    type="button"
                                    :aria-label="color.name"
                                    :class="[colorButtonClass, color.backgroundClass, color.hoverClass]"
                                    variant="ghost"
                                    color="neutral"
                                    size="sm"
                                    square
                                    @click="handleTableColorSelect(color.name)">
                            </UButton>
                        </div>
                    </div>
                </template>
            </UPopover>

            <UDropdownMenu
                    :items="dropdownMenuItems"
                    :content="{align: 'end', sideOffset: 6}"
                    :ui="dropdownMenuUi"
                    size="sm">
                <UButton
                        variant="ghost"
                        color="neutral"
                        size="sm"
                        icon="i-lucide-settings-2"
                        :aria-label="t('editor.table.moreActions')"
                        class="rounded-md"
                />
            </UDropdownMenu>
        </div>

    </div>
</template>
