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

package tech.lamprism.lampray.user.event;

import org.springframework.context.ApplicationEvent;
import space.lingu.NonNull;
import tech.lamprism.lampray.user.AttributedUser;

/**
 * Only published when a new user is created. Including user creation by admin
 * and self-registration.
 *
 * @author RollW
 */
public class NewUserCreatedEvent extends ApplicationEvent {
    @NonNull
    private final AttributedUser user;

    public NewUserCreatedEvent(@NonNull AttributedUser user) {
        super(user);
        this.user = user;
    }

    @NonNull
    public AttributedUser getUser() {
        return user;
    }
}
