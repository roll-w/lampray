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

import type {StructuralText} from "@/components/structuraltext/types.ts";

export function extractPlainText(node: StructuralText | null | undefined): string {
    if (!node || typeof node !== "object") {
        return "";
    }

    const chunks: string[] = [];
    const walk = (current: StructuralText | null | undefined): void => {
        if (!current || typeof current !== "object") {
            return;
        }
        const content = typeof current.content === "string" ? current.content : "";
        if (content.length > 0) {
            chunks.push(content);
        }

        if (!Array.isArray(current.children) || current.children.length === 0) {
            return;
        }

        for (const child of current.children) {
            walk(child);
        }
    };

    walk(node);
    return chunks.join(" ").trim();
}
