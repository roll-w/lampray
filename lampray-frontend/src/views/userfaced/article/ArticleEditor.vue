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
import StructuralTextEditor from "@/components/structuraltext/StructuralTextEditor.vue";
import {ref} from "vue";
import type {StructuralText} from "@/components/structuraltext/types.ts";
import {StructuralTextType} from "@/components/structuraltext/types.ts";
import {z} from "zod";
import {newErrorToast, newSuccessToast} from "@/utils/toasts.ts";
import {useI18n} from "vue-i18n";
import {articleService} from "@/services/content/article.service.ts";
import {useAxios} from "@/composables/useAxios.ts";

const {t} = useI18n();
const axios = useAxios();
const title = ref<string>("");
const content = ref<StructuralText | undefined>();
const isPublishing = ref(false);
const toast = useToast();

/**
 * Compute total text length of a StructuralText tree by summing `content` lengths.
 * This mirrors server-side StructuralTextValidator's total text length checks.
 */
function computeTotalTextLength(node: StructuralText | undefined): number {
    if (!node) return 0
    let len = 0
    if (node.content.length > 0) {
        len += node.content.length
    }
    if (Array.isArray(node.children) && node.children.length > 0) {
        for (const child of node.children) {
            len += computeTotalTextLength(child)
            if (len > 100000) break // early exit to avoid excessive work
        }
    }
    return len
}

// Create Zod schema at runtime so translations are evaluated with current locale
function createArticlePreSchema(i18nT: (key: string) => string) {
    return z.object({
        title: z.string().min(1, {message: i18nT("article.editor.title.required")}).max(100, {message: i18nT("article.editor.title.tooLong")}),
        content: z.any(),
    }).superRefine((data, ctx) => {
        const value = data.content as StructuralText | undefined
        if (!value) {
            ctx.addIssue({code: "custom" as any, message: i18nT("article.editor.content.empty")})
            return
        }
        if (value.type !== StructuralTextType.DOCUMENT) {
            ctx.addIssue({code: "custom" as any, message: i18nT("article.editor.content.rootMustBeDocument")})
        }
        const total = computeTotalTextLength(value)
        if (total < 1) {
            ctx.addIssue({code: "too_small" as any, minimum: 1, message: i18nT("article.editor.content.tooShort")})
        }
        if (total > 100000) {
            ctx.addIssue({code: "too_big" as any, maximum: 100000, message: i18nT("article.editor.content.tooLong")})
        }
    })
}

const handleChange = (value: StructuralText) => {
    content.value = value
}

const publishArticle = async () => {
    const ArticlePreSchema = createArticlePreSchema(t)

    try {
        ArticlePreSchema.parse({title: title.value, content: content.value})
    } catch (e) {
        if (e instanceof z.ZodError) {
            const messages = e.issues.map(issue => issue.message ?? JSON.stringify(issue)).join(";\n")
            toast.add(newErrorToast(t("request.error.title"), messages))
            return
        }
        toast.add(newErrorToast(t("request.error.title"), t("article.editor.publish.failure.invalidInput")))
        return
    }

    isPublishing.value = true
    try {
        const article = await articleService(axios).createArticle({
            title: title.value,
            content: content.value!,
        })
        console.log(article)
        // TODO: auto redirect to article page after publishing
        toast.add(newSuccessToast(t("request.success.title"), t("article.editor.publish.success")))
    } finally {
        isPublishing.value = false
    }
}

</script>

<template>
    <div class="mx-auto">
        <StructuralTextEditor
                v-model="content"
                :editable="true"
                :placeholder="t('article.editor.placeholder.content')"
                :show-toolbar="true"
                :show-outline="true"
                @change="handleChange"
                :ui="{content: {
                    root: 'w-full md:w-[85vw] lg:w-[50vw] mx-auto overflow-y-auto px-4'
                }, toolbar: {
                    root: 'border-b border-gray-200 dark:border-gray-700 overflow-hidden top-[var(--ui-header-height)]',
                    centered: true
                }, outline:{
                    root: 'top-[calc(var(--ui-header-height)+var(--toolbar-height))]',
                }}"
        >
            <template #toolbar-menu-end>
                <UButton size="sm" variant="ghost" color="primary" @click="publishArticle">
                    {{ $t("common.publish") }}
                </UButton>
            </template>

            <template #before-content>
                <div class="p-4 w-full md:w-[85vw] lg:w-[50vw] mx-auto">
                    <input v-model="title" type="text"
                           :placeholder="t('article.editor.placeholder.title')"
                           class="w-full h-12 text-3xl font-bold outline-none border-none
                           focus:ring-0 focus:outline-none bg-transparent"
                           :disabled="isPublishing"
                    />
                </div>
            </template>
        </StructuralTextEditor>
    </div>
</template>

<style scoped>

</style>