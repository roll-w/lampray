/*
 * Copyright (C) 2023 RollW
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

/**
 * @deprecated use format.js/formatTimestamp instead
 */
export function formatTimestamp(timestamp) {
    let date = new Date(timestamp)
    let Y = date.getFullYear() + '-'
    let M = (date.getMonth() + 1 < 10
        ? '0' + (date.getMonth() + 1)
        : date.getMonth() + 1) + '-'
    let D = date.getDate() < 10
        ? '0' + date.getDate()
        : date.getDate()
    let h = date.getHours() + ':'
    let mm = date.getMinutes() < 10
        ? '0' + date.getMinutes()
        : date.getMinutes()
    let m = mm + ':'
    let s = date.getSeconds() < 10
        ? '0' + date.getSeconds()
        : date.getSeconds()
    return Y + M + D + " " + h + m + s
}