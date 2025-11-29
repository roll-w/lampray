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

import {Node} from "@tiptap/core";
import {VueNodeViewRenderer} from "@tiptap/vue-3";
import CodeBlockComponent from "@/components/structuraltext/components/CodeBlockComponent.vue";
import {createLowlight} from "lowlight";
import {createLowlightPlugin} from "./CodeBlockLowlightPlugin";

export const lowlight = createLowlight();

const DEFAULT_TAB_SIZE = 4;

export interface CodeBlockOptions {
    /**
     * Define whether the node should be exited on triple enter.
     * @default true
     */
    exitOnTripleEnter: boolean | null | undefined;
    /**
     * Define whether the node should be exited on arrow down if there is no node after it.
     * @default true
     */
    exitOnArrowDown: boolean | null | undefined;
    /**
     * The default language.
     * @default null
     * @example "js"
     */
    defaultLanguage: string | null | undefined;
    /**
     * Enable tab key for indentation in code blocks.
     * @default false
     */
    enableTabIndentation: boolean | null | undefined;
    /**
     * The number of spaces to use for tab indentation.
     * @default 4
     */
    tabSize: number | null | undefined;
    /**
     * Custom HTML attributes that should be added to the rendered HTML tag.
     * @default {}
     * @example { class: "foo" }
     */
    HTMLAttributes: Record<string, any>;
}

declare module "@tiptap/core" {
    interface Commands<ReturnType> {
        codeBlock: {
            /**
             * Set a code block
             * @param attributes Code block attributes
             * @example editor.commands.setCodeBlock({ language: "javascript" })
             */
            setCodeBlock: (attributes?: { language: string }) => ReturnType;
            /**
             * Toggle a code block
             * @param attributes Code block attributes
             * @example editor.commands.toggleCodeBlock({ language: "javascript" })
             */
            toggleCodeBlock: (attributes?: { language: string }) => ReturnType;
        };
    }
}

export const CodeBlock = Node.create(
    {
        name: "codeBlock",
        group: "block",
        content: "text*",
        marks: "bold italic strike underline",
        atom: false,
        code: true,
        defining: true,
        isolating: false,

        addOptions() {
            return {
                exitOnTripleEnter: true,
                exitOnArrowDown: true,
                defaultLanguage: null,
                enableTabIndentation: false,
                tabSize: DEFAULT_TAB_SIZE,
                HTMLAttributes: {},
            };
        },

        addAttributes() {
            return {
                language: {
                    default: this.options.defaultLanguage,
                    parseHTML: element => {
                        const language = element.getAttribute("data-language");
                        if (language !== null) {
                            return language
                        }
                        return "text"
                    },
                    rendered: false,
                },
                showLineNumbers: {
                    default: true,
                    parseHTML: element => {
                        const attr = element.getAttribute("data-show-line-numbers");
                        if (!attr) {
                            return true;
                        }
                        return attr === "true";
                    },
                    renderHTML: attributes => {
                        return {
                            "data-show-line-numbers": attributes.showLineNumbers,
                        };
                    },
                },
                wrapLines: {
                    default: false,
                    parseHTML: element => {
                        return element.getAttribute("data-wrap-lines") === "true";
                    },
                    renderHTML: attributes => {
                        return {
                            "data-wrap-lines": attributes.wrapLines,
                        };
                    },
                },
            };
        },

        parseHTML() {
            return [
                {
                    tag: "pre"
                }
            ];
        },

        renderHTML({node, HTMLAttributes}) {
            return [
                "pre", {
                    "data-type": "codeBlock",
                    "data-language": node.attrs.language || ""
                },
                ["code", {}, node.textContent || ""]
            ];
        },

        markdownTokenName: "code",

        parseMarkdown: (token, helpers) => {
            if (token.raw?.startsWith("```") === false && token.codeBlockStyle !== "indented") {
                return []
            }

            return helpers.createNode(
                "codeBlock",
                {language: token.lang || null},
                token.text ? [helpers.createTextNode(token.text)] : [],
            )
        },

        renderMarkdown: (node, h) => {
            let output = ""
            const language = node.attrs?.language || ""

            if (!node.content) {
                output = `\`\`\`${language}\n\n\`\`\``
            } else {
                const lines = [`\`\`\`${language}`, h.renderChildren(node.content), "```"]
                output = lines.join("\n")
            }

            return output
        },

        addNodeView() {
            return VueNodeViewRenderer(CodeBlockComponent);
        },

        addProseMirrorPlugins() {
            return [createLowlightPlugin(lowlight, this.name)];
        },

        addCommands() {
            return {
                setCodeBlock:
                    attributes =>
                        ({commands}) => {
                            return commands.setNode(this.name, attributes);
                        },
                toggleCodeBlock:
                    attributes =>
                        ({commands}) => {
                            return commands.toggleNode(this.name, "paragraph", attributes);
                        },
            };
        },

        addKeyboardShortcuts() {
            return {
                "Enter": () => {
                    if (!this.editor.isActive(this.name)) {
                        return false;
                    }
                    return this.editor.commands.first(({commands}) => [
                        () => commands.newlineInCode(),
                        () => commands.createParagraphNear(),
                    ]);
                },
                "Shift-Enter": () => {
                    if (!this.editor.isActive(this.name)) {
                        return false;
                    }

                    const {state, view} = this.editor;
                    const {selection} = state;
                    const {$from} = selection;

                    // Find the end of current line
                    const text = $from.parent.textContent;
                    const currentPos = $from.parentOffset;
                    let lineEnd = text.indexOf("\n", currentPos);
                    if (lineEnd === -1) {
                        lineEnd = text.length;
                    }

                    const absoluteLineEnd = $from.start() + lineEnd;

                    // Insert newline at the end of current line
                    const tr = state.tr.insertText("\n", absoluteLineEnd);
                    view.dispatch(tr);

                    return true;
                },
                "ArrowUp": ({editor}) => {
                    const {state} = editor;
                    const {selection, doc} = state;
                    const {$from} = selection;

                    // Check if cursor is at the start of a code block
                    if (this.editor.isActive(this.name) && $from.parentOffset === 0) {
                        const beforePos = $from.before();
                        // Try to move to the node before
                        if (beforePos > 0) {
                            const resolvedPos = doc.resolve(beforePos - 1);
                            editor.commands.setTextSelection(resolvedPos.pos);
                            return true;
                        }
                    }
                    return false;
                },
                "ArrowDown": ({editor}) => {
                    const {state} = editor;
                    const {selection, doc} = state;
                    const {$from} = selection;

                    // Check if cursor is at the end of a code block
                    if (this.editor.isActive(this.name)) {
                        const parent = $from.parent;
                        if ($from.parentOffset === parent.content.size) {
                            const afterPos = $from.after();
                            // Try to move to the node after
                            if (afterPos < doc.content.size) {
                                editor.commands.setTextSelection(afterPos);
                                return true;
                            }
                        }
                    }
                    return false;
                },
                "Mod-a": ({editor}) => {
                    if (!this.editor.isActive(this.name)) {
                        return false;
                    }

                    const {$from} = editor.state.selection;
                    const node = $from.node();
                    if (node.type.name !== this.name) return false;
                    const pos = $from.before();
                    const size = node.nodeSize;

                    editor.commands.setTextSelection({
                        from: pos + 1,
                        to: pos + size - 1,
                    });

                    return true;
                },
                "Tab": () => {
                    if (!this.editor.isActive(this.name) || !this.options.enableTabIndentation) {
                        return false;
                    }
                    const tabSize = this.options.tabSize || DEFAULT_TAB_SIZE;
                    return this.editor.commands.insertContent(" ".repeat(tabSize));
                },
            };
        },
    }
);


