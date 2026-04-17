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
import {computed, ref, watch} from "vue";
import type {Editor} from "@tiptap/core";
import {DragHandle} from "@tiptap/extension-drag-handle-vue-3";
import {useI18n} from "vue-i18n";
import {useStructuralTextInsertController} from "@/components/structuraltext/composables/useStructuralTextInsertController";
import {buildBlockTransformActions, canRunBlockTransformAt, runBlockTransformAt} from "@/components/structuraltext/blockActions";

interface BlockMenuAction {
    key: string
    label: string
    icon: string
    color?: "neutral" | "primary" | "error"
    disabled?: boolean
    onSelect: () => void
}

const props = defineProps<{
    editor: Editor | null;
}>();

const emit = defineEmits<{
    (e: "nodeChange", selectedNode: { node: unknown; pos: number }): void;
}>();

const {t} = useI18n()
const insertController = useStructuralTextInsertController()
const currentNodePos = ref<number | null>(null)
const pinnedNodePos = ref<number | null>(null)
const menuOpen = ref(false)

const activeNodePos = computed(() => menuOpen.value ? pinnedNodePos.value : currentNodePos.value)

function selectBlockAt(pos: number | null) {
    if (!props.editor || pos == null) {
        return null
    }

    const node = props.editor.state.doc.nodeAt(pos)
    if (!node) {
        return null
    }

    const selectedNode = {node: node.toJSON(), pos}
    emit("nodeChange", selectedNode)
    props.editor.chain().focus().setNodeSelection(pos).run()
    return selectedNode
}

function onNodeChange({pos}: { pos: number }) {
    currentNodePos.value = pos >= 0 ? pos : null
}

function handleHandleClick() {
    selectBlockAt(activeNodePos.value ?? currentNodePos.value)
}

function closeMenu() {
    menuOpen.value = false
}

function runMenuAction(action: BlockMenuAction) {
    if (action.disabled) {
        return
    }

    action.onSelect()
    closeMenu()
}

function getActiveNode() {
    if (!props.editor || activeNodePos.value == null) {
        return null
    }

    const node = props.editor.state.doc.nodeAt(activeNodePos.value)
    if (!node) {
        return null
    }

    return {
        node,
        pos: activeNodePos.value,
    }
}

function replaceActiveBlockWithHorizontalRule() {
    if (!props.editor || activeNodePos.value == null) {
        return false
    }

    const activeNode = getActiveNode()
    const horizontalRuleNode = props.editor.schema.nodes.horizontalRule
    if (!activeNode || !activeNode.node.isTextblock || !horizontalRuleNode) {
        return false
    }

    return props.editor.chain().focus().insertContentAt({
        from: activeNode.pos,
        to: activeNode.pos + activeNode.node.nodeSize,
    }, {
        type: horizontalRuleNode.name,
    }).run()
}

function duplicateBlock() {
    if (!props.editor) {
        return false
    }

    const activeNode = getActiveNode()
    if (!activeNode) {
        return false
    }

    return props.editor.chain().focus().insertContentAt(activeNode.pos + activeNode.node.nodeSize, activeNode.node.toJSON()).run()
}

function deleteBlock() {
    if (!props.editor) {
        return false
    }

    const activeNode = getActiveNode()
    if (!activeNode) {
        return false
    }

    return props.editor.chain().focus().setNodeSelection(activeNode.pos).deleteSelection().run()
}

const transformActions = computed(() => {
    if (!props.editor || activeNodePos.value == null || !canRunBlockTransformAt(props.editor, activeNodePos.value)) {
        return [] satisfies BlockMenuAction[]
    }

    return buildBlockTransformActions(t).map(action => ({
        key: action.id,
        label: action.label,
        icon: action.icon,
        onSelect: () => {
            if (!props.editor || activeNodePos.value == null) {
                return
            }

            if (action.id === "divider") {
                replaceActiveBlockWithHorizontalRule()
                return
            }

            runBlockTransformAt(props.editor, activeNodePos.value, action)
        }
    })) satisfies BlockMenuAction[]
})

const isInsertBelowSupported = computed(() => {
    const activeNode = getActiveNode()
    if (!activeNode) {
        return false
    }

    return activeNode.node.type.name !== "listItem" && activeNode.node.type.name !== "taskItem"
})

const insertBelowActions = computed(() => {
    return [
        {
            key: "insert-table-below",
            label: t("editor.blockMenu.insertTable"),
            icon: "i-lucide-table",
            disabled: !isInsertBelowSupported.value,
            onSelect: () => {
                if (activeNodePos.value != null && isInsertBelowSupported.value) {
                    insertController.openTableInsertModalBelow(activeNodePos.value)
                }
            }
        },
        {
            key: "insert-image-below",
            label: t("editor.blockMenu.insertImage"),
            icon: "i-lucide-image",
            disabled: !isInsertBelowSupported.value,
            onSelect: () => {
                if (activeNodePos.value != null && isInsertBelowSupported.value) {
                    insertController.openImageModalBelow(activeNodePos.value)
                }
            }
        },
        {
            key: "insert-link-below",
            label: t("editor.blockMenu.insertLink"),
            icon: "i-lucide-link",
            disabled: !isInsertBelowSupported.value,
            onSelect: () => {
                if (activeNodePos.value != null && isInsertBelowSupported.value) {
                    insertController.openLinkModalBelow(activeNodePos.value)
                }
            }
        }
    ] satisfies BlockMenuAction[]
})

const utilityActions = computed(() => {
    return [
        {
            key: "duplicate-block",
            label: t("editor.blockMenu.duplicateBlock"),
            icon: "i-lucide-copy",
            onSelect: duplicateBlock,
        },
        {
            key: "delete-block",
            label: t("editor.blockMenu.deleteBlock"),
            icon: "i-lucide-trash-2",
            color: "error",
            onSelect: deleteBlock,
        }
    ] satisfies BlockMenuAction[]
})

watch(menuOpen, isOpen => {
    if (isOpen) {
        pinnedNodePos.value = currentNodePos.value
        selectBlockAt(pinnedNodePos.value)
        return
    }

    pinnedNodePos.value = null
})
</script>

<template>
    <DragHandle v-if="editor"
                :editor="editor"
                :on-node-change="onNodeChange"
                :nested="true"
                class="hidden sm:flex items-center justify-center pe-4 transition-all duration-200 ease-out"
    >
        <UPopover v-model:open="menuOpen">
            <UButton
                    type="button"
                    variant="ghost"
                    color="neutral"
                    size="sm"
                    square
                    class="cursor-grab rounded-full"
                    icon="i-lucide-grip-vertical"
                    :aria-label="t('editor.blockMenu.open')"
                    @click.stop="handleHandleClick"
            />

            <template #content>
                <div class="w-60 p-1.5">
                    <template v-if="transformActions.length > 0">
                        <div class="px-2 py-1 text-[11px] font-medium text-muted">
                            {{ t('editor.blockMenu.turnInto') }}
                        </div>
                        <div class="grid gap-0.5 p-1">
                            <UButton
                                    v-for="action in transformActions"
                                    :key="action.key"
                                 color="neutral"
                                 variant="ghost"
                                 size="xs"
                                 :icon="action.icon"
                                 :disabled="action.disabled"
                                 class="justify-start rounded-md px-2"
                                 @click="runMenuAction(action)"
                         >
                                {{ action.label }}
                            </UButton>
                        </div>

                        <div class="my-1 h-px bg-default"/>
                    </template>

                    <div class="px-2 py-1 text-[11px] font-medium text-muted">
                        {{ t('editor.blockMenu.insertBelow') }}
                    </div>
                    <div class="grid gap-0.5 p-1">
                        <UButton
                                v-for="action in insertBelowActions"
                                :key="action.key"
                                color="neutral"
                                variant="ghost"
                                size="xs"
                                :icon="action.icon"
                                :disabled="action.disabled"
                                class="justify-start rounded-md px-2"
                                @click="runMenuAction(action)"
                        >
                            {{ action.label }}
                        </UButton>
                    </div>

                    <div class="my-1 h-px bg-default"/>

                    <div class="grid gap-0.5 p-1">
                        <UButton
                                v-for="action in utilityActions"
                                :key="action.key"
                                :color="action.color || 'neutral'"
                                variant="ghost"
                                size="xs"
                                :icon="action.icon"
                                class="justify-start rounded-md px-2"
                                @click="runMenuAction(action)"
                        >
                            {{ action.label }}
                        </UButton>
                    </div>
                </div>
            </template>
        </UPopover>
    </DragHandle>
</template>
