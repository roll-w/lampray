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

package tech.lamprism.lampray.staff.persistence

import org.springframework.stereotype.Repository
import tech.lamprism.lampray.common.data.CommonRepository
import tech.lamprism.lampray.staff.StaffType
import java.util.Optional

/**
 * @author RollW
 */
@Repository
class StaffRepository(
    private val staffDao: StaffDao
) : CommonRepository<StaffDo, Long>(staffDao) {
    fun findByUserId(userId: Long): Optional<StaffDo> {
        return findOne { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get(StaffDo_.userId), userId)
        }
    }

    fun  findByTypes(types: Set<StaffType>): List<StaffDo> {
        return findAll { root, _, criteriaBuilder ->
            criteriaBuilder.and(
                *types.map {
                    criteriaBuilder.isMember(it, root.get(StaffDo_.types))
                }.toTypedArray()
            )
        }
    }
}