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

const REMOTE_IMAGE_EXTENSIONS = [
    ".png",
    ".jpg",
    ".jpeg",
    ".gif",
    ".webp",
    ".svg",
    ".bmp",
    ".avif",
]

function hasWhitespace(value: string) {
    for (const character of value) {
        if (character.trim() === "") {
            return true
        }
    }

    return false
}

export function parseHttpUrl(value: string) {
    const trimmedValue = value.trim()

    if (!trimmedValue || hasWhitespace(trimmedValue)) {
        return null
    }

    try {
        const parsedUrl = new URL(trimmedValue)
        if (parsedUrl.protocol !== "http:" && parsedUrl.protocol !== "https:") {
            return null
        }

        return parsedUrl
    } catch {
        return null
    }
}

export function isBareHttpUrl(value: string) {
    return parseHttpUrl(value) !== null
}

export function isRemoteImageUrl(value: string) {
    const parsedUrl = parseHttpUrl(value)
    if (!parsedUrl) {
        return false
    }

    const pathname = parsedUrl.pathname.toLowerCase()
    return REMOTE_IMAGE_EXTENSIONS.some(extension => pathname.endsWith(extension))
}

export function getSelectedText(editor: Editor): string {
    const {from, to} = editor.state.selection
    return editor.state.doc.textBetween(from, to, " ")
}

export function getLinkHref(editor: Editor): string {
    return editor.getAttributes("link").href || ""
}

export function isSelectionInEmptyParagraph(editor: Editor) {
    const {selection} = editor.state
    if (!selection.empty) {
        return false
    }

    const parentNode = selection.$from.parent
    return parentNode.type.name === "paragraph" && parentNode.textContent.length === 0 && parentNode.childCount === 0
}

function moveCursorAfterSelection(editor: Editor) {
    const {doc} = editor.state
    const position = editor.state.selection.to
    if (position > doc.content.size) {
        return
    }

    const $position = doc.resolve(position)
    editor.view.dispatch(editor.state.tr.setSelection(TextSelection.near($position, 1)))
}

export function insertLinkedText(editor: Editor, href: string, text: string) {
    const linkText = text.trim() || href

    return editor.chain()
        .focus()
        .insertContent({
            type: "text",
            text: linkText,
            marks: [
                {
                    type: "link",
                    attrs: {href},
                }
            ]
        })
        .run()
}

export function applyLink(editor: Editor, href: string, text?: string) {
    if (!href) {
        return editor.chain().focus().unsetLink().run()
    }

    const parsedUrl = parseHttpUrl(href)
    if (!parsedUrl) {
        return false
    }

    const normalizedHref = parsedUrl.toString()

    const linkMark = editor.schema.marks.link
    if (!linkMark) {
        return false
    }

    const {empty} = editor.state.selection
    const normalizedText = text?.trim()

    if (empty && editor.isActive("link")) {
        return editor.chain()
            .focus()
            .extendMarkRange("link")
            .setLink({href: normalizedHref})
            .command(({tr}) => {
                tr.removeStoredMark(linkMark)
                return true
            })
            .run()
    }

    if (empty) {
        return insertLinkedText(editor, normalizedHref, normalizedText || normalizedHref)
    }

    const hasApplied = editor.chain()
        .focus()
        .setLink({href: normalizedHref})
        .command(({tr}) => {
            tr.removeStoredMark(linkMark)
            return true
        })
        .run()

    if (hasApplied) {
        moveCursorAfterSelection(editor)
    }

    return hasApplied
}

export function unsetLink(editor: Editor) {
    return editor.chain().focus().unsetLink().run()
}

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

    const setLink = (href: string, text?: string) => {
        return applyLink(editor, href, text)
    };

    const removeLink = () => {
        return unsetLink(editor)
    };

    const getCurrentLinkHref = (): string => {
        return getLinkHref(editor)
    };

    const getCurrentSelectedText = (): string => {
        return getSelectedText(editor)
    };

    const copySelectedText = async (): Promise<boolean> => {
        const text = getCurrentSelectedText();
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
        unsetLink: removeLink,
        getLinkHref: getCurrentLinkHref,
        getSelectedText: getCurrentSelectedText,
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
