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

import {createLowlight} from "lowlight";
import type {LanguageFn} from "highlight.js";

type LowlightInstance = ReturnType<typeof createLowlight>;

export const supportedLanguages = [
    {value: "plain", label: "Plain Text", ref: "text"},
    {value: "javascript", label: "JavaScript", ref: "javascript"},
    {value: "typescript", label: "TypeScript", ref: "typescript"},
    {value: "python", label: "Python", ref: "python"},
    {value: "java", label: "Java", ref: "java"},
    {value: "cpp", label: "C++", ref: "cpp"},
    {value: "c", label: "C", ref: "c"},
    {value: "csharp", label: "C#", ref: "csharp"},
    {value: "go", label: "Go", ref: "go"},
    {value: "rust", label: "Rust", ref: "rust"},
    {value: "php", label: "PHP", ref: "php"},
    {value: "ruby", label: "Ruby", ref: "ruby"},
    {value: "swift", label: "Swift", ref: "swift"},
    {value: "kotlin", label: "Kotlin"},
    {value: "html", label: "HTML", ref: "xml"},
    {value: "css", label: "CSS", ref: "css"},
    {value: "xml", label: "XML", ref: "xml"},
    {value: "json", label: "JSON", ref: "json"},
    {value: "yaml", label: "YAML", ref: "yaml"},
    {value: "toml", label: "TOML", ref: "toml"},
    {value: "markdown", label: "Markdown", ref: "markdown"},
    {value: "sql", label: "SQL", ref: "sql"},
    {value: "bash", label: "Bash", ref: "bash"},
    {value: "shell", label: "Shell", ref: "shell"},
    {value: "powershell", label: "PowerShell", ref: "powershell"},
];

const languageLoaders: Record<string, () => Promise<{default: LanguageFn}>> = {
    javascript: () => import("highlight.js/lib/languages/javascript"),
    typescript: () => import("highlight.js/lib/languages/typescript"),
    python: () => import("highlight.js/lib/languages/python"),
    java: () => import("highlight.js/lib/languages/java"),
    cpp: () => import("highlight.js/lib/languages/cpp"),
    c: () => import("highlight.js/lib/languages/c"),
    csharp: () => import("highlight.js/lib/languages/csharp"),
    go: () => import("highlight.js/lib/languages/go"),
    rust: () => import("highlight.js/lib/languages/rust"),
    php: () => import("highlight.js/lib/languages/php"),
    ruby: () => import("highlight.js/lib/languages/ruby"),
    swift: () => import("highlight.js/lib/languages/swift"),
    kotlin: () => import("highlight.js/lib/languages/kotlin"),
    xml: () => import("highlight.js/lib/languages/xml"),
    html: () => import("highlight.js/lib/languages/xml"),
    css: () => import("highlight.js/lib/languages/css"),
    json: () => import("highlight.js/lib/languages/json"),
    yaml: () => import("highlight.js/lib/languages/yaml"),
    toml: () => import("highlight.js/lib/languages/ini"),
    markdown: () => import("highlight.js/lib/languages/markdown"),
    sql: () => import("highlight.js/lib/languages/sql"),
    bash: () => import("highlight.js/lib/languages/bash"),
    shell: () => import("highlight.js/lib/languages/shell"),
    powershell: () => import("highlight.js/lib/languages/powershell"),
};

/**
 * Track languages currently being loaded to avoid duplicate requests
 */
const loadingPromises = new Map<string, Promise<boolean>>();

/**
 * Check if a language is registered in the lowlight instance
 */
function isRegistered(lowlight: LowlightInstance, language: string): boolean {
    try {
        return lowlight.listLanguages().includes(language);
    } catch {
        return false;
    }
}

export async function loadLanguage(
    lowlight: LowlightInstance,
    language: string
): Promise<boolean> {
    if (language === "plain" || language === "text") {
        return true;
    }

    // Check if already registered in the lowlight instance
    if (isRegistered(lowlight, language)) {
        return true;
    }

    // Check if currently being loaded
    if (loadingPromises.has(language)) {
        return loadingPromises.get(language)!;
    }

    const loader = languageLoaders[language];
    if (!loader) {
        console.warn(`No loader found for language: ${language}`);
        return false;
    }

    const loadingPromise = (async () => {
        try {
            const module = await loader();
            lowlight.register(language, module.default);
            return true;
        } catch (err) {
            console.error(`Failed to load language ${language}:`, err);
            return false;
        } finally {
            loadingPromises.delete(language);
        }
    })();

    loadingPromises.set(language, loadingPromise);
    return loadingPromise;
}

export function isLanguageLoaded(lowlight: LowlightInstance, language: string): boolean {
    return language === "plain" || language === "text" || isRegistered(lowlight, language);
}

export async function preloadLanguages(
    lowlight: LowlightInstance,
    languages: string[]
): Promise<void> {
    await Promise.all(
        languages.map(lang => loadLanguage(lowlight, lang))
    );
}

export function getSupportedLanguages(): string[] {
    return Object.keys(languageLoaders);
}

