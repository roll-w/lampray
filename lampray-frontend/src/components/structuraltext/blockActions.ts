/*
 * Copyright (C) 2023-2026 RollW
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

import type {ChainedCommands, Editor} from "@tiptap/core"

type Translate = (key: string) => string

export interface BlockTransformAction {
    id: string
    label: string
    description: string
    icon: string
    keywords: string[]
    execute: (chain: ChainedCommands) => ChainedCommands
}

interface BlockSelectionTarget {
    type: "text" | "node"
    from?: number
    to?: number
    pos?: number
}

function createBlockTransformAction(
    id: string,
    label: string,
    description: string,
    icon: string,
    keywords: string[],
    execute: (chain: ChainedCommands) => ChainedCommands,
): BlockTransformAction {
    return {
        id,
        label,
        description,
        icon,
        keywords,
        execute,
    }
}

export function buildBlockTransformActions(t: Translate): BlockTransformAction[] {
    return [
        createBlockTransformAction(
            "paragraph",
            t("editor.toolbar.paragraph"),
            t("editor.slash.description.paragraph"),
            "i-lucide-pilcrow",
            ["paragraph", "text", "normal"],
            chain => chain.setParagraph(),
        ),
        createBlockTransformAction(
            "heading-1",
            t("editor.toolbar.heading1"),
            t("editor.slash.description.heading1"),
            "i-lucide-heading-1",
            ["heading", "title", "h1"],
            chain => chain.setHeading({level: 1}),
        ),
        createBlockTransformAction(
            "heading-2",
            t("editor.toolbar.heading2"),
            t("editor.slash.description.heading2"),
            "i-lucide-heading-2",
            ["heading", "section", "h2"],
            chain => chain.setHeading({level: 2}),
        ),
        createBlockTransformAction(
            "heading-3",
            t("editor.toolbar.heading3"),
            t("editor.slash.description.heading3"),
            "i-lucide-heading-3",
            ["heading", "subsection", "h3"],
            chain => chain.setHeading({level: 3}),
        ),
        createBlockTransformAction(
            "bullet-list",
            t("editor.toolbar.bulletList"),
            t("editor.slash.description.bulletList"),
            "i-lucide-list",
            ["bullet", "list", "unordered"],
            chain => chain.toggleBulletList(),
        ),
        createBlockTransformAction(
            "ordered-list",
            t("editor.toolbar.orderedList"),
            t("editor.slash.description.orderedList"),
            "i-lucide-list-ordered",
            ["numbered", "ordered", "list"],
            chain => chain.toggleOrderedList(),
        ),
        createBlockTransformAction(
            "task-list",
            t("editor.toolbar.taskList"),
            t("editor.slash.description.taskList"),
            "i-lucide-list-checks",
            ["task", "todo", "checklist"],
            chain => chain.toggleTaskList(),
        ),
        createBlockTransformAction(
            "blockquote",
            t("editor.toolbar.blockquote"),
            t("editor.slash.description.blockquote"),
            "i-lucide-quote",
            ["quote", "blockquote", "callout"],
            chain => chain.toggleBlockquote(),
        ),
        createBlockTransformAction(
            "code-block",
            t("editor.toolbar.codeBlock"),
            t("editor.slash.description.codeBlock"),
            "i-lucide-code-2",
            ["code", "snippet", "pre"],
            chain => chain.setCodeBlock(),
        ),
        createBlockTransformAction(
            "divider",
            t("editor.toolbar.horizontalRule"),
            t("editor.slash.description.divider"),
            "i-lucide-minus",
            ["divider", "separator", "rule"],
            chain => chain.setHorizontalRule(),
        ),
    ]
}

function getBlockSelection(editor: Editor, pos: number): BlockSelectionTarget | null {
    const node = editor.state.doc.nodeAt(pos)
    if (!node) {
        return null
    }

    if (!node.isBlock) {
        return null
    }

    if (node.isTextblock) {
        const from = pos + 1
        const to = Math.max(from, pos + node.nodeSize - 1)

        return {
            type: "text" as const,
            from,
            to,
        }
    }

    return {
        type: "node" as const,
        pos,
    }
}

export function canRunBlockTransformAt(editor: Editor, pos: number) {
    return getBlockSelection(editor, pos)?.type === "text"
}

export function runBlockTransformAt(editor: Editor, pos: number, action: BlockTransformAction) {
    const targetSelection = getBlockSelection(editor, pos)
    if (!targetSelection) {
        return false
    }

    if (targetSelection.type !== "text") {
        return false
    }

    const chain = editor.chain().focus()

    return action.execute(chain.setTextSelection({
        from: targetSelection.from,
        to: targetSelection.to,
    })).run()
}
