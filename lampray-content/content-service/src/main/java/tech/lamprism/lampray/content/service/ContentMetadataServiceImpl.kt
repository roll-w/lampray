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

package tech.lamprism.lampray.content.service

import org.springframework.stereotype.Service
import tech.lamprism.lampray.content.ContentMetadata
import tech.lamprism.lampray.content.ContentTrait
import tech.lamprism.lampray.content.common.ContentErrorCode
import tech.lamprism.lampray.content.common.ContentException
import tech.lamprism.lampray.content.persistence.ContentMetadataEntity.Companion.toEntity
import tech.lamprism.lampray.content.persistence.ContentMetadataRepository

/**
 * @author RollW
 */
@Service
class ContentMetadataServiceImpl(
    private val contentMetadataRepository: ContentMetadataRepository
) : ContentMetadataService {

    override fun getMetadata(contentTrait: ContentTrait): ContentMetadata {
        return contentMetadataRepository.findByContent(
            contentTrait
        ).orElseThrow {
            ContentException(
                ContentErrorCode.ERROR_CONTENT_NOT_FOUND,
                "Content metadata not found"
            )
        }.lock()
    }

    fun createMetadata(metadata: ContentMetadata) {
        contentMetadataRepository.save(metadata.toEntity())
    }

    override fun updateMetadata(metadata: ContentMetadata) {
        contentMetadataRepository.save(metadata.toEntity())
    }

    fun deleteMetadata(contentTrait: ContentTrait) {
    }
}
