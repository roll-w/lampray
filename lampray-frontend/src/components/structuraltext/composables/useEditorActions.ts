/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type {Editor} from "@tiptap/core";
import {computed} from "vue";
import {TextSelection} from "@tiptap/pm/state";

export function useEditorActions(editor: Editor) {
    const toggleBold = () => {
        editor.chain().focus().toggleBold().run();
    };

    const toggleItalic = () => {
        editor.chain().focus().toggleItalic().run();
    };

    const toggleStrike = () => {
        editor.chain().focus().toggleStrike().run();
    };

    const toggleUnderline = () => {
        editor.chain().focus().toggleUnderline().run();
    };

    const toggleCode = () => {
        editor.chain().focus().toggleCode().run();
    };

    const toggleHighlight = () => {
        editor.chain().focus().toggleMark("highlight").run();
    };

    const isBold = computed(() => editor.isActive("bold"));
    const isItalic = computed(() => editor.isActive("italic"));
    const isStrike = computed(() => editor.isActive("strike"));
    const isUnderline = computed(() => editor.isActive("underline"));
    const isCode = computed(() => editor.isActive("code"));
    const isHighlight = computed(() => editor.isActive("highlight"));
    const isLink = computed(() => editor.isActive("link"));

    const isCodeBlock = computed(() => editor.isActive("codeBlock"));

    const setLink = (href: string) => {
        if (!href) {
            editor.chain().focus().unsetLink().run();
            return;
        }

        const {from, to} = editor.state.selection;
        const linkMark = editor.schema.marks.link;

        if (!linkMark) {
            return;
        }

        if (from === to) {
            // No selection, just set link and ensure cursor moves out
            editor
                .chain()
                .focus()
                .extendMarkRange("link")
                .setLink({href})
                .command(({tr}) => {
                    tr.removeStoredMark(linkMark);
                    return true;
                })
                .run();
        } else {
            // Has selection, apply link to selected text
            editor
                .chain()
                .focus()
                .setLink({href})
                .command(({tr, state}) => {
                    // Move cursor after the link
                    const {doc} = state;
                    const pos = tr.selection.to;
                    if (pos <= doc.content.size) {
                        const $pos = doc.resolve(pos);
                        tr.setSelection(
                            TextSelection.near($pos, 1)
                        );
                    }
                    tr.removeStoredMark(linkMark);
                    return true;
                })
                .run();
        }
    };

    const unsetLink = () => {
        editor.chain().focus().unsetLink().run();
    };

    const getLinkHref = (): string => {
        return editor.getAttributes("link").href || "";
    };

    const getSelectedText = (): string => {
        const {from, to} = editor.state.selection;
        return editor.state.doc.textBetween(from, to, " ");
    };

    const copySelectedText = async (): Promise<boolean> => {
        const text = getSelectedText();
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch (err) {
            console.error("Failed to copy text:", err);
            return false;
        }
    };

    return {
        toggleBold,
        toggleItalic,
        toggleStrike,
        toggleUnderline,
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
        isUnderline,
        isCode,
        isHighlight,
        isLink,
        isCodeBlock,
    };
}

