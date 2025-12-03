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
import {ref, watch} from "vue";

const props = defineProps<NodeViewProps>()
const checked = ref<boolean>(props.node.attrs.checked)

watch(() => props.node.attrs.checked, (newChecked) => {
            if (newChecked !== checked.value) {
                checked.value = newChecked
            }
        }, {immediate: true}
)

const handleToggle = (value: boolean | "indeterminate") => {
    if (!props.editor.isEditable) {
        return
    }
    const newChecked = value === true
    if (newChecked !== checked.value) {
        checked.value = newChecked
        props.updateAttributes({
            checked: newChecked,
        })
    }
}
</script>

<template>
    <NodeViewWrapper
            as="li"
            :data-type="node.type.name"
            :data-checked="node.attrs.checked"
            class="flex items-start gap-2">
        <UCheckbox :model-value="checked"
                   @update:model-value="handleToggle"
                   :disabled="!editor.isEditable"
                   class="mt-1"/>
            <NodeViewContent as="div" class="flex-1"/>
    </NodeViewWrapper>
</template>
