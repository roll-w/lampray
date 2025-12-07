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
import {defineProps, ref} from "vue";
import type {Editor} from "@tiptap/core";
import {DragHandle} from "@tiptap/extension-drag-handle-vue-3";

const props = defineProps<{
    editor: Editor | null;
}>();

const emit = defineEmits<{
    (e: "nodeChange", selectedNode: { node: any; pos: number }): void;
}>();


const currentNodePos = ref<number | null>()

function onNodeChange({ pos }: { pos: number }) {
    currentNodePos.value = pos
}

function onClick() {
    if (!props.editor) {
        return
    }

    const pos = currentNodePos.value
    if (pos == null) return

    const node = props.editor.state.doc.nodeAt(pos)
    if (node) {
        const selectedNode = { node: node.toJSON(), pos }
        emit("nodeChange", selectedNode)
        props.editor.chain().setNodeSelection(pos).run()
        return selectedNode
    }
}

</script>

<template>
    <DragHandle v-if="editor"
                :editor="editor"
                :on-node-change="onNodeChange"
                @click="onClick"
                class="hidden sm:flex items-center justify-center transition-all duration-200 ease-out"
    >
        <UButton
                type="button"
                variant="ghost"
                color="neutral"
                size="sm"
                class="cursor-grab px-1"
                icon="i-lucide-grip-vertical"
        />
    </DragHandle>
</template>

<style scoped>

</style>