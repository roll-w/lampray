/*
 * Copyright (C) 2022 Lingu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package space.lingu.lamp.web.event.user;

import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.lingu.NonNull;

/**
 * @author RollW
 */
@Component
public class RegistrationListener implements ApplicationListener<OnUserRegistrationEvent> {

    @Override
    public void onApplicationEvent(@NonNull OnUserRegistrationEvent event) {
        handleRegistration(event);
    }

    @Async
    void handleRegistration(OnUserRegistrationEvent event) {
        // TODO: handle registration
    }
}
