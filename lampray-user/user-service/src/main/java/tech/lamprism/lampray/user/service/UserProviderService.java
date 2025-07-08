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

package tech.lamprism.lampray.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.User;
import tech.lamprism.lampray.user.UserResourceKind;
import tech.lamprism.lampray.user.UserViewException;
import tech.lamprism.lampray.user.filter.UserInfoFilter;
import tech.lamprism.lampray.user.repository.UserDo;
import tech.lamprism.lampray.user.repository.UserRepository;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.UserErrorCode;
import tech.rollw.common.web.system.SystemResource;
import tech.rollw.common.web.system.SystemResourceKind;
import tech.rollw.common.web.system.SystemResourceOperator;
import tech.rollw.common.web.system.SystemResourceOperatorFactory;
import tech.rollw.common.web.system.SystemResourceProvider;
import tech.rollw.common.web.system.UnsupportedKindException;

import java.util.Objects;

/**
 * @author RollW
 */
@Service
public class UserProviderService implements SystemResourceProvider<Long>,
        SystemResourceOperatorFactory<Long>, UserOperatorDelegate {
    private final UserRepository userRepository;
    private final UserInfoFilter userInfoFilter;
    private final PasswordEncoder passwordEncoder;

    public UserProviderService(UserRepository userRepository,
                               UserInfoFilter userInfoFilter,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userInfoFilter = userInfoFilter;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(@NonNull SystemResourceKind systemResourceKind) {
        return Objects.equals(
                systemResourceKind.getName(),
                UserResourceKind.INSTANCE.getName()
        );
    }

    @NonNull
    @Override
    public SystemResourceOperator<Long> createResourceOperator(
            SystemResource<Long> systemResource, boolean checkDelete) {
        User user = getUser(systemResource.getResourceId())
                .lock();
        return new UserOperatorImpl(user, this, checkDelete);
    }

    @NonNull
    @Override
    public AttributedUser provide(@NonNull SystemResource<Long> rawSystemResource)
            throws CommonRuntimeException, UnsupportedKindException {
        if (rawSystemResource instanceof AttributedUser attributedUser) {
            return attributedUser;
        }
        return getUser(rawSystemResource.getResourceId());
    }

    private UserDo getUser(long id) {
        UserDo user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new UserViewException(UserErrorCode.ERROR_USER_NOT_EXIST);
        }
        return user;
    }

    @Override
    public void updateUser(User user) {
        userRepository.save(UserDo.toDo(user));
    }

    @Override
    public boolean checkUsernameExist(String username, long id) {
        UserDo user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.isCanceled()) {
            return false;
        }
        return user.getUserId() != id;
    }

    @Override
    public boolean checkEmailExist(String email, long id) {
        UserDo user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.isCanceled()) {
            return false;
        }
        return user.getUserId() != id;
    }

    @Override
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    @Override
    public UserInfoFilter getUserInfoFilter() {
        return userInfoFilter;
    }

}
