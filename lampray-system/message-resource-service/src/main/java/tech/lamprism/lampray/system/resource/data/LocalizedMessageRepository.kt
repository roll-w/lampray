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

package tech.lamprism.lampray.system.resource.data

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.rollw.common.web.page.Page
import java.util.Locale

/**
 * @author RollW
 */
@Repository
class LocalizedMessageRepository(
    private val localizedMessageDao: LocalizedMessageDao
) : CommonRepository<LocalizedMessageEntity, String>(localizedMessageDao) {
    override fun <S : LocalizedMessageEntity> save(entity: S): S {
        return localizedMessageDao.saveAndFlush(entity)
    }

    fun findByKey(key: String, locale: Locale) =
        findOne { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get<String>("key"), key),
                criteriaBuilder.equal(root.get<Locale>("locale"), locale)
            )
        }

    fun findByKey(
        key: String,
        pageable: Pageable = Pageable.unpaged()
    ): Page<LocalizedMessageEntity> =
        findAll(pageable) { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get<String>("key"), key)
        }

    fun deleteByKey(key: String) {
        delete { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get<String>("key"), key)
            )
        }
    }

    fun deleteByKey(key: String, locale: Locale) {
        delete { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get<String>("key"), key),
                criteriaBuilder.equal(root.get<Locale>("locale"), locale)
            )
        }
    }
}
