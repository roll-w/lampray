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

package tech.lamprism.lampray.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import space.lingu.NonNull;
import tech.lamprism.lampray.LampException;
import tech.lamprism.lampray.user.Role;
import tech.lamprism.lampray.user.User;
import tech.lamprism.lampray.user.UserOperator;
import tech.lamprism.lampray.user.UserViewException;
import tech.lamprism.lampray.user.filter.UserFilteringInfo;
import tech.lamprism.lampray.user.filter.UserFilteringInfoType;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.ErrorCode;
import tech.rollw.common.web.UserErrorCode;

import java.time.OffsetDateTime;

/**
 * User operator implementation.
 * <p>
 * This implementation is thread-safe.
 *
 * @author RollW
 */
public class UserOperatorImpl implements UserOperator {
    private boolean checkDelete;
    private boolean autoUpdateEnabled;

    private User user;
    private final User.Builder userBuilder;
    private final UserOperatorDelegate delegate;

    private boolean updateFlag = false;

    public UserOperatorImpl(User user, UserOperatorDelegate delegate,
                            boolean checkDelete) {
        this.user = user;
        this.userBuilder = user.toBuilder();
        this.delegate = delegate;
        this.checkDelete = checkDelete;
    }

    @Override
    public void setCheckDeleted(boolean checkDeleted) {
        this.checkDelete = checkDeleted;
    }

    @Override
    public boolean isCheckDeleted() {
        return checkDelete;
    }

    @Override
    public Long getResourceId() {
        return getUserId();
    }

    @Override
    public UserOperator disableAutoUpdate() {
        this.autoUpdateEnabled = false;
        return this;
    }

    @Override
    public UserOperator enableAutoUpdate() {
        this.autoUpdateEnabled = true;
        return this;
    }

    @Override
    public boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }

    @Override
    public UserOperator update() throws CommonRuntimeException {
        if (!updateFlag) {
            return this;
        }
        user = userBuilder
                .setUpdateTime(OffsetDateTime.now())
                .build();
        delegate.updateUser(user);
        updateFlag = false;
        return this;
    }

    @Override
    public UserOperator delete() throws CommonRuntimeException {
        checkDelete();
        userBuilder.setCanceled(true);
        return updateInternal();
    }

    @Override
    public UserOperator rename(String username) throws CommonRuntimeException {
        checkDelete();
        if (username == null) {
            throw new LampException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "username is null");
        }
        if (user.getUsername().equals(username)) {
            return this;
        }
        checkRule(username, UserFilteringInfoType.USERNAME);

        if (delegate.checkUsernameExist(username, user.getUserId())) {
            throw new UserViewException(UserErrorCode.ERROR_USER_EXISTED);
        }

        userBuilder.setUsername(username);
        return updateInternal();
    }

    @Override
    public UserOperator setEmail(String email)
            throws CommonRuntimeException {
        checkDelete();
        if (email == null) {
            throw new LampException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "email is null");
        }
        if (user.getEmail().equals(email)) {
            return this;
        }

        checkRule(email, UserFilteringInfoType.EMAIL);
        if (delegate.checkEmailExist(email, user.getUserId())) {
            throw new UserViewException(UserErrorCode.ERROR_EMAIL_EXISTED);
        }

        userBuilder.setEmail(email);
        return updateInternal();
    }

    @Override
    public UserOperator setRole(Role role) throws CommonRuntimeException {
        checkDelete();
        if (role == null) {
            throw new LampException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "role is null");
        }
        if (user.getRole().equals(role)) {
            return this;
        }

        userBuilder.setRole(role);
        return updateInternal();
    }

    @Override
    public UserOperator setPassword(String password) throws CommonRuntimeException {
        checkDelete();
        if (password == null) {
            throw new LampException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "password is null");
        }
        if (user.getPassword().equals(password)) {
            return this;
        }

        checkRule(password, UserFilteringInfoType.PASSWORD);
        PasswordEncoder encoder = delegate.getPasswordEncoder();
        userBuilder.setPassword(
                encoder.encode(password)
        );
        return updateInternal();
    }

    private void checkRule(String value, UserFilteringInfoType type) {
        UserFilteringInfo info =
                new UserFilteringInfo(value, type);
        ErrorCode errorCode =
                delegate.getUserInfoFilter().filter(info);
        if (errorCode.failed()) {
            throw new UserViewException(errorCode);
        }
    }

    @Override
    public UserOperator setPassword(String oldPassword,
                                    String password)
            throws CommonRuntimeException {
        checkDelete();
        if (oldPassword == null) {
            throw new LampException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "oldPassword is null");
        }
        if (password == null) {
            throw new LampException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "password is null");
        }
        PasswordEncoder encoder = delegate.getPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new UserViewException(UserErrorCode.ERROR_PASSWORD_NOT_CORRECT);
        }
        checkRule(password, UserFilteringInfoType.PASSWORD);
        userBuilder.setPassword(encoder.encode(password));

        return updateInternal();
    }

    @Override
    public UserOperator setEnabled(boolean enabled) throws CommonRuntimeException {
        checkDelete();
        if (user.isEnabled() == enabled) {
            return this;
        }
        userBuilder.setEnabled(enabled);
        return updateInternal();
    }

    @Override
    public UserOperator setLocked(boolean locked) throws CommonRuntimeException {
        checkDelete();
        if (user.isLocked() == locked) {
            return this;
        }
        userBuilder.setLocked(locked);
        return updateInternal();
    }

    @Override
    public UserOperator setCanceled(boolean canceled) throws CommonRuntimeException {
        checkDelete();
        if (user.isCanceled() == canceled) {
            return this;
        }
        userBuilder.setCanceled(canceled);
        return updateInternal();
    }

    @Override
    public UserOperator getSystemResource() {
        return this;
    }

    private void checkDelete() {
        if (!checkDelete) {
            return;
        }
        if (user.isCanceled()) {
            throw new UserViewException(UserErrorCode.ERROR_USER_CANCELED);
        }
    }

    private UserOperator updateInternal() {
        if (!autoUpdateEnabled) {
            updateFlag = true;
            return this;
        }
        user = userBuilder
                .setUpdateTime(OffsetDateTime.now())
                .build();
        delegate.updateUser(user);
        updateFlag = false;
        return this;
    }

    public Long getId() {
        return user.getEntityId();
    }

    @Override
    public long getUserId() {
        return user.getUserId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public Role getRole() {
        return user.getRole();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public boolean isLocked() {
        return user.isLocked();
    }

    @Override
    public boolean isCanceled() {
        return user.isCanceled();
    }

    @NonNull
    @Override
    public OffsetDateTime getCreateTime() {
        return user.getCreateTime();
    }

    @NonNull
    @Override
    public OffsetDateTime getUpdateTime() {
        return user.getUpdateTime();
    }
}
