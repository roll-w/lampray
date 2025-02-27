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

package tech.lamprism.lampray.user.details;

import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserTrait;

import java.util.List;

/**
 * @author RollW
 */
public interface UserPersonalDataService {
    UserPersonalData getPersonalData(long userId);

    UserPersonalData getPersonalData(UserIdentity userIdentity);

    List<UserPersonalData> getPersonalData(List<? extends UserIdentity> userIdentities);

    List<UserPersonalData> getPersonalDataByIds(List<Long> ids);

    <T> void updatePersonalData(UserTrait user, UserDataFieldType<T> type, T value);

    void updatePersonalData(UserTrait user, UserDataField<?>... fields);
}
