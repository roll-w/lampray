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

package tech.lamprism.lampray.content.comment.persistence

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.content.ContentType

/**
 * @author RollW
 */
@Repository
class CommentRepository(
    private val commentDao: CommentDao
) : CommonRepository<CommentEntity, String>(commentDao) {
    override fun <S : CommentEntity> save(entity: S): S {
        return commentDao.saveAndFlush(entity)
    }

    override fun <S : CommentEntity> saveAll(entities: Iterable<S>): List<S> {
        return commentDao.saveAllAndFlush(entities)
    }

    fun findAllByUserId(userId: Long): List<CommentEntity> {
        return commentDao.findAllByUserId(userId)
    }

    fun findByContent(
        contentId: String,
        contentType: ContentType
    ): List<CommentEntity> {
        return findAll(createContentSpecification(contentId, contentType))
    }

    private fun createContentSpecification(
        contentId: String,
        contentType: ContentType
    ): Specification<CommentEntity> = Specification { root, query, criteriaBuilder ->
        criteriaBuilder.and(
            criteriaBuilder.equal(root.get(CommentEntity_.commentOnId), contentId),
            criteriaBuilder.equal(root.get(CommentEntity_.commentOnType), contentType)
        )
    }
}
