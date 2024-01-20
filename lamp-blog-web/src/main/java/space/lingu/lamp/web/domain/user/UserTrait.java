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

package space.lingu.lamp.web.domain.user;

import space.lingu.NonNull;
import space.lingu.lamp.web.domain.systembased.LampSystemResourceKind;
import tech.rollw.common.web.system.Operator;
import tech.rollw.common.web.system.SystemResource;
import tech.rollw.common.web.system.SystemResourceKind;

/**
 * @author RollW
 */
public interface UserTrait extends Operator, SystemResource<Long> {
    long getUserId();

    @Override
    default long getOperatorId() {
        return getUserId();
    }

    @Override
    default Long getResourceId() {
        return getUserId();
    }

    @NonNull
    @Override
    default SystemResourceKind getSystemResourceKind() {
        return LampSystemResourceKind.USER;
    }

    static UserTrait of(long userId) {
        return SimpleUserTrait.of(userId);
    }
}
