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

package tech.lamprism.lampray.web.controller.staff.model;

import tech.lamprism.lampray.staff.AttributedStaff;
import tech.lamprism.lampray.staff.StaffType;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author RollW
 */
// TODO: add fields
public record StaffVo(
        String id,
        Long legacyId,
        long userId,
        Set<StaffType> types,
        OffsetDateTime createTime,
        OffsetDateTime updateTime
) {

    public static StaffVo from(AttributedStaff staffInfo) {
        if (staffInfo == null) {
            return null;
        }
        return new StaffVo(
                staffInfo.getResourceId(),
                staffInfo.getStaffId(),
                staffInfo.getUserId(),
                staffInfo.getTypes(),
                staffInfo.getCreateTime(),
                staffInfo.getUpdateTime()
        );
    }
}
