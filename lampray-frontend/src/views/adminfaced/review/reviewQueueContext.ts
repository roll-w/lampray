import {computed, inject, provide, type Ref} from "vue";
import type {ContentLocationRange} from "@/components/structuraltext/types.ts";
import type {
    ReviewFeedbackEntry,
    ReviewJobContentView,
    ReviewJobView,
    ReviewTaskView,
    ReviewVerdict
} from "@/services/content/review.type.ts";

export interface LocalReviewEntry extends ReviewFeedbackEntry {
    originalText?: string;
}

export interface ReviewEntryDraft {
    text?: string;
    location?: ContentLocationRange;
}

export interface ReviewSelection {
    range: ContentLocationRange;
    text: string;
}

interface ReviewQueueState {
    job: Ref<ReviewJobView | null>;
    jobContent: Ref<ReviewJobContentView | null>;
    task: Ref<ReviewTaskView | null>;
    entries: Ref<LocalReviewEntry[]>;
    draft: Ref<ReviewEntryDraft | null>;
    summary: Ref<string>;
    selectedEntry: Ref<ReviewFeedbackEntry | null>;
    progress: Ref<number>;
    isFirst: Ref<boolean>;
    isLast: Ref<boolean>;
    disabled: Ref<boolean>;
    submitting: Ref<boolean>;
}

interface ReviewQueueActions {
    setDraftFromSelection: (selection: ReviewSelection) => void;
    clearDraft: () => void;
    selectEntry: (entry: ReviewFeedbackEntry | null) => void;
    toggleEntrySelection: (entry: ReviewFeedbackEntry) => void;
    clearSelection: () => void;
    submitReview: (verdict: ReviewVerdict) => Promise<void>;
    prevJob: () => Promise<void>;
    nextJob: () => Promise<void>;
}

export interface ReviewQueueContext {
    state: ReviewQueueState;
    actions: ReviewQueueActions;
}

const REVIEW_QUEUE_CONTEXT_KEY = Symbol("review-queue-context");

export const provideReviewQueueContext = (context: ReviewQueueContext) => {
    provide(REVIEW_QUEUE_CONTEXT_KEY, context);
};

export const useReviewQueueContext = () => {
    const context = inject<ReviewQueueContext>(REVIEW_QUEUE_CONTEXT_KEY);
    if (!context) {
        throw new Error("ReviewQueueContext is not provided.");
    }
    return context;
};

export const useReviewQueueState = () => useReviewQueueContext().state;
export const useReviewQueueActions = () => useReviewQueueContext().actions;

export const useReviewQueueSummary = () => {
    const {summary} = useReviewQueueState();
    return computed({
        get: () => summary.value,
        set: (value: string) => {
            summary.value = value;
        }
    });
};

export const useReviewQueueDraft = () => {
    const {draft} = useReviewQueueState();
    return computed({
        get: () => draft.value,
        set: (value: ReviewEntryDraft | null) => {
            draft.value = value;
        }
    });
};
