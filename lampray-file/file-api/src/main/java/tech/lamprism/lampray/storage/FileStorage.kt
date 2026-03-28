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

package tech.lamprism.lampray.storage

import tech.lamprism.lampray.DataEntity
import tech.rollw.common.web.system.SystemResourceKind
import java.time.OffsetDateTime

/**
 * Metadata for a file managed by the storage module.
 *
 * @author RollW
 */
data class FileStorage(
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    override val createTime: OffsetDateTime,
) : DataEntity<String> {
    override fun getEntityId(): String = fileId

    override fun getUpdateTime(): OffsetDateTime = createTime

    override fun getSystemResourceKind(): SystemResourceKind = StorageResourceKind
}
