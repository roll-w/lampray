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

package tech.lamprism.lampray.web.controller.user;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tech.lamprism.lampray.storage.StorageUrlProvider;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.lamprism.lampray.user.UserIdentity;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.user.UserSearchService;
import tech.lamprism.lampray.user.UserViewException;
import tech.lamprism.lampray.user.details.UserPersonalData;
import tech.lamprism.lampray.user.details.UserPersonalDataService;
import tech.lamprism.lampray.web.common.ApiContext;
import tech.lamprism.lampray.web.common.ParamValidate;
import tech.lamprism.lampray.web.controller.Api;
import tech.lamprism.lampray.web.controller.user.model.UserCommonDetailsVo;
import tech.rollw.common.web.AuthErrorCode;
import tech.rollw.common.web.HttpResponseEntity;
import tech.rollw.common.web.system.ContextThreadAware;

import java.util.List;

/**
 * @author RollW
 */
@Api
public class UserController {
    private final ContextThreadAware<ApiContext> apiContextThreadAware;
    private final UserProvider userProvider;
    private final UserSearchService userSearchService;
    private final UserPersonalDataService userPersonalDataService;
    private final StorageUrlProvider storageUrlProvider;

    public UserController(ContextThreadAware<ApiContext> apiContextThreadAware,
                          UserProvider userProvider,
                          UserSearchService userSearchService,
                          UserPersonalDataService userPersonalDataService,
                          StorageUrlProvider storageUrlProvider) {
        this.apiContextThreadAware = apiContextThreadAware;
        this.userProvider = userProvider;
        this.userSearchService = userSearchService;
        this.userPersonalDataService = userPersonalDataService;
        this.storageUrlProvider = storageUrlProvider;
    }

    @GetMapping("/user")
    public HttpResponseEntity<UserCommonDetailsVo> getAuthenticatedUser() {
        ApiContext context = apiContextThreadAware.getContextThread().getContext();
        UserIdentity contextUser = context.getUser();
        if (contextUser == null) {
            throw new UserViewException(AuthErrorCode.ERROR_UNAUTHORIZED_USE);
        }
        UserPersonalData userPersonalData = userPersonalDataService.getPersonalData(contextUser);
        return HttpResponseEntity.success(UserCommonDetailsVo.of(
                contextUser, userPersonalData,
                storageUrlProvider.getUrlOfStorage(userPersonalData.getAvatar()),
                storageUrlProvider.getUrlOfStorage(userPersonalData.getCover())
        ));
    }

    @GetMapping("/users")
    public HttpResponseEntity<List<UserCommonDetailsVo>> listUsers() {
        List<AttributedUserDetails> users = userProvider.getUsers();
        List<UserCommonDetailsVo> userCommonDetailsVos = users.stream()
                .map(this::toDetailsVo)
                .toList();
        return HttpResponseEntity.success(
                userCommonDetailsVos
        );
    }

    @GetMapping("/users/{userId}")
    public HttpResponseEntity<UserCommonDetailsVo> getUserInfo(
            @PathVariable("userId") Long userId) {
        UserIdentity userIdentity = userProvider.getUser(userId);
        UserPersonalData userPersonalData =
                userPersonalDataService.getPersonalData(userIdentity);
        return HttpResponseEntity.success(UserCommonDetailsVo.of(
                userIdentity, userPersonalData,
                storageUrlProvider.getUrlOfStorage(userPersonalData.getAvatar()),
                storageUrlProvider.getUrlOfStorage(userPersonalData.getCover())
        ));
    }

    @GetMapping("/users/search")
    public HttpResponseEntity<List<UserCommonDetailsVo>> searchUsers(
            @RequestParam("keyword") String keyword) {
        ParamValidate.notEmpty(keyword, "keyword");

        // TODO: page support
        List<AttributedUser> attributedUsers = userSearchService.findUsers(keyword);
        List<UserCommonDetailsVo> userCommonDetailsVos = attributedUsers.stream()
                .map(this::toDetailsVo)
                .toList();
        return HttpResponseEntity.success(userCommonDetailsVos);
    }

    private UserCommonDetailsVo toDetailsVo(AttributedUser user) {
        UserPersonalData userPersonalData =
                userPersonalDataService.getPersonalData(user);
        return UserCommonDetailsVo.of(
                user, userPersonalData,
                storageUrlProvider.getUrlOfStorage(
                        userPersonalData.getAvatar()),
                storageUrlProvider.getUrlOfStorage(
                        userPersonalData.getCover())
        );
    }

    @PutMapping("/users/{userId}/blocks")
    public void blockUser(@PathVariable("userId") Long userId) {

    }

    @DeleteMapping("/users/{userId}/blocks")
    public void unblockUser(@PathVariable("userId") Long userId) {

    }
}
