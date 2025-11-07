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

import type {Toast} from "@nuxt/ui/runtime/composables/useToast.d.ts";
import type {HttpResponseBody} from "@/services/common.type.ts";

export const newSuccessToast = (title: string | undefined, message: string | undefined = undefined): Partial<Toast> => {
    return {
        title: title,
        type: "foreground",
        icon: "i-lucide-check-circle",
        color: "success",
        duration: 3000,
        progress: false,
        description: message
    }
}

export const newErrorToast = (title: string | undefined, message: string | undefined = undefined): Partial<Toast> => {
    return {
        title: title,
        type: "foreground",
        icon: "i-lucide-x-circle",
        color: "error",
        duration: 3000,
        progress: false,
        description: message
    }
}

export const newErrorToastFromError = (error: any, defaultTitle: string = "Error"): Partial<Toast> => {
    let title = defaultTitle
    let message: string | undefined = undefined
    if (error.response) {
        const data: HttpResponseBody<any> = error.response.data;
        message = data.tip
    }

    message = message || error.message || String(error)
    return newErrorToast(title, message)
}