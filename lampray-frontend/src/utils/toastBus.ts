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

import type {Toast} from "@nuxt/ui/runtime/composables/useToast.d.ts";

type ToastHandler = (payload: Partial<Toast>) => void

let toastHandler: ToastHandler | null = null

export const registerToastHandler = (handler: ToastHandler): void => {
    toastHandler = handler
}

export const clearToastHandler = (handler?: ToastHandler): void => {
    if (!handler || toastHandler === handler) {
        toastHandler = null
    }
}

export const pushToast = (payload: Partial<Toast>): void => {
    toastHandler?.(payload)
}
