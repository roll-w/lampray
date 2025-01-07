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

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.event.OnUserRoleChangeEvent;

/**
 * @author RollW
 */
@Component
public class UserRoleChangeListener implements ApplicationListener<OnUserRoleChangeEvent> {

    public UserRoleChangeListener() {
    }

    @Override
    @Async
    public void onApplicationEvent(@NonNull OnUserRoleChangeEvent event) {
        AttributedUser user = event.getUser();
        // TODO: update staff
    }
}
