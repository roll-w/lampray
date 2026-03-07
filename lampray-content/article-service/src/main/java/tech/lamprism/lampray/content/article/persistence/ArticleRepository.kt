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

package tech.lamprism.lampray.content.article.persistence

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class ArticleRepository(
    private val articleDao: ArticleDao
) : CommonRepository<ArticleDo, String>(articleDao) {
    fun findById(id: Long): Optional<ArticleDo> =
        Optional.ofNullable(articleDao.findById(id))

    fun findAllByIdIn(ids: List<Long>): List<ArticleDo> =
        if (ids.isEmpty()) emptyList() else articleDao.findAllByIdIn(ids)

    fun findAllByUserId(userId: Long): List<ArticleDo> {
        return articleDao.findAllByUserId(userId)
    }

    fun findByTitle(title: String, userId: Long): Optional<ArticleDo> {
        return articleDao.findOne(createTitleSpecification(title, userId))
    }

    private fun createTitleSpecification(
        title: String,
        userId: Long
    ): Specification<ArticleDo> {
        return Specification { root, query, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(ArticleDo_.title), title),
                criteriaBuilder.equal(root.get(ArticleDo_.userId), userId)
            )
        }
    }
}
