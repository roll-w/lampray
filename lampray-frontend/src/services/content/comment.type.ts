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
import type {ContentType} from "@/services/content/content.type.ts";

export interface CommentRequest {
    content: StructuralText;
    parent?: number | null;
}

export interface CommentView {
    id: number;
    userId: number;
    parent: number;
    content: StructuralText;
    contentId: number;
    contentType: ContentType;
    createTime: string;
    updateTime: string;
    pending?: boolean;
    failed?: boolean;
}

export interface CommentThreadNode extends CommentView {
    replies: CommentThreadNode[];
    pending?: boolean;
    failed?: boolean;
}

function compareByTimeAndId(left: CommentView, right: CommentView): number {
    const leftTime = new Date(left.createTime).getTime();
    const rightTime = new Date(right.createTime).getTime();

    if (leftTime !== rightTime) {
        return leftTime - rightTime;
    }
    return left.id - right.id;
}

function cloneNode(comment: CommentView): CommentThreadNode {
    return {
        ...comment,
        replies: [],
    };
}

export function normalizeCommentThread(comments: CommentView[]): CommentThreadNode[] {
    if (comments.length === 0) {
        return [];
    }

    const sorted = [...comments].sort(compareByTimeAndId);
    const map = new Map<number, CommentThreadNode>();

    for (const comment of sorted) {
        map.set(comment.id, cloneNode(comment));
    }

    const roots: CommentThreadNode[] = [];
    for (const comment of sorted) {
        const node = map.get(comment.id)!;
        const parentId = comment.parent;
        if (!parentId || parentId <= 0) {
            roots.push(node);
            continue;
        }

        const parent = map.get(parentId);
        if (!parent) {
            roots.push(node);
            continue;
        }
        parent.replies.push(node);
    }

    return roots;
}
