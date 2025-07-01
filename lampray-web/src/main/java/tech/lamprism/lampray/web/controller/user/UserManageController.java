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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tech.lamprism.lampray.storage.StorageUrlProvider;
import tech.lamprism.lampray.user.AttributedUser;
import tech.lamprism.lampray.user.AttributedUserDetails;
import tech.lamprism.lampray.user.UserManageService;
import tech.lamprism.lampray.user.UserProvider;
import tech.lamprism.lampray.user.details.UserPersonalData;
import tech.lamprism.lampray.user.details.UserPersonalDataService;
import tech.lamprism.lampray.web.controller.AdminApi;
import tech.lamprism.lampray.web.controller.user.model.UserCreateRequest;
import tech.lamprism.lampray.web.controller.user.model.UserDetailsVo;
import tech.rollw.common.web.HttpResponseEntity;

import java.util.List;

/**
 * @author RollW
 */
@AdminApi
public class UserManageController {
    private final UserManageService userManageService;
    private final UserProvider userProvider;
    private final UserPersonalDataService userPersonalDataService;
    private final StorageUrlProvider storageUrlProvider;

    public UserManageController(UserManageService userManageService,
                                UserProvider userProvider,
                                UserPersonalDataService userPersonalDataService,
                                StorageUrlProvider storageUrlProvider) {
        this.userManageService = userManageService;
        this.userProvider = userProvider;
        this.userPersonalDataService = userPersonalDataService;
        this.storageUrlProvider = storageUrlProvider;
    }

    @GetMapping("/users")
    public HttpResponseEntity<List<UserDetailsVo>> listUser(
            @RequestParam(required = false, defaultValue = "") String filter
    ) {
        List<AttributedUserDetails> users = userProvider.getUsers();
        List<UserPersonalData> personalDataList = userPersonalDataService.getPersonalData(users);

        List<UserDetailsVo> userDetailsVos = users.stream()
                .map(user -> {
                    UserPersonalData personalData = findPersonalData(user, personalDataList);
                    return UserDetailsVo.of(
                            user, personalData,
                            storageUrlProvider.getUrlOfStorage(personalData.getAvatar()),
                            storageUrlProvider.getUrlOfStorage(personalData.getCover())
                    );
                })
                .toList();
        return HttpResponseEntity.success(userDetailsVos);
    }

    private UserPersonalData findPersonalData(AttributedUser user,
                                              List<UserPersonalData> personalDataList) {
        return personalDataList.stream()
                .filter(data -> data.getUserId() == user.getUserId())
                .findFirst()
                .orElse(UserPersonalData.defaultOf(user));
    }

    @GetMapping("/users/{userId}")
    public HttpResponseEntity<UserDetailsVo> getUserDetails(
            @PathVariable Long userId) {
        AttributedUser user = userProvider.getUser(userId);
        UserPersonalData userPersonalData =
                userPersonalDataService.getPersonalData(user);
        UserDetailsVo userDetailsVo = UserDetailsVo.of(
                user, userPersonalData,
                storageUrlProvider.getUrlOfStorage(userPersonalData.getAvatar()),
                storageUrlProvider.getUrlOfStorage(userPersonalData.getCover())
        );
        return HttpResponseEntity.success(userDetailsVo);
    }

    @DeleteMapping("/users/{userId}")
    public void deleteUser(@PathVariable String userId) {

    }

    @PutMapping("/users/{userId}")
    public void updateUser(@PathVariable String userId) {

    }

    @PostMapping("/users")
    public HttpResponseEntity<Long> createUser(@RequestBody UserCreateRequest userCreateRequest) {
        AttributedUser user = userManageService.createUser(
                userCreateRequest.username(),
                userCreateRequest.password(),
                userCreateRequest.email(),
                userCreateRequest.role(),
                true
        );

        return HttpResponseEntity.success(user.getUserId());
    }

    @PutMapping("/users/{userId}/blocks")
    public void blockUser(@PathVariable String userId) {

    }

    @DeleteMapping("/users/{userId}/blocks")
    public void unblockUser(@PathVariable String userId) {

    }

    @GetMapping("/users/{userId}/blocks")
    public void getBlockedUserList(@PathVariable String userId) {

    }
}
