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
import {computed} from "vue";
import {useI18n} from "vue-i18n";
import type {SettingDetailsValueLayer} from "@/services/system/system.type.ts";

const props = defineProps<{ layers?: SettingDetailsValueLayer[]; activeSource?: string }>();

const {t} = useI18n();

const formatLayerValue = (layer: SettingDetailsValueLayer) => {
    try {
        if (layer.value === null || layer.value === undefined || layer.value === "") {
            return t("views.adminfaced.system.settings.detail.noValue");
        }
        if (typeof layer.value === "string" || typeof layer.value === "number" || typeof layer.value === "boolean") {
            return String(layer.value);
        }
        return JSON.stringify(layer.value, null, 2);
    } catch (e) {
        return String(layer.value);
    }
}

const items = computed(() => {
    const layers = props.layers || [];
    return layers.map((layer, idx) => {
        const isActive = !!props.activeSource && props.activeSource === layer.source;
        const inactiveIndicator = "w-3 h-3 rounded-full border-2 border-gray-300 dark:border-gray-600 bg-transparent transition-colors mt-2";
        const activeIndicator = "w-3 h-3 rounded-full border-2 border-primary bg-transparent mt-2 ring-2 ring-primary transition-colors";

        return ({
            title: layer.source,
            description: formatLayerValue(layer),
            value: idx,
            ui: {
                indicator: isActive ? activeIndicator : inactiveIndicator,
            }
        });
    });
})
</script>

<template>
    <UTimeline :items="items" size="md" class="w-full" :ui="{
      title: 'text-sm font-normal text-neutral-400 dark:text-neutral-500',
      description: 'text-lg text-neutral-600 dark:text-neutral-400 whitespace-pre-wrap break-words',
    }"/>
</template>
