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

package tech.lamprism.lampray.content.persistence

import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.content.ContentTrait
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class ContentMetadataRepository(
    private val contentMetadataDao: ContentMetadataDao
) : CommonRepository<ContentMetadataDo, Long>(contentMetadataDao) {
    fun findByContent(content: ContentTrait): Optional<ContentMetadataDo> {
        return contentMetadataDao.findByContent(content.contentId, content.contentType)
    }
}