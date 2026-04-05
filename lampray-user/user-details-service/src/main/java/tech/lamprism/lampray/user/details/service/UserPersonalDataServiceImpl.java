/*
 * Copyright (C) 2023-2026 RollW
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

package tech.lamprism.lampray.user.details.service;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.user.UserTrait;
import tech.lamprism.lampray.user.details.UserDataField;
import tech.lamprism.lampray.user.details.UserDataFieldType;
import tech.lamprism.lampray.user.details.UserPersonalData;
import tech.lamprism.lampray.user.details.UserPersonalDataService;
import tech.lamprism.lampray.user.details.persistence.UserPersonalDataEntity;
import tech.lamprism.lampray.user.details.persistence.UserPersonalDataRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author RollW
 */
@Service
public class UserPersonalDataServiceImpl implements UserPersonalDataService {
    private final UserProvider userProvider;
    private final UserPersonalDataRepository userPersonalDataRepository;

    public UserPersonalDataServiceImpl(UserProvider userProvider,
                                       UserPersonalDataRepository userPersonalDataRepository) {
        this.userProvider = userProvider;
        this.userPersonalDataRepository = userPersonalDataRepository;
    }

    @Override
    public UserPersonalData getPersonalData(long userId) {
        UserPersonalDataEntity data = userPersonalDataRepository.findById(userId).orElse(null);
        if (data == null) {
            AttributedUser user = userProvider.getUser(userId);
            return UserPersonalData.defaultOf(user);
        }
        UserPersonalData locked = data.lock();
        if (UserPersonalData.checkNecessaryFields(locked)) {
            return locked;
        }
        AttributedUser user = userProvider.getUser(userId);
        return UserPersonalData.replaceWithDefault(user, locked);
    }

    @Override
    public UserPersonalData getPersonalData(UserIdentity userIdentity) {
        UserPersonalDataEntity data = userPersonalDataRepository.findById(userIdentity.getUserId()).orElse(null);
        if (data == null) {
            return UserPersonalData.defaultOf(userIdentity);
        }
        UserPersonalData locked = data.lock();
        if (UserPersonalData.checkNecessaryFields(locked)) {
            return locked;
        }
        return UserPersonalData.replaceWithDefault(userIdentity, locked);
    }

    @Override
    public List<UserPersonalData> getPersonalData(List<? extends UserIdentity> userIdentities) {
        List<Long> ids = userIdentities.stream().map(UserIdentity::getUserId)
                .toList();
        List<UserPersonalData> userPersonalData = getPersonalDataByIds(ids);
        return userPersonalData.stream().map(data -> {
            if (UserPersonalData.checkNecessaryFields(data)) {
                return data;
            }
            return UserPersonalData.replaceWithDefault(
                    userIdentities.stream()
                            .filter(identity -> identity.getUserId() == data.getUserId())
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Get personal data error")),
                    data
            );
        }).toList();
    }

    @Override
    public List<UserPersonalData> getPersonalDataByIds(List<Long> ids) {
        return userPersonalDataRepository.findAllById(ids)
                .stream()
                .map(UserPersonalDataEntity::lock)
                .toList();
    }

    @Override
    public <T> void updatePersonalData(UserTrait user, UserDataFieldType<T> type,
                                       T value) {
        updatePersonalData(user, new UserDataField<>(type, value));
    }

    @Override
    public void updatePersonalData(UserTrait user, UserDataField<?>... fields) {
        if (fields.length == 0) {
            return;
        }
        UserPersonalDataEntity exist = userPersonalDataRepository.findById(user.getUserId()).orElse(null);
        UserPersonalDataEntity.Builder builder = toBuilder(exist, user.getUserId());
        for (UserDataField<?> field : fields) {
            Utils.setBuilderValue(builder, field);
        }
        builder.setUpdateTime(OffsetDateTime.now());
        userPersonalDataRepository.save(builder.build());
    }

    private UserPersonalDataEntity.Builder toBuilder(UserPersonalDataEntity data, long userId) {
        if (data != null) {
            return data.toBuilder();
        }
        return UserPersonalDataEntity.builder()
                .setUserId(userId);
    }
}
