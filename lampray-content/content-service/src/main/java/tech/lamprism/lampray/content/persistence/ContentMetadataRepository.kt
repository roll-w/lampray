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

package tech.lamprism.lampray.content.persistence

import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.content.ContentTrait
import tech.lamprism.lampray.content.ContentType
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class ContentMetadataRepository(
    private val contentMetadataDao: ContentMetadataDao
) : CommonRepository<ContentMetadataEntity, String>(contentMetadataDao) {
    override fun <S : ContentMetadataEntity> save(entity: S): S {
        return contentMetadataDao.saveAndFlush(entity)
    }

    override fun <S : ContentMetadataEntity> saveAll(entities: Iterable<S>): List<S> {
        return contentMetadataDao.saveAllAndFlush(entities)
    }

    fun findByContent(content: ContentTrait): Optional<ContentMetadataEntity> {
        return findOne { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get<String>("contentId"), content.contentId),
                criteriaBuilder.equal(root.get<ContentType>("contentType"), content.contentType)
            )
        }
    }

    fun findByContents(contents: List<ContentTrait>): List<ContentMetadataEntity> {
        if (contents.isEmpty()) {
            return emptyList()
        }
        return findAll { root, _, builder ->
            val predicates = contents.map {
                builder.and(
                    builder.equal(root.get<String>("contentId"), it.contentId),
                    builder.equal(root.get<ContentType>("contentType"), it.contentType)
                )
            }
            builder.or(*predicates.toTypedArray())
        }
    }
}
