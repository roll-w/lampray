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

package tech.lamprism.lampray.staff.service;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.staff.StaffType;
import tech.lamprism.lampray.staff.persistence.StaffDo;
import tech.lamprism.lampray.staff.persistence.StaffRepository;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.Role;
import tech.lamprism.lampray.user.event.NewUserCreatedEvent;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
@Component
public class CreateStaffOnUserCreateListener implements ApplicationListener<NewUserCreatedEvent> {

    private final StaffRepository staffRepository;

    public CreateStaffOnUserCreateListener(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    @Async
    public void onApplicationEvent(@NonNull NewUserCreatedEvent event) {
        AttributedUser attributedUser = event.getUser();
        if (attributedUser.getRole() == Role.USER) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        StaffType type = StaffType.of(attributedUser.getRole());
        StaffDo staff = StaffDo.builder()
                .setUserId(attributedUser.getUserId())
                .setCreateTime(now)
                .setUpdateTime(now)
                .addTypes(type)
                .setDeleted(false)
                .build();
        staffRepository.save(staff);
    }
}
