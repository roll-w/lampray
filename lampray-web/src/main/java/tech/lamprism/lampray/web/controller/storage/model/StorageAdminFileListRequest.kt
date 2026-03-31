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

package tech.lamprism.lampray.web.controller.storage.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class StorageAdminFileListRequest(
    @field:Min(1)
    val page: Int = 1,

    @field:Min(1)
    @field:Max(200)
    val size: Int = 20,

    val groupName: String? = null,

    val ownerUserId: Long? = null,

    val fileName: String? = null,
)
