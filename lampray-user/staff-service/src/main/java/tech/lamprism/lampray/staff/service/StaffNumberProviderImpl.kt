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

package tech.lamprism.lampray.staff.service

import org.springframework.stereotype.Service
import tech.lamprism.lampray.staff.Staff
import tech.lamprism.lampray.staff.StaffNumberProvider
import tech.lamprism.lampray.staff.StaffType
import tech.rollw.common.web.CommonErrorCode
import tech.rollw.common.web.system.SystemResourceException

/**
 * @author RollW
 */
@Service
class StaffNumberProviderImpl : StaffNumberProvider {
    override fun getStaffNumber(staff: Staff): String {
        if (staff.entityId == null) {
            throw SystemResourceException(CommonErrorCode.ERROR_NULL, "Staff id is null.")
        }
        val type = staff.types.firstOrNull() ?: StaffType.UNASSIGNED
        val idDigitFormat = "%08d".format(staff.entityId)

        return "${prefixOf(type)}${idDigitFormat}"
    }

    private fun prefixOf(staffType: StaffType): String {
        return when (staffType) {
            StaffType.ADMIN -> "AM"
            StaffType.REVIEWER -> "RV"
            StaffType.EDITOR -> "ET"
            StaffType.UNASSIGNED -> "UA"
            StaffType.CUSTOMER_SERVICE -> "CS"
        }
    }
}