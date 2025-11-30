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
import {NodeViewContent, nodeViewProps, NodeViewWrapper} from "@tiptap/vue-3";

const props = defineProps(nodeViewProps)

const handleCheckboxChange = (checked: boolean | "indeterminate") => {
    props.updateAttributes({
        checked: checked
    })
}
</script>

<template>
    <NodeViewWrapper
            as="li"
            :data-type="node.type.name"
            :data-checked="node.attrs.checked"
            class="flex items-start gap-2 mb-2"
    >
        <UCheckbox
                :model-value="node.attrs.checked"
                @update:model-value="handleCheckboxChange"
                class="mt-1"
        />
        <NodeViewContent
                as="div"
                class="flex-1"
                :class="{ 'line-through text-gray-500 dark:text-gray-400': node.attrs.checked }"
        />
    </NodeViewWrapper>
</template>

