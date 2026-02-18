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

import {useStorage, StorageSerializers} from "@vueuse/core";
import {computed, type Ref} from "vue";
import type {LocalReviewEntry} from "@/views/adminfaced/review/reviewQueueContext.ts";

export interface ReviewDraft {
    jobId: string | number;
    taskId: string | number;
    entries: LocalReviewEntry[];
    summary: string;
    timestamp: number;
}

const DRAFT_STORAGE_KEY = "lampray-review-draft";

export function useReviewDraftStorage() {
    const draftStorage: Ref<ReviewDraft | null> = useStorage(
        DRAFT_STORAGE_KEY,
        null,
        localStorage,
        {serializer: StorageSerializers.object}
    );

    const hasDraft = (jobId: string | number, taskId: string | number): boolean => {
        const draft = draftStorage.value;
        if (!draft) return false;
        return draft.jobId === jobId && draft.taskId === taskId;
    };

    const getDraft = (jobId: string | number, taskId: string | number): ReviewDraft | null => {
        const draft = draftStorage.value;
        if (!draft) return null;
        if (draft.jobId !== jobId || draft.taskId !== taskId) return null;
        return draft;
    };

    const saveDraft = (
        jobId: string | number,
        taskId: string | number,
        entries: LocalReviewEntry[],
        summary: string
    ) => {
        draftStorage.value = {
            jobId,
            taskId,
            entries: [...entries],
            summary,
            timestamp: Date.now()
        };
    };

    const clearDraft = () => {
        draftStorage.value = null;
    };

    const hasAnyDraft = computed(() => draftStorage.value !== null);
    const lastSavedAt = computed(() => draftStorage.value?.timestamp ?? null);

    return {
        draftStorage,
        hasDraft,
        getDraft,
        saveDraft,
        clearDraft,
        hasAnyDraft,
        lastSavedAt
    };
}
