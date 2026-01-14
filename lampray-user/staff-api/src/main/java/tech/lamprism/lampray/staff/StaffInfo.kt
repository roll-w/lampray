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

package tech.lamprism.lampray.staff

import tech.lamprism.lampray.user.UserIdentity
import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class StaffInfo(
    val id: Long,
    val userIdentity: UserIdentity,
    override val types: Set<StaffType>,
    private val createTime: OffsetDateTime,
    private val updateTime: OffsetDateTime,
    val allowUser: Boolean,
    val deleted: Boolean
) : AttributedStaff {
    override val staffId: Long
        get() = id

    override val userId: Long
        get() = userIdentity.userId

    override fun getCreateTime(): OffsetDateTime = createTime

    override fun getUpdateTime(): OffsetDateTime = updateTime

    companion object {
        @JvmStatic
        fun from(staff: Staff, userIdentity: UserIdentity): StaffInfo {
            return StaffInfo(
                staff.entityId,
                userIdentity,
                staff.types,
                staff.createTime,
                staff.updateTime,
                staff.isAsUser,
                staff.isDeleted
            )
        }

        @JvmStatic
        fun from(userIdentity: UserIdentity): StaffInfo {
            return StaffInfo(
                userIdentity.userId,
                userIdentity,
                emptySet(),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                false,
                false
            )
        }

        @JvmStatic
        fun Staff.toStaffInfo(userIdentity: UserIdentity): StaffInfo {
            return from(this, userIdentity)
        }
    }
}