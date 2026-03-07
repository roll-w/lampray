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

package tech.lamprism.lampray.staff.service;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.LampException;
import tech.lamprism.lampray.staff.StaffInfo;
import tech.lamprism.lampray.staff.StaffService;
import tech.lamprism.lampray.staff.persistence.StaffDo;
import tech.lamprism.lampray.staff.persistence.StaffRepository;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserProvider;
import tech.rollw.common.web.DataErrorCode;

import java.util.Optional;

/**
 * @author RollW
 */
@Service
public class StaffServiceImpl implements StaffService {
    private final StaffRepository staffRepository;
    private final UserProvider userProvider;

    public StaffServiceImpl(StaffRepository staffRepository,
                            UserProvider userProvider) {
        this.staffRepository = staffRepository;
        this.userProvider = userProvider;
    }

    @Override
    public StaffInfo getStaffByUser(long userId) {
        StaffDo staff = staffRepository.findByUserId(userId).orElse(null);
        if (staff == null) {
            throw new LampException(DataErrorCode.ERROR_DATA_NOT_EXIST);
        }
        UserIdentity userIdentity = userProvider.getUser(userId);
        return StaffInfo.from(staff.lock(), userIdentity);
    }

    @Override
    public StaffInfo getStaff(String resourceId) {
        StaffDo staff = staffRepository.findById(resourceId)
                .or(() -> findLegacyStaff(resourceId))
                .orElse(null);
        if (staff == null) {
            throw new LampException(DataErrorCode.ERROR_DATA_NOT_EXIST);
        }
        UserIdentity userIdentity = userProvider.getUser(staff.getUserId());
        return StaffInfo.from(staff.lock(), userIdentity);
    }

    private Optional<StaffDo> findLegacyStaff(String resourceId) {
        if (!resourceId.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }

        try {
            return staffRepository.findByStaffId(Long.parseLong(resourceId));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
