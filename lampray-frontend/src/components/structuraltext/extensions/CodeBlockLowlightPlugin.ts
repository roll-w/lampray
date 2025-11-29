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

import {Plugin, PluginKey} from "@tiptap/pm/state";
import {Decoration, DecorationSet} from "@tiptap/pm/view";
import type {Node as ProseMirrorNode} from "@tiptap/pm/model";
import {createLowlight} from "lowlight";
import {isLanguageLoaded, loadLanguage} from "./LanguageLoader";

type LowlightInstance = ReturnType<typeof createLowlight>;

export function createLowlightPlugin(lowlight: LowlightInstance, nodeTypeName: string) {
    const pluginKey = new PluginKey("lowlight");

    return new Plugin({
        key: pluginKey,
        state: {
            init: (_, {doc}) => {
                preloadLanguagesInDocument(doc, lowlight, nodeTypeName);
                return getDecorations(doc, lowlight, nodeTypeName);
            },
            apply: (transaction, oldState) => {
                if (!transaction.docChanged) {
                    return oldState;
                }
                preloadLanguagesInDocument(transaction.doc, lowlight, nodeTypeName);
                return getDecorations(transaction.doc, lowlight, nodeTypeName);
            },
        },
        props: {
            decorations(state) {
                return pluginKey.getState(state);
            },
        },
    });
}

/**
 * Preload languages found in the document
 */
function preloadLanguagesInDocument(
    doc: ProseMirrorNode,
    lowlight: LowlightInstance,
    nodeTypeName: string
): void {
    const languagesToLoad = new Set<string>();

    doc.descendants((node) => {
        if (node.type.name === nodeTypeName) {
            const language = node.attrs.language || "plain";
            if (language !== "plain" && !isLanguageLoaded(lowlight, language)) {
                languagesToLoad.add(language);
            }
        }
    });
    // Load all required languages in parallel
    languagesToLoad.forEach(language => {
        loadLanguage(lowlight, language).catch(err => {
            console.warn(`Failed to preload language ${language}:`, err);
        });
    });
}

function getDecorations(
    doc: ProseMirrorNode,
    lowlight: LowlightInstance,
    nodeTypeName: string
): DecorationSet {
    const decorations: Decoration[] = [];

    doc.descendants((node, pos) => {
        if (node.type.name !== nodeTypeName) {
            return;
        }

        const language = node.attrs.language || "plain";
        if (language === "plain" || language === "text") {
            return;
        }

        // Skip if language is not loaded yet
        if (!isLanguageLoaded(lowlight, language)) {
            return;
        }

        const text = node.textContent;
        if (!text) {
            return;
        }

        try {
            const result = lowlight.highlight(language, text);
            const offset = pos + 1; // +1 for the opening tag

            flattenHighlightTree(result.children, offset, decorations);
        } catch (err) {
            console.warn(`Failed to highlight code with language ${language}:`, err);
        }
    });

    return DecorationSet.create(doc, decorations);
}

function flattenHighlightTree(
    nodes: any[],
    offset: number,
    decorations: Decoration[],
    inheritedClasses: string[] = []
): number {
    let currentOffset = offset;

    for (const node of nodes) {
        if (node.type === "text") {
            const length = node.value.length;
            if (inheritedClasses.length > 0) {
                decorations.push(
                    Decoration.inline(currentOffset, currentOffset + length, {
                        class: inheritedClasses.join(" "),
                    })
                );
            }
            currentOffset += length;
        } else if (node.type === "element") {
            const classes = node.properties?.className || [];
            const classList = Array.isArray(classes) ? classes : [classes];
            const allClasses = [...inheritedClasses, ...classList];

            if (node.children && node.children.length > 0) {
                currentOffset = flattenHighlightTree(
                    node.children,
                    currentOffset,
                    decorations,
                    allClasses
                );
            }
        }
    }

    return currentOffset;
}

