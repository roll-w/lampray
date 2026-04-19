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

export const editorFloatingSurfaceClass = "rounded-lg border border-default bg-default shadow-sm"
export const editorCompactFloatingSurfaceClass = "rounded-lg border border-default bg-default p-1.5 shadow-sm"
export const editorMenuSurfaceClass = "rounded-lg border border-default bg-default p-3 shadow-sm"
export const editorToolbarGroupClass = "flex items-center gap-1 rounded-lg border border-default bg-default p-1"
export const editorTableToolbarGroupClass = "flex items-center gap-1 rounded-lg border border-default bg-default p-1"
export const editorColorButtonClass = "h-7 w-7 rounded-md border border-default transition-colors"
export const editorMenuItemClass = "justify-start rounded-md px-2.5 py-1.5"
export const editorSectionLabelClass = "px-2 py-1 text-[11px] font-medium uppercase tracking-[0.08em] text-muted"
export const editorDividerClass = "my-1 h-px bg-default"
export const editorVerticalDividerClass = "h-5 w-px bg-default"
export const editorHandleButtonClass = "rounded-md border border-transparent bg-transparent text-muted shadow-none"
export const editorRoundHandleButtonClass = "rounded-full border border-transparent bg-transparent text-muted shadow-none"

export const editorPopoverUi = {
    content: "rounded-lg border border-default bg-default shadow-sm focus:outline-none"
}

export const editorDropdownMenuUi = {
    content: "min-w-40 rounded-lg border border-default bg-default shadow-sm",
    viewport: "relative divide-y divide-default scroll-py-1 overflow-y-auto flex-1",
    group: "p-1 isolate",
    label: editorSectionLabelClass,
    separator: "-mx-1 my-1 h-px bg-default",
    item: "group relative w-full flex items-start select-none rounded-md text-default outline-none before:absolute before:z-[-1] before:inset-0 before:rounded-md data-disabled:cursor-not-allowed data-disabled:opacity-60 data-highlighted:before:bg-elevated/40 data-[state=open]:before:bg-elevated/40",
    itemLeadingIcon: "shrink-0 text-muted group-data-highlighted:text-default group-data-[state=open]:text-default",
    itemWrapper: "flex-1 min-w-0 text-start",
    itemDescription: "truncate text-muted"
}

export const editorContextMenuUi = {
    content: "min-w-40 rounded-lg border border-default bg-default shadow-sm",
    viewport: "relative divide-y divide-default scroll-py-1 overflow-y-auto flex-1",
    group: "p-1 isolate",
    label: editorSectionLabelClass,
    separator: "-mx-1 my-1 h-px bg-default",
    item: "group relative w-full flex items-start select-none rounded-md text-default outline-none before:absolute before:z-[-1] before:inset-0 before:rounded-md data-disabled:cursor-not-allowed data-disabled:opacity-60 data-highlighted:before:bg-elevated/40 data-[state=open]:before:bg-elevated/40",
    itemLeadingIcon: "shrink-0 text-muted group-data-highlighted:text-default group-data-[state=open]:text-default",
    itemWrapper: "flex-1 min-w-0 text-start",
    itemDescription: "truncate text-muted"
}

export const editorSelectMenuUi = {
    content: "rounded-lg border border-default bg-default shadow-sm",
    viewport: "relative divide-y divide-default scroll-py-1 overflow-y-auto flex-1",
    group: "p-1 isolate",
    input: "border-b border-default",
    label: editorSectionLabelClass,
    separator: "-mx-1 my-1 h-px bg-default",
    item: "data-highlighted:before:bg-elevated/40 data-[state=open]:before:bg-elevated/40"
}

export const editorModalUi = {
    overlay: "bg-black/15 dark:bg-black/45",
    content: "rounded-xl border border-default bg-default shadow-sm ring-0",
    body: "p-0",
    close: "top-4 end-4"
}
