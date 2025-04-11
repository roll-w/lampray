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

package tech.lamprism.lampray.content.favorite.persistence

import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.rollw.common.web.system.Operator
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class FavoriteGroupRepository(
    private val favoriteGroupDao: FavoriteGroupDao
) : CommonRepository<FavoriteGroupDo, Long>(favoriteGroupDao) {
    fun findByName(name: String, operator: Operator): Optional<FavoriteGroupDo> =
        findOne { root, _, builder ->
            builder.and(
                builder.equal(root.get(FavoriteGroupDo_.name), name),
                builder.equal(root.get(FavoriteGroupDo_.userId), operator.operatorId)
            )
        }

    fun findByUser(userId: Long): List<FavoriteGroupDo> =
        findAll { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get(FavoriteGroupDo_.userId), userId)
            )
        }
}